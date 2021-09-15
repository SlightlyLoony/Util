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

// TODO: resolver have DNSRecursiveQuery and DNSIterativeQuery
// TODO: resolver follow CNAME chains when building answers from cache or iterative query
// TODO: resolver only queries upon cache failure; always builds answer from cache?

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.agent.*;
import com.dilatush.util.dns.cache.DNSCache;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRClass;
import com.dilatush.util.dns.message.DNSRRType;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.dilatush.util.General.isNull;


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

    private static final Outcome.Forge<DNSResolver> outcomeResolver = new Outcome.Forge<>();
    private static final Outcome.Forge<DNSQuestion> outcomeQuestion = new Outcome.Forge<>();
    private static final Outcome.Forge<?>           outcome         = new Outcome.Forge<>();

    private final ExecutorService         executor;
    private final DNSNIO                  nio;
    private final List<AgentParams>       agentParams;
    private final Map<String,AgentParams> agentsByName;
    private final List<AgentParams>       agentsByPriority;
    private final List<AgentParams>       agentsBySpeed;
    private final Map<Short,DNSQuery>     activeQueries;
    private final AtomicInteger           nextQueryID;
    private final DNSCache                cache;


    /**
     * Creates a new instance of this class with the given parameters.
     *
     * @param _executor Specifies the executor that will be used to decode and process messages received from DNS servers.
     * @param _agentParams Specifies the parameters for recursive DNS server agents that may be used by this resolver.
     * @param _maxCacheSize Specifies the maximum DNS resource record cache size.
     * @param _maxAllowableTTLMillis Specifies the maximum allowable TTL (in millisconds) for a resource record in the cache.
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


    public Outcome<?> queryIPv4( final String _domainName, final Consumer<Outcome<DNSQuery.QueryResult>> _handler, final DNSTransport _initialTransport,
                                 final DNSServerSelectionStrategy _strategy, final String _name  ) {

        Outcome<DNSQuestion> dqo = getQuestion( _domainName, DNSRRType.A );
        if( dqo.notOk() )
            return outcome.notOk( dqo.msg(), dqo.cause() );

        query( dqo.info(), _handler, _initialTransport, _strategy, _name );
        return outcome.ok();
    }


    public void query( final DNSQuestion _question, final Consumer<Outcome<DNSQuery.QueryResult>> _handler, final DNSTransport _initialTransport,
                       final DNSServerSelectionStrategy _strategy, final String _name ) {

        if( isNull( _question, _handler, _initialTransport, _strategy ) )
            throw new IllegalArgumentException( "Missing required query argument(s)" );

        if( (_strategy == DNSServerSelectionStrategy.NAMED) && (_name == null) )
            throw new IllegalArgumentException( "Missing DNS server name when using NAMED strategy" );

        DNSResolution resolutionMode = (_strategy == DNSServerSelectionStrategy.ITERATIVE) ? DNSResolution.ITERATIVE : DNSResolution.RECURSIVE;

        List<AgentParams> agents = getAgents( _strategy, _name );

        DNSQuery query = new DNSQuery( this, nio, executor, activeQueries, _question, nextQueryID.getAndIncrement(), agents, _handler, resolutionMode );
        int id = nextQueryID.getAndIncrement();

        query.initiate( _initialTransport );
    }


    private Outcome<DNSQuestion> getQuestion( final String _domainName, final DNSRRType _type, final DNSRRClass _class ) {

        Outcome<DNSDomainName> dno = DNSDomainName.fromString( _domainName );
        if( dno.notOk() )
            return outcomeQuestion.notOk( dno.msg(), dno.cause() );

        DNSQuestion question = new DNSQuestion( dno.info(), _type, _class );
        return outcomeQuestion.ok( new DNSQuestion( dno.info(), _type, _class ) );
    }


    private Outcome<DNSQuestion> getQuestion( final String _domainName, final DNSRRType _type ) {
        return getQuestion( _domainName, _type, DNSRRClass.IN );
    }


    /**
     * Returns a mutable list of {@link AgentParams} ordered according to the given {@link DNSServerSelectionStrategy}.  In the case of the {@code NAMED} strategy, the list will
     * have a single element, corresponding to the agent with the given name.  If the name is not found, the list will be empty.  In the case of the {@code ITERATIVE} strategy,
     * this method returns a {@code null}.
     *
     * @param _strategy
     * @param _name
     * @return
     */
    private List<AgentParams> getAgents( final DNSServerSelectionStrategy _strategy, final String _name ) {

        return switch( _strategy ) {

            case PRIORITY    -> new ArrayList<>( agentsByPriority );
            case SPEED       -> new ArrayList<>( agentsBySpeed );
            case ROUND_ROBIN -> new ArrayList<>( agentParams );
            case ITERATIVE   -> null;
            case RANDOM      -> {
                ArrayList<AgentParams> result = new ArrayList<>( agentParams );
                Collections.shuffle( result );
                yield result;
            }
            case NAMED       -> {
                AgentParams ap = agentsByName.get( _name );
                ArrayList<AgentParams> result = new ArrayList<>( 1 );
                if( ap != null ) result.add( ap );
                yield result;
            }
        };
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
         * Specifies the maximum allowable TTL (in millisconds) for a resource record in the cache.  The default is two hours.
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
         * @param _timeoutMillis The maximum time (in millisconds) to wait for responses from the recursive DNS server.
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
    public record AgentParams( long timeoutMillis, int priority, String name, InetSocketAddress serverAddress ){};
}
