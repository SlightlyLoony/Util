package com.dilatush.util.dns;

// TODO: implement iterative resolution...
// TODO: if not enough servers, handle timeouts by retries
// TODO: implement logic to handle:
// TODO:   - normal UDP on truncation TCP iterative
// TODO:   - TCP-only iterative
// TODO: Handle responses with no answers (see RFC 2308)
// TODO: Get rid of protected everywhere
// TODO: Move DNS Resolver into its own project
// TODO: Comments and Javadocs...
// TODO: implement PTR rr
// TODO: implement SRV rr (https://en.wikipedia.org/wiki/SRV_record)
// TODO: resolver follow CNAME chains when building answers from cache or iterative query

import com.dilatush.util.Checks;
import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.agent.*;
import com.dilatush.util.dns.cache.DNSCache;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSOpCode;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;
import com.dilatush.util.dns.rr.DNSResourceRecord;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.dns.agent.DNSQuery.QueryResult;
import static com.dilatush.util.dns.message.DNSRRType.*;


/**
 * <p>Instances of this class implement a DNS "resolver".  Given a fully-qualified domain name (FQDN), a resolver uses name servers on the Internet to discover information about
 * that FQDN.  Most commonly the information sought is the IP address (v4 or v6) of the host with that FQDN, but there are other uses as well.  For instance, given the FQDN
 * "cnn.com", a resolver can discover the FQDNs of mail exchangers (servers) for that domain, or the name servers that are authoritative for that domain.</p>
 * <p>DNS resolvers can operate in one or both of two quite different modes: by querying a recursive name server (recursive mode) or by doing all the work itself (iterative mode).
 * Most programmers are familiar with recursive mode, as it's how most DNS resolvers on clients or servers work.  Generally the host has a DNS resolver built into the operating
 * system, and this resolver knows the IP addresses of one or more recursive name servers (such as Google's DNS service, OpenDNS, the ISP's DNS server, etc.).  In this most
 * common case, the host's DNS resolver is simply delegating the work to the recursive name server.  A DNS resolver operating in iterative mode does something much more complex,
 * and best illustrated with an example.  Suppose, for instance, that we want to resolve the IPv4 address for "www.bogus.com".  Here are the steps a DNS resolver operating in
 * iterative mode would go through:</p>
 * <ol>
 *     <li>Read the "root hints" file.  This has a list of the domain names and IP addresses for the root name servers.</li>
 *     <li>Query a root name server for "www.bogus.com".  It answers with the domain names and IP addresses for authoritative name servers for "com".</li>
 *     <li>Query a "com" name server for "www.bogus.com".  It answers with the domain names and IP addresses for authoritative name servers for "bogus.com"</li>
 *     <li>Query a "bogus.com" name server for "www.bogus.com".  It answers with the IPv4 address for "www.bogus.com".</li>
 * </ol>
 * <p>Instances of this class can operate in either recursive mode or iterative mode; this option is selected for each query mode.</p>
 * <p>Instances of this class include an optional cache of the results of DNS queries, which can greatly increase the resolver's performance when multiple queries to the same
 * domain are made.  This is a very common occurrence in almost any application, so using the cache is highly recommended.</p>
 */
public class DNSResolver {

    private static final Outcome.Forge<DNSResolver> outcomeResolver    = new Outcome.Forge<>();
    private static final Outcome.Forge<QueryResult> outcomeQueryResult = new Outcome.Forge<>();
    private static final Outcome.Forge<?>           outcome            = new Outcome.Forge<>();

    private final ExecutorService               executor;
    private final DNSNIO                        nio;
    private final List<AgentParams>             agentParams;
    private final Map<String,AgentParams>       agentsByName;
    private final List<AgentParams>             agentsByPriority;
    private final List<AgentParams>             agentsBySpeed;
    private final Map<Short,DNSQuery>           activeQueries;
    private final AtomicInteger                 nextQueryID;
    private final DNSCache                      cache;


