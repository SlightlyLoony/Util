package com.dilatush.util.networkingengine;

import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;
import com.dilatush.util.ip.IPAddress;
import com.dilatush.util.ip.IPv4Address;
import com.dilatush.util.networkingengine.interfaces.OnErrorHandler;
import com.dilatush.util.networkingengine.interfaces.OnReceiveDatagramHandler;

import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Instances of this class implement clients using the UDP protocol.  In particular, they listen for datagrams from a particular remote source, and permit sending datagrams to
 * that same remote source.
 */
@SuppressWarnings( "unused" )
public class UDPClient extends UDPBase {

    private static final Outcome.Forge<InboundDatagram> forgeInboundDatagram = new Outcome.Forge<>();
    private static final Outcome.Forge<UDPClient>       forgeUDPClient       = new Outcome.Forge<>();

    private static final Logger LOGGER = getLogger();


    protected final AtomicBoolean receiveInProgress;

    protected OnReceiveDatagramHandler onReceiveDatagramHandler;


    /**
     * Attempts to create a new instance of this class that is associated with the given networking engine, bound to the local network interface specified by the given bind-to IP
     * address and to the given bind-to local UDP port, using the remote UDP server specified by the remote IP address and UDP port, with the specified maximum number of bytes on
     * received datagrams, and the specified (optional) error handler.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _bindToAddress The IP address of the local network interface to bind the new instance to, or the wildcard IP address to bind the new instance to all network
     *                       interfaces.
     * @param _bindToPort The local UDP port to bind the new instance to, or zero to use a randomly selected ephemeral UDP port; must be in the range [0..65535].
     * @param _remoteAddress The IP address of the remote UDP server that the new instance will be a client to.
     * @param _remotePort The UDP port (in the range [1..65535] of the remote UDP server that the new instance will be a client to.
     * @param _maxDatagramBytes The maximum number of bytes allowed in a received datagram.  Any excess bytes in a received datagram are discarded, and the datagram is marked as
     *                          truncated.
     * @param _onErrorHandler The optional error handler; if {@code null}, the default error handler (which just logs the error at WARNING level) is used.
     * @return The result of this attempt.  If ok, the info contains the new {@link UDPClient} instance.  If not ok, there is a message describing the problem, and possibly an
     * exception that caused the problem.
     */
    public static Outcome<UDPClient> getNewInstance( final NetworkingEngine _engine,
                                                     final IPAddress _bindToAddress, final int _bindToPort,
                                                     final IPAddress _remoteAddress, final int _remotePort,
                                                     final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler ) {

        try {
            // sanity checks...
            if( isNull( _bindToAddress, _remoteAddress ) )
                return forgeUDPClient.notOk( "_bindToAddress or _remoteAddress is null" );
            if( (_remotePort < 1) || (_remotePort > 65535) )
                return forgeUDPClient.notOk( "_remotePort out of range [1..65535]: " + _remotePort );
            if( (_bindToPort < 0) || (_bindToPort > 65535) )
                return forgeUDPClient.notOk( "_bindToPort out of range [0..65535]: " + _bindToPort );

            // get a non-blocking datagram channel...
            var protocolFamily = (_bindToAddress instanceof IPv4Address) ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;
            var channel = DatagramChannel.open( protocolFamily );
            channel.configureBlocking( false );

            // bind and connect our channel...
            channel.bind( new InetSocketAddress( _bindToAddress.toInetAddress(), _bindToPort ) );
            channel.connect( new InetSocketAddress( _remoteAddress.toInetAddress(), _remotePort ) );

            return forgeUDPClient.ok( new UDPClient( _engine, channel, _maxDatagramBytes, _onErrorHandler ) );
        }
        catch( final Exception _e ) {
            return forgeUDPClient.notOk( "Problem instantiating UDPClient: " + _e.getMessage(), _e );
        }
    }


