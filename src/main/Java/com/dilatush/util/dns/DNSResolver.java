package com.dilatush.util.dns;

// TODO: implement iterative resolution...
// TODO:   - read root hints
// TODO:   - synchronized cache with map of cache entries in array, sorted map of expirations
// TODO:   - add "useCache" argument to DNSQuery
// TODO: handle query response codes other than OK
// TODO: handle timeouts by retrying the next server
// TODO: if not enough servers, handle timeouts by retries
// TODO: implement delayed shutdown of TCP connection
// TODO: implement logic to handle:
// TODO:   - TCP-only recursive
// TODO:   - normal UDP on truncation TCP iterative
// TODO:   - TCP-only iterative
// TODO: Move DNS Resolver into its own project

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.agent.*;
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

public class DNSResolver {

    private static final Outcome.Forge<DNSResolver> outcomeResolver = new Outcome.Forge<>();
    private static final Outcome.Forge<DNSQuestion> outcomeQuestion = new Outcome.Forge<>();
    private static final Outcome.Forge<?>           outcome         = new Outcome.Forge<>();

    private static final long MIN_TIMEOUT_MILLIS = 5;
    private static final long MAX_TIMEOUT_MILLIS = 15000;

    private final ExecutorService         executor;
    private final DNSNIO                  nio;
    private final List<AgentParams>       agentParams;
    private final Map<String,AgentParams> agentsByName;
    private final List<AgentParams>       agentsByPriority;
    private final List<AgentParams>       agentsBySpeed;
    private final Map<Short,DNSQuery>     activeQueries;
    private final AtomicInteger           nextQueryID;


    private DNSResolver( final ExecutorService _executor, final List<AgentParams> _agentParams ) throws DNSException {

        executor      = _executor;
        nio           = new DNSNIO();
        agentParams   = _agentParams;
        activeQueries = new ConcurrentHashMap<>();
        nextQueryID   = new AtomicInteger();

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


    public static class Builder {


        private       ExecutorService      executor;
        private final List<AgentParams>    agentParams = new ArrayList<>();


        public Outcome<DNSResolver> getDNSResolver() {

            if( executor == null )
                executor = new ExecutorService();

            try {
                return outcomeResolver.ok( new DNSResolver( executor, agentParams ) );
            }
            catch( DNSException _e ) {
                return outcomeResolver.notOk( "Problem creating DNSResolver", _e );
            }
        }


        public void setExecutor( final ExecutorService _executor ) {
            executor = _executor;
        }


        public void addDNSServer( final InetSocketAddress _serverAddress, final long _timeoutMillis, final int _priority, final String _name ) {

            if( isNull( _serverAddress, _name ) )
                throw new IllegalArgumentException( "Missing required argument(s)" );

            agentParams.add( new AgentParams( _timeoutMillis, _priority, _name, _serverAddress ) );
        }
    }


    public record AgentParams( long timeoutMillis, int priority, String name, InetSocketAddress serverAddress ){};
}