    /**
     * Creates a new instance of this class with the given parameters.
     *
     * @param _executor Specifies the executor that will be used to decode and process messages received from DNS servers.
     * @param _agentParams Specifies the parameters for recursive DNS server agents that may be used by this resolver.
     * @param _maxCacheSize Specifies the maximum DNS resource record cache size.
     * @param _maxAllowableTTLMillis Specifies the maximum allowable TTL (in milliseconds) for a resource record in the cache.
     * @throws DNSException if there is a problem instantiating {@link DNSNIO}.
     */
    private DNSResolver( final ExecutorService _executor, final List<AgentParams> _agentParams,
                         final int _maxCacheSize, final long _maxAllowableTTLMillis ) throws DNSException {

        executor      = _executor;
        nio           = new DNSNIO();
        agentParams   = _agentParams;
        activeQueries = new ConcurrentHashMap<>();
        nextQueryID   = new AtomicInteger();
        cache         = new DNSCache( _maxCacheSize, _maxAllowableTTLMillis );

        // map our agent parameters by name...
        Map<String,AgentParams> byName = new HashMap<>();
        agentParams.forEach( ap -> byName.put( ap.name, ap ) );
        agentsByName = Collections.unmodifiableMap( byName );

        // make a list of agents sorted in descending order of priority (so the highest priority agents are first)...
        List<AgentParams> temp = new ArrayList<>( agentParams );
        temp.sort( (a,b) -> b.priority - a.priority );
        agentsByPriority = Collections.unmodifiableList( temp );

        // make a list of agents sorted in ascending order of timeout (so the fastest agents are first)...
        temp = new ArrayList<>( agentParams );
        temp.sort( Comparator.comparingLong( a -> a.timeoutMillis ) );
        agentsBySpeed = Collections.unmodifiableList( temp );
    }


    // recursive query...
    // only one question per query!
    // https://stackoverflow.com/questions/4082081/requesting-a-and-aaaa-records-in-single-dns-query/4083071#4083071
    public void query( final DNSQuestion _question, final Consumer<Outcome<QueryResult>> _handler, final DNSTransport _initialTransport,
                       final DNSServerSelection _serverSelection ) {

        Checks.required( _question, _handler, _initialTransport, _serverSelection );

        if( resolveFromCache( _question, _handler ) )
            return;

        List<AgentParams> agents = getAgents( _serverSelection );

        DNSQuery query = new DNSRecursiveQuery( this, cache, nio, executor, activeQueries, _question, nextQueryID.getAndIncrement(), agents, _handler );

        query.initiate( _initialTransport );
    }


    // iterative query...
    // only one question per query!
    // https://stackoverflow.com/questions/4082081/requesting-a-and-aaaa-records-in-single-dns-query/4083071#4083071
    public void query( final DNSQuestion _question, final Consumer<Outcome<QueryResult>> _handler, final DNSTransport _initialTransport ) {

        Checks.required( _question, _handler, _initialTransport );

        if( resolveFromCache( _question, _handler ) )
            return;

        DNSQuery query = new DNSIterativeQuery( this, cache, nio, executor, activeQueries, _question, nextQueryID.getAndIncrement(), _handler );

        query.initiate( _initialTransport );
    }


