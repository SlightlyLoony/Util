package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.*;

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

// TODO: implement iterative resolution...
// TODO: implement TCP on truncation...
/**
 * Implements an asynchronous resolver for DNS queries to a particular DNS server.  Any number of resolvers can be instantiated concurrently, but
 * only one resolver for each DNS server.  Each resolver can process any number of queries concurrently.  Each resolver can connect using either UDP
 * or TCP (normally UDP, but switching to TCP as needed).  All resolver I/O is performed by a single thread owned by the singleton
 * {@link DNSResolverRunner}, which is instantiated on demand (when any {@link DNSResolver} is instantiated).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSResolver {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<DNSResolver> createOutcome = new Outcome.Forge<>();
    private static final Outcome.Forge<DNSQuery> queryOutcome = new Outcome.Forge<>();

    protected static DNSResolverRunner runner;  // the singleton instance of the resolver runner...

    protected       DNSUDPChannel                 udpChannel;
    protected       DNSTCPChannel                 tcpChannel;

    private   final Map<Integer,DNSQuery>         queryMap = new ConcurrentHashMap<>();
    private   final AtomicInteger                 nextID = new AtomicInteger();


    private DNSResolver() {
    }


    //TODO better comments...
    /**
     * Query asynchronously for host address IPv4.
     *
     * @param _domain
     * @param _handler
     * @param _timeoutMillis
     */
    public Outcome<DNSQuery> query( final String _domain, final Consumer<Outcome<DNSQuery>> _handler, final long _timeoutMillis ) {

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( true );
        builder.setId( nextID.getAndIncrement() );

        // TODO: flesh out this prototype code...
        Outcome<DNSDomainName> domainNameOutcome = DNSDomainName.fromString( _domain );
        if( domainNameOutcome.notOk() )
            return queryOutcome.notOk( domainNameOutcome.msg(), domainNameOutcome.cause() );

        Outcome<DNSQuestion> questionOutcome = DNSQuestion.create( domainNameOutcome.info(), DNSRRType.A );
        if( questionOutcome.notOk() )
            return queryOutcome.notOk( questionOutcome.msg(), questionOutcome.cause() );

        builder.addQuestion( questionOutcome.info() );

        DNSMessage queryMsg = builder.getMessage();

        Outcome<ByteBuffer> encodeOutcome = queryMsg.encode();
        if( encodeOutcome.notOk() )
            return queryOutcome.notOk( encodeOutcome.msg(), encodeOutcome.cause() );

        ByteBuffer data = encodeOutcome.info();

        DNSQuery query = new DNSQuery( queryMsg, data, _handler, _timeoutMillis );
        queryMap.put( queryMsg.id, query );
        udpChannel.send( query );

        return queryOutcome.ok( query );
    }


    protected void handleReceivedData( final ByteBuffer _receivedData ) {

        Outcome<DNSMessage> messageOutcome = DNSMessage.decode( _receivedData );

        if( messageOutcome.notOk() ) {
            LOGGER.log( Level.WARNING, "Can't decode received message: " + messageOutcome.msg(), messageOutcome.cause() );
            return;
        }

        DNSMessage message = messageOutcome.info();
        DNSQuery query = queryMap.get( message.id );
        if( query == null ) {
            LOGGER.log( Level.WARNING, "Received response to inactive query (timed out?)" );
            return;
        }

        boolean cancelled = query.timeout.cancel();
        queryMap.remove( message.id );
        query = query.addResponse( message );
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


    public static Outcome<DNSResolver> create( final InetSocketAddress _serverAddress ) {

        if( isNull( _serverAddress ) )
            return createOutcome.notOk( "Server address is missing (null)" );

        try {
            ensureRunner();

            DNSResolver resolver = new DNSResolver();

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
            return createOutcome.notOk( "Problem creating DNSResolver", _e );
        }
    }
}
