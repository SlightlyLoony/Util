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
 * Abstract base class for query implementations.
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
    protected final QueryLog                        queryLog;

    protected       DNSServerAgent                  agent;
    protected       DNSTransport                    transport;

    protected       DNSMessage                      queryMessage;
    protected       DNSMessage                      responseMessage;


    /**
     * Creates a new instance of this abstract base class using the given arguments.  Each instance of this class exists to answer a single {@link DNSQuestion}, which is one
     * of the arguments.
     *
     * @param _resolver The {@link DNSResolver} responsible for creating this query.  This reference provides the query access to some of the resolver's methods and fields.
     * @param _cache The {@link DNSCache} owned by the resolver  This reference provides access for the query to use cached resource records to resolve the query (fully or
     *               partially), and to add new resource records received from other DNS servers.
     * @param _nio The {@link DNSNIO} to use for all network I/O.  This reference can be passed along to the {@link DNSServerAgent}s that need it.
     * @param _executor The {@link ExecutorService} to be used for processing received messages, to keep the load on the NIO thread to a minimum.
     * @param _activeQueries The {@link DNSResolver}'s map of currently active queries.  This reference allows the query to update the map.  The map has a peculiar purpose: to
     *                       keep a reference to any active queries, as the resolver otherwise keeps none.
     * @param _question The {@link DNSQuestion} to be resolved by this query.
     * @param _id The unique identifying 32-bit integer for this query.  The DNS specifications call for this ID to help the resolver match incoming responses to the query that
     *            produced them.  In this implementation, the matching is done by the fact that each query has a unique port number associated with it, so the ID isn't needed at
     *            all for matching.  Nevertheless, it has an important purpose: it is the key for the active query map described above.
     * @param _agents The {@link List List&lt;AgentParams&gt;} of the parameters used to create {@link DNSServerAgent} instances that can query other DNS servers.  Note that for
     *                recursive queries this list is supplied by the resolver, but for iterative queries it is generated in the course of making the queries.
     * @param _handler The {@link Consumer Consumer&lt;Outcome&lt;QueryResult&gt;&gt;} handler that will be called when the query is completed.  Note that the handler is called
     *                 either for success or failure.
     */
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
        handler         = (new LoggingHandlerWrapper( _handler ))::handler;
        queryLog        = new QueryLog();
        
        activeQueries.put( (short) id, this );

        queryLog.log("New query for " + question );
    }


    /**
     * Initiates a query using the given transport (UDP or TCP).  Note that a call to this method may result in several messages to DNS servers and several responses from them.
     * This may happen if a queried DNS server doesn't respond within the timeout time, or if a series of DNS servers must be queried to get the answer to the question this
     * query is trying to resolve.
     *
     * @param _initialTransport The initial transport (UDP or TCP) to use when resolving this query.
     * @return The {@link Outcome Outcome&lt;QueryResult&gt;} of this operation.
     */
    public abstract Outcome<QueryResult> initiate( final DNSTransport _initialTransport );


    /**
     * Add the IP addresses contained in any A or AAAA records in the given list of DNS resource records to the given list of IP addresses.
     *
     * @param _ips The list of IP addresses to append to.
     * @param _rrs The list of DNS resource records to get IP addresses from.
     */
    protected void addIPs( final List<InetAddress> _ips, final List<DNSResourceRecord> _rrs ) {
        _rrs.forEach( (rr) -> {
            if( resolver.useIPv4() && (rr instanceof A) )
                _ips.add( ((A)rr).address );
            else if( resolver.useIPv6() && (rr instanceof AAAA) )
                _ips.add( ((AAAA)rr).address );
        } );
    }


    protected abstract Outcome<QueryResult> query();


    protected abstract void handleResponse( final DNSMessage _responseMsg, final DNSTransport _transport );

    protected boolean tryOtherServers( final String _errorName ) {
        queryLog.log("Response message was not OK: " + _errorName );
        while( !agents.isEmpty() ) {
            Outcome<QueryResult> qo = query();
            if( qo.ok() )
                return true;
        }
        queryLog.log("No more DNS servers to try" );
        handler.accept( queryOutcome.notOk( "No more DNS servers to try; last one responded with " + _errorName, null,
                new QueryResult( queryMessage, null, queryLog ) ) );
        return false;
    }


    protected void handleResponseProblem( final String _msg, final Throwable _cause ) {
        queryLog.log("Problem with response: " + _msg + ((_cause != null) ? " - " + _cause.getMessage() : "") );
        while( !agents.isEmpty() ) {
            Outcome<QueryResult> qo = query();
            if( qo.ok() )
                return;
        }
        queryLog.log("No more DNS servers to try" );
        handler.accept( queryOutcome.notOk( _msg, _cause, new QueryResult( queryMessage, null, queryLog ) ) );
        activeQueries.remove( (short) id );
    }


    public String toString() {
        return "DNSQuery: " + responseMessage.answers.size() + " answers";
    }


    public static class QueryLogEntry {

        public final String msg;
        public final long timeMillis;
        public final int depth;

        private QueryLogEntry( final String _msg ) {
            msg = _msg;
            timeMillis = System.currentTimeMillis();
            depth = 0;
        }


        private QueryLogEntry( final long _timeMillis, final String _msg, final int _depth ) {
            timeMillis = _timeMillis;
            msg = _msg;
            depth = _depth;
        }


        public String toString( final long _startTime ) {
            String timeStr = String.format( "%5d", timeMillis - _startTime );
            String depthStr = "| ".repeat( depth );
            return timeStr + " " + depthStr + " " + msg + "\n";
        }
    }
    
    
    public static class QueryLog {
        
        private final long startTime;
        private final List<QueryLogEntry> entries;
        
        public QueryLog() {
            startTime = System.currentTimeMillis();
            entries = new ArrayList<>();
        }


        public void log( final String _msg ) {
            entries.add( new QueryLogEntry( _msg ) );
        }


        public void addSubQueryLog( final QueryLog _log ) {
            _log.entries.forEach( (e) -> entries.add( new QueryLogEntry( e.timeMillis, e.msg, e.depth + 1 ) ) );
        }


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            entries.forEach( (e) -> sb.append( e.toString( startTime ) ) );
            return sb.toString();
        }
    }

    public record QueryResult( DNSMessage query, DNSMessage response, QueryLog log ) {}


    private static class LoggingHandlerWrapper {

        private final Consumer<Outcome<QueryResult>> actualHandler;

        private LoggingHandlerWrapper( final Consumer<Outcome<QueryResult>> _actualHandler ) {
            actualHandler = _actualHandler;
        }

        private void handler( final Outcome<QueryResult> _outcome ) {
            if( _outcome.notOk() ) {
                LOGGER.fine( "NOT OK query outcome: " + _outcome.msg() );
            }
            actualHandler.accept( _outcome );
        }
    }
}