    /**
     * Attempts to resolve the given question from the DNS cache, return {@code true} if it resolved successfully.  To be resolved from the cache, the given {@link DNSQuestion}
     * must be for a discrete record type, not {@link DNSRRType#ANY} or {@link DNSRRType#UNIMPLEMENTED}.  There must be at least one result matching the question.  This method
     * will follow CNAME record chains to see if the terminus of the chain is a record matching the type in the question.  If the resolution is successful, this method synthesizes
     * a {@link DNSMessage} containing the answers retrieved from the cache and passes it to the given handler.  If the handler is called, the outcome it passes will always be ok.
     * This method does not add any resource records to the authorities or additional records sections to the synthesized response message.
     *
     * @param _question The {@link DNSQuestion} to attempt to resolve from the DNSCache.
     * @param _handler The {@link Consumer Consumer&lt;Outcome&lt;QueryResult&gt;&gt;} handler to call if the question is successfully resolved from the cache.
     * @return {@code true} if the question was resolved from the cache.
     */
    private boolean resolveFromCache( final DNSQuestion _question, final Consumer<Outcome<QueryResult>> _handler ) {

        // we don't want to attempt resolving ANY queries from cache, because we can't tell if the cache has everything...
        // we don't want to attempt resolving UNIMPLEMENTED queries from cache because, well, they're unimplemented...
        if( (_question.qtype == ANY) || (_question.qtype == UNIMPLEMENTED) )
            return false;

        // get anything the cache might have from the domain we're looking for...
        List<DNSResourceRecord> cached = cache.get( _question.qname );

        // if we got nothing at all back, bail out negatively...
        if( cached.size() == 0 )
            return false;

        // we got something back from the cache, so it's possible we can resolve the question...
        // make a place to stuff our answers...
        List<DNSResourceRecord> answers = new ArrayList<>();

        // iterate over the cached records to see if any of them match what we're looking for, or match a CNAME that might point to what we need...
        cached.forEach( (rr) -> {

            // if the cached record matches the class and type in the question, stuff it directly into the answers...
            if( (rr.klass == _question.qclass) && (rr.type == _question.qtype) )
                answers.add( rr );

            // if it's a CNAME, we need to resolve the chain (which could be arbitrarily long)...
            else if( rr.type == CNAME ) {

                // resolve the CNAME chain, recording the elements into the list cnameChain...
                List<DNSResourceRecord> cnameChain = new ArrayList<>();
                cnameChain.add( rr );
                while( (cnameChain.size() == 1) && (cnameChain.get(0).type == CNAME) ) {
                    answers.add( cnameChain.get( 0 ) );
                    cnameChain = cache.get( ((com.dilatush.util.dns.rr.CNAME)cnameChain.get( 0 )).cname );
                }

                // see if we got the record type we wanted after resolving the CNAME chain...
                AtomicBoolean gotOne = new AtomicBoolean( false );
                cnameChain.forEach( (cnameChainElement) -> {
                    if( (cnameChainElement.klass == _question.qclass) && (cnameChainElement.type == _question.qtype) )
                        gotOne.set( true );
                } );

                // if we got at least one, then dump the CNAME resolution chain into our answers...
                if( gotOne.get() )
                    answers.addAll( cnameChain );
            }
        } );

        // if we got no answers, then we leave, sadly, with a negative answer...
        if( answers.isEmpty() )
            return false;

        // get our synthesized query results, as we have some answers...

        // first our query message...
        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.addQuestion( _question );
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( true );
        builder.setCanRecurse( true );
        DNSMessage query = builder.getMessage();

        // then our response message...
        DNSMessage response = query.getSyntheticResponse( answers );

        // then our log...
        List<DNSQuery.QueryLogEntry> log = new ArrayList<>();
        log.add( new DNSQuery.QueryLogEntry( "Query resolved from cache", System.currentTimeMillis() ) );

        // finally, we have our query result...
        QueryResult queryResult = new QueryResult( query, response, log );

        // call the handler with the result, for we are done...
        _handler.accept( outcomeQueryResult.ok( queryResult ) );

        return true;
    }


    /**
     * Returns a mutable list of {@link AgentParams} ordered according to the given {@link DNSServerSelectionStrategy}.  In the case of the {@code NAMED} strategy, the list will
     * have a single element, corresponding to the agent with the given name.  If the name is not found, the list will be empty.  In the case of the {@code ITERATIVE} strategy,
     * this method returns an empty list.
     *
     * @param _serverSelection
     * @return
     */
    private List<AgentParams> getAgents( final DNSServerSelection _serverSelection ) {

        return switch( _serverSelection.strategy ) {

            case PRIORITY    -> new ArrayList<>( agentsByPriority );
            case SPEED       -> new ArrayList<>( agentsBySpeed );
            case ROUND_ROBIN -> new ArrayList<>( agentParams );
            case RANDOM      -> {
                ArrayList<AgentParams> result = new ArrayList<>( agentParams );
                Collections.shuffle( result );
                yield result;
            }
            case NAMED       -> {
                AgentParams ap = agentsByName.get( _serverSelection.agentName );
                ArrayList<AgentParams> result = new ArrayList<>( 1 );
                if( ap != null ) result.add( ap );
                yield result;
            }
        };
    }


