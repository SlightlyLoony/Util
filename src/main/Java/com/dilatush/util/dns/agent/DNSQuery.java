package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolver.AgentParams;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSOpCode;
import com.dilatush.util.dns.message.DNSQuestion;

import javax.management.Query;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.dns.agent.DNSTransport.TCP;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

/**
 * Instances of this class contain the elements and state of a DNS query, and provide methods that implement the resolution of that query.
 */
public class DNSQuery {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<QueryResult> queryOutcome = new Outcome.Forge<>();
    private static final AtomicInteger              nextID       = new AtomicInteger();


    private final DNSResolver                     resolver;
    private final DNSNIO                          nio;
    private final ExecutorService                 executor;
    private final DNSQuestion                     question;
    private final Consumer<Outcome<QueryResult>>  handler;
    private final DNSResolution                   resolutionMode;
    private final List<AgentParams>               agents;
    private final long                            startTime;

    private       DNSServerAgent                  agent;
    private       DNSTransport                    transport;

    private       DNSMessage                      queryMessage;
    private       DNSMessage                      responseMessage;


    public DNSQuery( final DNSResolver _resolver, final DNSNIO _nio, final ExecutorService _executor, final DNSQuestion _question, final List<AgentParams> _agents,
                     final Consumer<Outcome<QueryResult>> _handler, final DNSResolution _resolutionMode ) {

        if( isNull( _resolver, _nio, _executor, _question, _handler, _resolutionMode ) )
            throw new IllegalArgumentException( "Required argument(s) are missing" );
        if( (_agents == null) && (_resolutionMode == DNSResolution.RECURSIVE) )
            throw new IllegalArgumentException( "Agents argument missing; required in recursive resolution mode" );

        resolver        = _resolver;
        nio             = _nio;
        executor        = _executor;
        question        = _question;
        agents          = _agents;
        handler         = _handler;
        resolutionMode  = _resolutionMode;
        startTime       = System.currentTimeMillis();
    }


    protected Outcome<QueryResult> initiate() {
        return initiate( UDP );
    }

    public Outcome<QueryResult> initiate( final DNSTransport _initialTransport ) {

        if( isNull( _initialTransport ) )
            throw new IllegalArgumentException( "Required initial transport (TCP/UDP) argument is missing" );

        transport = _initialTransport;

        // figure out what agent we're going to use...
        if( resolutionMode == DNSResolution.RECURSIVE ) {

            if( agents.size() == 0 )
                return queryOutcome.notOk( "No working DNS servers" );

            AgentParams params = agents.remove( 0 );
            agent = new DNSServerAgent( resolver, this, nio, executor, params.timeoutMillis(), params.priority(), params.name(), params.serverAddress() );
        }

        if( resolutionMode == DNSResolution.ITERATIVE ) {
            // TODO: implement for iterative resolution...
        }

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( resolutionMode == DNSResolution.RECURSIVE );
        builder.setId( nextID.getAndIncrement() & 0xFFFF );
        builder.addQuestion( question );

        queryMessage = builder.getMessage();

        Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );

        if( sendOutcome.notOk() )
            return queryOutcome.notOk( sendOutcome.msg(), sendOutcome.cause() );

        return queryOutcome.ok( new QueryResult( queryMessage, null, 0 ) );
    }


    protected void handleResponse( final DNSMessage _responseMsg, final DNSTransport _transport ) {

        if( _transport != transport ) {
            String msg = "Received message on " + _transport + ", expected it on " + transport;
            LOGGER.log( Level.WARNING, msg );
            agent.close();
            handler.accept( queryOutcome.notOk( msg ) );
            return;
        }

        responseMessage = _responseMsg;

        if( (transport == UDP) && _responseMsg.truncated ) {
            transport = TCP;
            Outcome<?> sendOutcome = agent.sendQuery( queryMessage, TCP );
            if( sendOutcome.notOk() ) {
                agent.close();
                handler.accept( queryOutcome.notOk( sendOutcome.msg(), sendOutcome.cause() ) );
            }
        }
        else {
            agent.close();
            handler.accept( queryOutcome.ok( new QueryResult( queryMessage, responseMessage, System.currentTimeMillis() - startTime )) );
        }
    }


    protected void handleResponseProblem( final String _msg, final Throwable _cause ) {
        handler.accept( queryOutcome.notOk( _msg, _cause ) );
    }


    public String toString() {
        return "DNSQuery: " + responseMessage.answers.size() + " answers";
    }


    public record QueryResult( DNSMessage query, DNSMessage response, long millis ) {}
}
