package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolver.AgentParams;
import com.dilatush.util.dns.cache.DNSCache;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSOpCode;
import com.dilatush.util.dns.message.DNSQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.dns.agent.DNSResolution.ITERATIVE;
import static com.dilatush.util.dns.agent.DNSResolution.RECURSIVE;
import static com.dilatush.util.dns.agent.DNSTransport.TCP;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

// TODO: add internal handlers as needed, get rid of external handler (do that in DNSResolver)
/**
 * Instances of this class contain the elements and state of a DNS query, and provide methods that implement the resolution of that query.
 */
public class DNSQuery {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<QueryResult> queryOutcome = new Outcome.Forge<>();


    private final DNSResolver                     resolver;
    private final DNSCache                        cache;
    private final DNSNIO                          nio;
    private final ExecutorService                 executor;
    private final Map<Short,DNSQuery>             activeQueries;
    private final int                             id;
    private final DNSQuestion                     question;
    private final Consumer<Outcome<QueryResult>>  handler;
    private final DNSResolution                   resolutionMode;
    private final List<AgentParams>               agents;
    private final long                            startTime;
    private final List<QueryLogEntry>             queryLog;

    private       DNSServerAgent                  agent;
    private       DNSTransport                    transport;

    private       DNSMessage                      queryMessage;
    private       DNSMessage                      responseMessage;



    public DNSQuery( final DNSResolver _resolver, final DNSCache _cache, final DNSNIO _nio, final ExecutorService _executor,
                     final Map<Short,DNSQuery> _activeQueries, final DNSQuestion _question, final int _id,
                     final List<AgentParams> _agents, final Consumer<Outcome<QueryResult>> _handler, final DNSResolution _resolutionMode ) {

        // sanity checks...
        if( isNull( _resolver, _cache, _nio, _executor, _activeQueries, _question, _handler, _resolutionMode ) )
            throw new IllegalArgumentException( "Required argument(s) are missing" );
        if( (_agents == null) && (_resolutionMode == RECURSIVE) )
            throw new IllegalArgumentException( "Agents argument missing; required in recursive resolution mode" );

        resolver        = _resolver;
        cache           = _cache;
        nio             = _nio;
        executor        = _executor;
        activeQueries   = _activeQueries;
        question        = _question;
        id              = _id;
        agents          = _agents;
        handler         = _handler;
        resolutionMode  = _resolutionMode;
        startTime       = System.currentTimeMillis();
        queryLog        = new ArrayList<>();

        activeQueries.put( (short) id, this );

        logQuery("New instance " + question );
    }


    public Outcome<QueryResult> initiate() {
        return initiate( UDP );
    }

    public Outcome<QueryResult> initiate( final DNSTransport _initialTransport ) {

        logQuery("Initial query" );

        if( isNull( _initialTransport ) )
            throw new IllegalArgumentException( "Required initial transport (TCP/UDP) argument is missing" );

        transport = _initialTransport;

        if( agents.isEmpty() )
            return queryOutcome.notOk( "No DNS servers" );

        return initiateImpl();
    }


    private Outcome<QueryResult> initiateImpl() {

        // figure out what agent we're going to use...
        if( resolutionMode == RECURSIVE ) {

            agent = new DNSServerAgent( resolver, this, nio, executor, agents.remove( 0 ) );
        }

        if( resolutionMode == ITERATIVE ) {
            // TODO: implement for iterative resolution...
        }

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( resolutionMode == RECURSIVE );
        builder.setId( id & 0xFFFF );
        builder.addQuestion( question );

        queryMessage = builder.getMessage();

        logQuery("Sending " + resolutionMode + " query to " + agent.name + " via " + transport );

        Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );

        if( sendOutcome.notOk() )
            return queryOutcome.notOk( sendOutcome.msg(), sendOutcome.cause() );