    /**
     * Attempts to create a new instance of this class that is associated with the given networking engine, bound to the local network interface specified by the given bind-to IP
     * address and to the given bind-to local UDP port, using the remote UDP server specified by the remote IP address and UDP port, with the specified maximum number of bytes on
     * received datagrams, and the default error handler.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _bindToAddress The IP address of the local network interface to bind the new instance to, or the wildcard IP address to bind the new instance to all network
     *                       interfaces.
     * @param _bindToPort The local UDP port to bind the new instance to, or zero to use a randomly selected ephemeral UDP port; must be in the range [0..65535].
     * @param _remoteAddress The IP address of the remote UDP server that the new instance will be a client to.
     * @param _remotePort The UDP port (in the range [1..65535] of the remote UDP server that the new instance will be a client to.
     * @param _maxDatagramBytes The maximum number of bytes allowed in a received datagram.  Any excess bytes in a received datagram are discarded, and the datagram is marked as
     *                          truncated.
     * @return The result of this attempt.  If ok, the info contains the new {@link UDPClient} instance.  If not ok, there is a message describing the problem, and possibly an
     * exception that caused the problem.
     */
    public static Outcome<UDPClient> getNewInstance( final NetworkingEngine _engine,
                                                     final IPAddress _bindToAddress, final int _bindToPort,
                                                     final IPAddress _remoteAddress, final int _remotePort,
                                                     final int _maxDatagramBytes ) {
        return getNewInstance( _engine, _bindToAddress, _bindToPort, _remoteAddress, _remotePort, _maxDatagramBytes, null );
    }


    /**
     * Attempts to create a new instance of this class that is associated with the given networking engine, bound to all local network interfaces and to an ephemeral local UDP
     * port, using the remote UDP server specified by the remote IP address and UDP port, with the specified maximum number of bytes on received datagrams, and the specified
     * (optional) error handler.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _remoteAddress The IP address of the remote UDP server that the new instance will be a client to.
     * @param _remotePort The UDP port (in the range [1..65535] of the remote UDP server that the new instance will be a client to.
     * @param _maxDatagramBytes The maximum number of bytes allowed in a received datagram.  Any excess bytes in a received datagram are discarded, and the datagram is marked as
     *                          truncated.
     * @param _onErrorHandler The optional error handler; if {@code null}, the default error handler (which just logs the error at WARNING level) is used.
     * @return The result of this attempt.  If ok, the info contains the new {@link UDPClient} instance.  If not ok, there is a message describing the problem, and possibly an
     * exception that caused the problem.
     */
    public static Outcome<UDPClient> getNewInstance( final NetworkingEngine _engine,
                                                     final IPAddress _remoteAddress, final int _remotePort,
                                                     final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler ) {
        return getNewInstance( _engine, IPv4Address.WILDCARD, 0, _remoteAddress, _remotePort, _maxDatagramBytes, _onErrorHandler );
    }


    /**
     * Attempts to create a new instance of this class that is associated with the given networking engine, bound to all local network interfaces and to an ephemeral local UDP
     * port, using the remote UDP server specified by the remote IP address and UDP port, with the specified maximum number of bytes on received datagrams, and the default error
     * handler.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _remoteAddress The IP address of the remote UDP server that the new instance will be a client to.
     * @param _remotePort The UDP port (in the range [1..65535] of the remote UDP server that the new instance will be a client to.
     * @param _maxDatagramBytes The maximum number of bytes allowed in a received datagram.  Any excess bytes in a received datagram are discarded, and the datagram is marked as
     *                          truncated.
     * @return The result of this attempt.  If ok, the info contains the new {@link UDPClient} instance.  If not ok, there is a message describing the problem, and possibly an
     * exception that caused the problem.
     */
    public static Outcome<UDPClient> getNewInstance( final NetworkingEngine _engine,
                                                     final IPAddress _remoteAddress, final int _remotePort,
                                                     final int _maxDatagramBytes ) {
        return getNewInstance( _engine, IPv4Address.WILDCARD, 0, _remoteAddress, _remotePort, _maxDatagramBytes, null );
    }


    /**
     * Create a new instance of this class that is associated with the given networking engine, using the given {@link DatagramChannel} (which must be bound and connected), the
     * specified maximum number of bytes on received datagrams, and the specified (optional) on error handler.  This constructor should only be called from the public
     * {@code getNewInstance} factory methods.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _channel The {@link DatagramChannel} to use with the new instance.
     * @param _maxDatagramBytes The maximum number of bytes allowed in a received datagram.  Any excess bytes in a received datagram are discarded, and the datagram is marked as
     *                          truncated.
     * @param _onErrorHandler The optional error handler; if {@code null}, the default error handler (which just logs the error at WARNING level) is used.
     * @throws Exception on any problem.
     */
    protected UDPClient( final NetworkingEngine _engine, final DatagramChannel _channel,
                         final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler ) throws Exception {
        super( _engine, _channel, _maxDatagramBytes, _onErrorHandler );

        // sanity checks...
        if( !_channel.isConnected() )
            throw new IllegalArgumentException( "_channel is connected; UDP clients must be connected" );
        if( _channel.getLocalAddress() == null )
            throw new IllegalArgumentException( "_channel is not bound; UDP clients must be bound" );

        // some setup...
        receiveInProgress = new AtomicBoolean( false );
    }


