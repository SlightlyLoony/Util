package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSException;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;
import com.dilatush.util.dns.rr.DNSTimeoutException;

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


/**
 * Implements an asynchronous resolver for DNS queries to a particular DNS server.  Any number of resolvers can be instantiated concurrently, but
 * only one resolver for each DNS server.  Each resolver can process any number of queries concurrently.  Each resolver can connect using either UDP
 * or TCP (normally UDP, but switching to TCP as needed).  All resolver I/O is performed by a single thread owned by the singleton
 * {@link DNSNIO}, which is instantiated on demand (when any {@link DNSServerAgent} is instantiated).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSServerAgent {

    private static final long MIN_TIMEOUT_MILLIS = 5;
    private static final long MAX_TIMEOUT_MILLIS = 15000;

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private   static final Outcome.Forge<DNSServerAgent> createOutcome = new Outcome.Forge<>();
    private   static final Outcome.Forge<DNSQuery>       queryOutcome  = new Outcome.Forge<>();

    private          final DNSUDPChannel                 udpChannel;
    private          final DNSTCPChannel                 tcpChannel;
    private          final ExecutorService               executor;
    private          final DNSResolver                   resolver;
    private          final DNSQuery                      query;
    private          final DNSNIO                        nio;

    private                DNSQueryTimeout               timeout;

    public           final long                          timeoutMillis;
    public           final int                           priority;
    public           final String                        name;


    public DNSServerAgent( final DNSResolver _resolver, final DNSQuery _query, final DNSNIO _nio, final ExecutorService _executor, final DNSResolver.AgentParams _params ) {
        this( _resolver, _query, _nio, _executor, _params.timeoutMillis(), _params.priority(), _params.name(), _params.serverAddress() );
    }


    public DNSServerAgent( final DNSResolver _resolver, final DNSQuery _query, final DNSNIO _nio, final ExecutorService _executor,
                           final long _timeoutMillis, final int _priority, final String _name, final InetSocketAddress _serverAddress ) {

        if( isNull( _resolver, _query, _nio, _executor, _name, _serverAddress ) )
            throw new IllegalArgumentException( "Required argument(s) are missing" );

        if( (_timeoutMillis < MIN_TIMEOUT_MILLIS) || (_timeoutMillis > MAX_TIMEOUT_MILLIS) )
            throw new IllegalArgumentException( "Timeout outside permissible range of [" + MIN_TIMEOUT_MILLIS + ".." + MAX_TIMEOUT_MILLIS + "] milliseconds: " + _timeoutMillis );

        resolver         = _resolver;
        query            = _query;
        nio              = _nio;
        executor         = _executor;
        timeoutMillis    = _timeoutMillis;
        priority         = _priority;
        name             = _name;

        udpChannel = new DNSUDPChannel( this, nio, executor, _serverAddress );
        tcpChannel = new DNSTCPChannel( this, nio, executor, _serverAddress );
    }

    protected Outcome<?> sendQuery( final DNSMessage _queryMsg, final DNSTransport _transport ) {
        Outcome<?> result = switch( _transport ) {
            case UDP -> udpChannel.send( _queryMsg );
            case TCP -> tcpChannel.send( _queryMsg );
        };
        if( result.ok() ) {
            timeout = new DNSQueryTimeout( timeoutMillis, this::handleTimeout );
            nio.addTimeout( timeout );
        }
        return queryOutcome.ok();
    }


    protected void close() {
        timeout.cancel();
        udpChannel.close();
        tcpChannel.close();
    }


    private void handleTimeout() {
        query.handleResponseProblem( "Query timed out", new DNSTimeoutException( "Query timed out" ) );
    }


    /**
     * Handles decoding and processing received data (which may be from either a UDP channel or a TCP channel).  The given {@link ByteBuffer} must contain exactly one full
     * message, without the TCP length prefix
     *
     * @param _receivedData
     * @param _transport
     */
    protected void handleReceivedData( final ByteBuffer _receivedData, final DNSTransport _transport ) {

        timeout.cancel();

        Outcome<DNSMessage> messageOutcome = DNSMessage.decode( _receivedData );

        if( messageOutcome.notOk() ) {
            close();
            query.handleResponseProblem( "Could not decode received DNS message", null );
            return;
        }

        DNSMessage message = messageOutcome.info();

        query.handleResponse( message, _transport );
    }
}