        return queryOutcome.ok( new QueryResult( queryMessage, null, queryLog ) );
    }


    protected void handleResponse( final DNSMessage _responseMsg, final DNSTransport _transport ) {

        logQuery("Received response via " + _transport );

        // no matter what happens next, we need to shut down the agent...
        agent.close();

        if( _transport != transport ) {
            String msg = "Received message on " + _transport + ", expected it on " + transport;
            LOGGER.log( Level.WARNING, msg );
            logQuery( msg );
            agent.close();
            handler.accept( queryOutcome.notOk( msg ) );
            activeQueries.remove( (short) id );
            return;
        }

        responseMessage = _responseMsg;

        // if our UDP response was truncated, retry it with TCP...
        if( (transport == UDP) && _responseMsg.truncated ) {
            logQuery("UDP response was truncated; retrying with TCP" );
            transport = TCP;
            Outcome<?> sendOutcome = agent.sendQuery( queryMessage, TCP );
            if( sendOutcome.notOk() ) {
                handler.accept( queryOutcome.notOk( "Could not send query via TCP: " + sendOutcome.msg(), sendOutcome.cause() ) );
                activeQueries.remove( (short) id );
            }
            return;
        }

        // handle appropriately according to the response code...
        switch( responseMessage.responseCode ) {

            // the question was answered; the response is valid...
            case OK -> {
                logQuery("Response was ok, " + responseMessage.answers.size() + " answers" );

                // add our results to the cache...
                cache.add( responseMessage.answers );
                cache.add( responseMessage.authorities );
                cache.add( responseMessage.additionalRecords );

                handler.accept( queryOutcome.ok( new QueryResult( queryMessage, responseMessage, queryLog )) );
            }

            case REFUSED -> {
                if( tryOtherServers( "REFUSED" ) )
                    return;
            }

            case NAME_ERROR -> {
                if( tryOtherServers( "NAME ERROR" ) )
                    return;
            }

            // the question could not be interpreted by the server...
            case FORMAT_ERROR -> {
                if( tryOtherServers( "FORMAT ERROR" ) )
                    return;
            }

            case SERVER_FAILURE -> {
                if( tryOtherServers( "SERVER FAILURE" ) )
                    return;
            }

            case NOT_IMPLEMENTED -> {
                if( tryOtherServers( "NOT IMPLEMENTED" ) )
                    return;
            }
        }

        // if we get here, we need to show that this query is inactive...
        activeQueries.remove( (short) id );
    }


    private boolean tryOtherServers( final String _errorName ) {
        logQuery("Response message was not OK: " + _errorName );
        while( !agents.isEmpty() ) {
            Outcome<QueryResult> qo = initiateImpl();
            if( qo.ok() )
                return true;
        }
        logQuery("No more DNS servers to try" );
        handler.accept( queryOutcome.notOk( "No more DNS servers to try; last one responded with " + _errorName ) );
        return false;
    }


    protected void handleResponseProblem( final String _msg, final Throwable _cause ) {
        logQuery("Problem with response: " + _msg + ((_cause != null) ? " - " + _cause.getMessage() : "") );
        while( !agents.isEmpty() ) {
            Outcome<QueryResult> qo = initiateImpl();
            if( qo.ok() )
                return;
        }
        logQuery("No more DNS servers to try" );
        handler.accept( queryOutcome.notOk( _msg, _cause ) );
        activeQueries.remove( (short) id );
    }


    public String toString() {
        return "DNSQuery: " + responseMessage.answers.size() + " answers";
    }


    private void logQuery( final String _message ) {
        queryLog.add( new QueryLogEntry( _message, startTime ) );
    }


    public static class QueryLogEntry {

        public final String msg;
        public final long timeMillis;

        public QueryLogEntry( final String _msg, final long _startTime ) {
            msg = _msg;
            timeMillis = System.currentTimeMillis() - _startTime;
        }

        @Override
        public String toString() {
            return "" + timeMillis + ": " + msg;
        }
    }

    public record QueryResult( DNSMessage query, DNSMessage response, List<QueryLogEntry> log ) {}
}
