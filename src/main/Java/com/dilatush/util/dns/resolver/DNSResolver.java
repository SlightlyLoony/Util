package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static java.lang.Thread.sleep;

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

    protected static DNSResolverRunner runner;  // the singleton instance of the resolver runner...

    protected       DNSUDPChannel                 udpChannel;
    protected       DNSTCPChannel                 tcpChannel;
    protected final Consumer<Outcome<DNSMessage>> handler;


    private DNSResolver( final Consumer<Outcome<DNSMessage>> _handler ) {
        handler = _handler;
    }


    //TODO better comments...
    /**
     * Query for host address IPv4.
     *
     * @param _domain
     * @param _timeoutMillis
     */
    public void query( final String _domain, final long _timeoutMillis ) {

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( true );

        // TODO: flesh out this prototype code...
        builder.addQuestion( DNSQuestion.create( DNSDomainName.fromString( _domain ).info(), DNSRRType.A ).info() );
        DNSMessage queryMsg = builder.getMessage();
        ByteBuffer data = queryMsg.encode().info();

        DNSQuery query = new DNSQuery( new Timeout( 500, this::timeoutHandler ), queryMsg, data, this );
        udpChannel.send( query );
    }


    private void timeoutHandler() {
        LOGGER.info( "timeout handler" );
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


    public static Outcome<DNSResolver> create( final InetSocketAddress _serverAddress, final Consumer<Outcome<DNSMessage>> _handler ) {

        if( isNull( _serverAddress ) )
            return createOutcome.notOk( "Server address is missing (null)" );

        try {
            ensureRunner();

            DNSResolver resolver = new DNSResolver( _handler );

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
