package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolver.AgentParams;
import com.dilatush.util.dns.cache.DNSCache;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.rr.A;
import com.dilatush.util.dns.rr.AAAA;
import com.dilatush.util.dns.rr.DNSResourceRecord;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class contain the elements and state of a DNS query, and provide methods that implement the resolution of that query.
 */
public abstract class DNSQuery {

    private static final Logger LOGGER                           = General.getLogger();

    protected static final Outcome.Forge<QueryResult> queryOutcome = new Outcome.Forge<>();


    protected final DNSResolver                     resolver;
    protected final DNSCache                        cache;
    protected final DNSNIO                          nio;
    protected final ExecutorService                 executor;
    protected final Map<Short,DNSQuery>             activeQueries;
    protected final int                             id;
    protected final DNSQuestion                     question;
    protected final Consumer<Outcome<QueryResult>>  handler;
    protected final List<AgentParams>               agents;
    protected final long                            startTime;
    protected final List<QueryLogEntry>             queryLog;

    protected       DNSServerAgent                  agent;
    protected       DNSTransport                    transport;

    protected       DNSMessage                      queryMessage;
    protected       DNSMessage                      responseMessage;



    protected DNSQuery( final DNSResolver _resolver, final DNSCache _cache, final DNSNIO _nio, final ExecutorService _executor,
                     final Map<Short,DNSQuery> _activeQueries, final DNSQuestion _question, final int _id,
                     final List<AgentParams> _agents, final Consumer<Outcome<QueryResult>> _handler ) {

        // sanity checks...
        if( isNull( _resolver, _cache, _nio, _executor, _activeQueries, _question, _handler ) )
            throw new IllegalArgumentException( "Required argument(s) are missing" );

        resolver        = _resolver;
        cache           = _cache;
        nio             = _nio;
        executor        = _executor;
        activeQueries   = _activeQueries;
        question        = _question;
        id              = _id;
        agents          = _agents;
        handler         = _handler;
        startTime       = System.currentTimeMillis();
        queryLog        = new ArrayList<>();

        activeQueries.put( (short) id, this );

        logQuery("New instance " + question );
    }


    public abstract Outcome<QueryResult> initiate( final DNSTransport _initialTransport );


    /**
     * Add the IP addresses contained in any A or AAAA records in the given list of DNS resource records to the given list of IP addresses.
     *
     * @param _ips The list of IP addresses to append to.
     * @param _rrs The list of DNS resource records to get IP addresses from.
     */
    protected void addIPs( final List<InetAddress> _ips, final List<DNSResourceRecord> _rrs ) {
        _rrs.forEach( (rr) -> {
            if( rr instanceof A )
                _ips.add( ((A)rr).address );
            else if( rr instanceof AAAA )
                _ips.add( ((AAAA)rr).address );
        } );
    }


    protected abstract Outcome<QueryResult> query();


    protected abstract void handleResponse( final DNSMessage _responseMsg, final DNSTransport _transport );

    protected boolean tryOtherServers( final String _errorName ) {
        logQuery("Response message was not OK: " + _errorName );
        while( !agents.isEmpty() ) {
            Outcome<QueryResult> qo = query();
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
            Outcome<QueryResult> qo = query();
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


    protected void logQuery( final String _message ) {
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