    public void clear() {
        cache.clear();
    }


    /**
     * Instances of this class provide a builder for instances of {@link DNSResolver}.
     */
    public static class Builder {


        private       ExecutorService      executor;
        private final List<AgentParams>    agentParams           = new ArrayList<>();
        private       int                  maxCacheSize          = 1000;
        private       long                 maxAllowableTTLMillis = 2 * 3600 * 1000;  // two hours...


        /**
         * Get an instance of {@link DNSResolver} using the current state of this builder instance.  The default  builder will produce a {@link DNSResolver} instance with
         * a single-threaded executor, no recursive DNS server agents, and a cache with a capacity of 1000 resource records and a maximum allowable TTL of two hours.
         *
         * @return the fresh, tasty new instance of {@link DNSResolver}.
         */
        public Outcome<DNSResolver> getDNSResolver() {

            // if no executor was specified, build a default executor...
            if( executor == null )
                executor = new ExecutorService();

            // try to construct the new instance (it might fail if there's a problem starting up NIO)...
            try {
                return outcomeResolver.ok( new DNSResolver( executor, agentParams, maxCacheSize, maxAllowableTTLMillis ) );
            }
            catch( DNSException _e ) {
                return outcomeResolver.notOk( "Problem creating DNSResolver", _e );
            }
        }


        /**
         * Specifies the executor that will be used to decode and process messages received from DNS servers.  The default is a single-threaded executor.
         *
         * @param _executor The executor to use when decoding and processing messages received from DNS servers.
         */
        public void setExecutor( final ExecutorService _executor ) {
            executor = _executor;
        }


        /**
         * Specifies the maximum DNS resource record cache size.  The default is 1,000 resource records.
         *
         * @param _maxCacheSize The maximum DNS resource record cache size.
         */
        public void setMaxCacheSize( final int _maxCacheSize ) {

            maxCacheSize = _maxCacheSize;
        }


        /**
         * Specifies the maximum allowable TTL (in milliseconds) for a resource record in the cache.  The default is two hours.
         *
         * @param _maxAllowableTTLMillis  the maximum allowable TTL for a resource record in the cache.
         */
        public void setMaxAllowableTTLMillis( final long _maxAllowableTTLMillis ) {

            maxAllowableTTLMillis = _maxAllowableTTLMillis;
        }


        /**
         * Add the given parameters for a recursive DNS server agent to the list of agent parameters contained in this builder.  The list of agent parameters determines the
         * recursive DNS servers that the {@link DNSResolver} instance will be able to use.
         *
         * @param _serverAddress The IP address of the recursive DNS server.
         * @param _timeoutMillis The maximum time (in milliseconds) to wait for responses from the recursive DNS server.
         * @param _priority The priority of this recursive DNS server, with larger numbers meaning higher priority.  The priority is used with
         * {@link DNSServerSelectionStrategy#PRIORITY}.
         * @param _name The human-readable name for this recursive DNS server, used in log entries.
         */
        public void addDNSServer( final InetSocketAddress _serverAddress, final long _timeoutMillis, final int _priority, final String _name ) {

            if( isNull( _serverAddress, _name ) )
                throw new IllegalArgumentException( "Missing required argument(s)" );

            agentParams.add( new AgentParams( _timeoutMillis, _priority, _name, _serverAddress ) );
        }
    }


    /**
     * A simple record to hold the parameters required to construct a {@link DNSServerAgent} instance.
     */
    public record AgentParams( long timeoutMillis, int priority, String name, InetSocketAddress serverAddress ){}
}
