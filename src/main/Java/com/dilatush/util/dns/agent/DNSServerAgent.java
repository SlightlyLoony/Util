package com.dilatush.util.dns.agent;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.dns.agent.DNSTransport.TCP;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

// TODO: implement iterative resolution...
// TODO: implement delayed shutdown of TCP connection
// TODO: implement logic to handle:
// TODO:   - TCP-only recursive
// TODO:   - normal UDP on truncation TCP iterative
// TODO:   - TCP-only iterative
// TODO: Other:
// TODO:   - create DNSResolver that:
// TODO:     - has one instance of DNSResolverRunner (which should be renamed)
// TODO:     - has any number of DNSResolverConnection instances
// TODO:     - has an optional cache
// TODO:     - has various ways of deciding which DNSResolverConnection to use
// TODO: Move DNS Resolver into its own project

/**
 * Implements an asynchronous resolver for DNS queries to a particular DNS server.  Any number of resolvers can be instantiated concurrently, but
 * only one resolver for each DNS server.  Each resolver can process any number of queries concurrently.  Each resolver can connect using either UDP
 * or TCP (normally UDP, but switching to TCP as needed).  All resolver I/O is performed by a single thread owned by the singleton
 * {@link DNSResolverRunner}, which is instantiated on demand (when any {@link DNSServerAgent} is instantiated).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSServerAgent {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private   static final Outcome.Forge<DNSServerAgent> createOutcome = new Outcome.Forge<>();
    private   static final Outcome.Forge<DNSQuery>    queryOutcome  = new Outcome.Forge<>();

    protected static       DNSResolverRunner          runner;  // the singleton instance of the resolver runner...

    protected              DNSUDPChannel              udpChannel;
    protected              DNSTCPChannel              tcpChannel;
    private   final        Map<Integer,DNSQuery>      queryMap           = new ConcurrentHashMap<>();
    private   final        AtomicInteger              nextID             = new AtomicInteger();
    private   final        DNSTransport               initialTransport;
    private   final        DNSResolution              resolutionMode;


    private DNSServerAgent( final DNSTransport _initialTransport, final DNSResolution _resolutionMode ) {

        initialTransport = _initialTransport;
        resolutionMode   = _resolutionMode;
    }


    public static Outcome<DNSServerAgent> create( final InetSocketAddress _serverAddress, final DNSTransport _initialTransport, final DNSResolution _resolutionMode ) {

        if( isNull( _serverAddress, _initialTransport, _resolutionMode ) )
            return createOutcome.notOk( "Missing required parameter(s)" );

        try {
            ensureRunner();

            DNSServerAgent resolver = new DNSServerAgent( _initialTransport, _resolutionMode );

            Outcome<DNSUDPChannel> udpOutcome = DNSUDPChannel.create( resolver, _serverAddress );
            if( udpOutcome.notOk() )
                return createOutcome.notOk( udpOutcome.msg(), udpOutcome.cause() );
            resolver.udpChannel = udpOutcome.info();

            Outcome<DNSTCPChannel> tcpOutcome = DNSTCPChannel.create( resolver, _serverAddress );
            if( tcpOutcome.notOk() )
                return createOutcome.notOk( tcpOutcome.msg(), tcpOutcome.cause() );
            resolver.tcpChannel = tcpOutcome.info();

            runner.register( resolver.udpChannel, resolver.tcpChannel );

            return createOutcome.ok( resolver );
        }

        catch( IOException _e ) {
            return createOutcome.notOk( "Problem creating DNSServerAgent", _e );
        }
    }


    public static Outcome<DNSServerAgent> create( final InetSocketAddress _serverAddress ) {
        return create( _serverAddress, UDP, DNSResolution.RECURSIVE );
    }


    //TODO better comments...
    /**
     * Query asynchronously for IPv4 host address, using recursive resolution and starting with a UDP query.
     *
     * @param _domain
     * @param _handler
     * @param _timeoutMillis
     * @return
     */
    public Outcome<DNSQuery> queryIPv4( final String _domain, final Consumer<Outcome<DNSQuery>> _handler, final long _timeoutMillis ) {

        // TODO: flesh out this prototype code...
        Outcome<DNSDomainName> domainNameOutcome = DNSDomainName.fromString( _domain );
        if( domainNameOutcome.notOk() )
            return queryOutcome.notOk( domainNameOutcome.msg(), domainNameOutcome.cause() );

        Outcome<DNSQuestion> questionOutcome = DNSQuestion.create( domainNameOutcome.info(), DNSRRType.A );
        if( questionOutcome.notOk() )
            return queryOutcome.notOk( questionOutcome.msg(), questionOutcome.cause() );

        return query( questionOutcome.info(), _handler, _timeoutMillis );
    }

    /**
     * Query asynchronously for resource records, using recursive resolution and starting with a UDP query.
     *
     * @param _domain
     * @param _handler
     * @param _timeoutMillis
     * @return
     */
    public Outcome<DNSQuery> queryAny( final String _domain, final Consumer<Outcome<DNSQuery>> _handler, final long _timeoutMillis ) {

        // TODO: flesh out this prototype code...
        Outcome<DNSDomainName> domainNameOutcome = DNSDomainName.fromString( _domain );
        if( domainNameOutcome.notOk() )
            return queryOutcome.notOk( domainNameOutcome.msg(), domainNameOutcome.cause() );

        Outcome<DNSQuestion> questionOutcome = DNSQuestion.create( domainNameOutcome.info(), DNSRRType.ANY );
        if( questionOutcome.notOk() )
            return queryOutcome.notOk( questionOutcome.msg(), questionOutcome.cause() );

        return query( questionOutcome.info(), _handler, _timeoutMillis );
    }


    /**
     *
     * @param _question
     * @param _handler
     * @param _timeoutMillis
     * @return
     */
    public Outcome<DNSQuery> query( final DNSQuestion _question, final Consumer<Outcome<DNSQuery>> _handler, final long _timeoutMillis ) {

        if( isNull( _question, _handler ) )
            return queryOutcome.notOk( "Missing parameter(s)" );

        return DNSQuery.initiate( this, resolutionMode, _question, _timeoutMillis, initialTransport, _handler, nextID.getAndIncrement() );
    }


    protected void removeQueryMapping( final int _id ) {
        queryMap.remove( _id );
    }

    protected void setQueryMapping( final DNSQuery _query ) {
        queryMap.put( _query.getID(), _query );
    }


    /**
     * Handles decoding and processing received data (which may be from either a UDP channel or a TCP channel).  The given {@link ByteBuffer} must contain exactly one full
     * message, without the TCP length prefix
     *
     * @param _receivedData
     * @param _transport
     */
    protected void handleReceivedData( final ByteBuffer _receivedData, final DNSTransport _transport ) {

        Outcome<DNSMessage> messageOutcome = DNSMessage.decode( _receivedData );

        if( messageOutcome.notOk() ) {
            LOGGER.log( Level.WARNING, "Can't decode received message: " + messageOutcome.msg(), messageOutcome.cause() );
            return;
        }

        DNSMessage message = messageOutcome.info();
        DNSQuery query = queryMap.get( message.id );
        if( query == null ) {
            LOGGER.log( Level.WARNING, "Received response to absent query (timed out?)" );
            return;
        }

        if( _transport != query.getTransport() ) {
            LOGGER.log( Level.WARNING, "Received message on " + _transport + ", expected it on " + query.getTransport() );
            return;
        }

        query.cancelTimeout();
        query.setResponse( message );

        if( (_transport == DNSTransport.UDP) && message.truncated ) {
            query.setTransport( TCP );
            Outcome<?> sendOutcome = query.sendQuery();
            if( sendOutcome.notOk() )
                query.onProblem( sendOutcome.msg(), sendOutcome.cause() );
            return;
        }
        query.onCompletion();
    }


    private static void ensureRunner() throws IOException {

        if( runner != null )
            return;

        synchronized( DNSResolverRunner.class ) {

            if( runner != null )
                return;

            runner = new DNSResolverRunner();
        }
    }
}