    /**
     * Initiate receiving a datagram from the remote server asynchronously (non-blocking).  Note that this method returns immediately even if there is no datagram from the remote
     * server currently available.  When a datagram <i>is</i> available, the given on receive datagram handler is called with the received datagram.  That handler is never called
     * in the thread that called this method; instead it is called with a thread from the scheduled executor in the associated networking engine.  Only one receive operation may
     * be in progress at any given time; any attempt to initiate more than one will result in an exception.
     *
     * @param _onReceiveDatagramHandler The handler to call when a datagram is received.
     * @throws NetworkingEngineException if no handler is supplied, or if a receive operation is already in progress.
     */
    public void receive( final OnReceiveDatagramHandler _onReceiveDatagramHandler ) throws NetworkingEngineException {

        // if we don't get a handler, we really have no alternative to an exception...
        if( isNull( _onReceiveDatagramHandler ) ) throw new NetworkingEngineException( "_onReceiveDatagramHandler is null" );

        // if we have a receive operation in progress, stop here and tell the caller...
        if( receiveInProgress.getAndSet( true ) ) throw new NetworkingEngineException( "Receive operation already in progress" );

        // save our handler...
        onReceiveDatagramHandler = _onReceiveDatagramHandler;

        // do the actual work of receiving a datagram...
        receiveImpl();
    }


    /**
     * Attempt to receive a datagram synchronously (blocking).
     *
     * @return The result of this operation.  If ok, then the info contains the received datagram.  If not ok, there is an explanatory message and possibly the exception that
     * caused the problem.
     * @throws NetworkingEngineException if a receive operation is already in progress.
     */
    public Outcome<InboundDatagram> receive() throws NetworkingEngineException {
        var waiter = new Waiter<Outcome<InboundDatagram>>();
        receive( waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * Called by the associated network engine when read interest is expressed, and the channel is readable.
     */
    @Override
    /* package-private */ void onReadable() {
        receiveImpl();
    }


    /**
     * The actual work of receiving a datagram.
     */
    private void receiveImpl() {

        try {
            // get a read buffer with one extra byte, so we can tell if the datagram was truncated...
            var readBuffer = ByteBuffer.allocate( maxDatagramBytes + 1 );

            // read our datagram, returning the socket address of the sender...
            var socket = (InetSocketAddress) channel.receive( readBuffer );

            // if we get a non-null for the socket address, we got a datagram...
            if( socket != null ) {

                // handle the case of the datagram being truncated...
                var truncated = ( readBuffer.limit() == readBuffer.capacity() );
                if( truncated ) readBuffer.limit( readBuffer.limit() - 1 );   // getting rid of the extra truncation-detection byte...

                // make our datagram...
                readBuffer.flip();
                var datagram = new InboundDatagram( readBuffer, socket, truncated );

                // post our ok completion...
                postReceiveOutcome( forgeInboundDatagram.ok( datagram ) );
            }

            // otherwise, we need to express read interest...
            else {
                LOGGER.finest( "Expressing read interest for UDP datagram" );
                key.interestOpsOr( READ_INTEREST );
                engine.wakeSelector();  // this guarantees that the key change will be effective immediately...
            }
        }
        catch( Exception _e ) {
            LOGGER.log( Level.FINE, "Problem in onReadable: " + General.toString( _e ), _e );
            postReceiveOutcome( forgeInboundDatagram.notOk( "Problem in onReadable: " + General.toString( _e ), _e ) );
        }
    }


    /**
     * Post the outcome of a read operation, using a thread in the networking engine's scheduled executor.
     *
     * @param _outcome The outcome of the read datagram operation.
     */
    private void postReceiveOutcome( final Outcome<InboundDatagram> _outcome ) {

        // if there was no read in progress, just return...
        if( !receiveInProgress.getAndSet( false ) ) return;

        // otherwise, send the completion...
        engine.execute( () -> onReceiveDatagramHandler.handle( _outcome ) );
    }
}
