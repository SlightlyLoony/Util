package com.dilatush.util.networkingengine;

import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPAddress;
import com.dilatush.util.ip.IPv4Address;
import com.dilatush.util.networkingengine.interfaces.OnDatagramReceiptHandler;
import com.dilatush.util.networkingengine.interfaces.OnErrorHandler;
import com.dilatush.util.networkingengine.interfaces.SourceFilter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Instances of this class (or its subclasses) implement servers using the UDP protocol.  In particular, they listen for inbound UDP datagrams from any remote source, calling a
 * specified handler when each datagram is received, and they permit sending datagrams to any remote destinations.
 */
@SuppressWarnings( "unused" )
public class UDPServer extends UDPBase {

    private static final Outcome.Forge<UDPServer> forgeUDPServer = new Outcome.Forge<>();

    private static final Logger LOGGER = getLogger();


    protected final OnDatagramReceiptHandler onReceiptHandler;
    protected final SourceFilter sourceFilter;


    /**
     * Attempts to create a new instance of {@link UDPServer}, registered to the given networking engine, bound to the network interface with the given IP address and the given
     * UDP port, using the given handler upon the receipt of a datagram that is accepted by the (optional) source filter, and (optionally) the given handler upon an error.  The
     * datagrams received may be any size at all, but any bytes greater than the given max datagram bytes will be discarded, and the received datagram marked as truncated.
     *
     * @param _engine The {@link NetworkingEngine} instance to register this UDP server with.
     * @param _bindToIP The local IP address to bind this UDP server to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                  wildcard address for all local network interfaces.
     * @param _bindToPort The local UDP port to bind this UDP server to.
     * @param _onReceiptHandler The handler to be called when a datagram is received.  The datagram's data {@link ByteBuffer} will have its position at zero, and the limit at
     *                          the byte after the last byte read.  The datagram's socket address will be the address of the sender.  The datagram's truncated flag will be
     *                          {@code true} if the maximum datagram size is smaller than the datagram read, and those extra bytes were truncated.  The received datagram will
     *                          contain the IP address and port of the datagram sender.
     * @param _maxDatagramBytes The maximum number of bytes to receive in a datagram (bytes more than this are truncated).  The valid range is [1..65535].
     * @param _onErrorHandler The optional (it may be {@code null} to use the default error handler) handler to be called if an error occurs while accepting a datagram.  A
     *                        message describing the problem, and possibly an exception causing the problem, are both passed to the handler.  The default error handler logs the
     *                        error (with any exception), but otherwise does nothing.
     * @param _sourceFilter The {@link SourceFilter} to use for filtering incoming UDP datagrams, or {@code null} to use the default source filter (accepts all).
     * @return The outcome of the attempt.  If ok, the info contains the new {@link UDPServer} instance, configured and registered.  If not ok, it contains an explanatory
     * message and possibly an exception that caused the problem.
     */
    public static Outcome<UDPServer> getNewInstance( final NetworkingEngine _engine, final IPAddress _bindToIP, final int _bindToPort,
                                                     final OnDatagramReceiptHandler _onReceiptHandler, final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler,
                                                     final SourceFilter _sourceFilter ) {

        try {
            // sanity checks...
            if( isNull( _bindToIP ) ) throw new IllegalArgumentException( "_bindToIP is null" );
            if( (_bindToPort < 1) || (_bindToPort > 65535) ) throw new IllegalArgumentException( "_bindToPort is out of range [1..65535]: " + _bindToPort );

            // get a datagram channel bound to the interface identified by the given IP, and to the given UDP port on that interface...
            var protocolFamily = (_bindToIP instanceof IPv4Address) ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;
            var channel = DatagramChannel.open( protocolFamily );
            channel.configureBlocking( false );
            var socketAddress = new InetSocketAddress( _bindToIP.toInetAddress(), _bindToPort );
            channel.bind( socketAddress );

            // time to actually create the UDP server...
            return forgeUDPServer.ok( new UDPServer( _engine, channel, _onReceiptHandler, _maxDatagramBytes, _onErrorHandler, _sourceFilter ) );
        }
        catch( IOException _e ) {
            return forgeUDPServer.notOk( "I/O problem: " + _e.getMessage(), _e );
        }
        catch( UnsupportedOperationException _e ) {
            return forgeUDPServer.notOk( "IP address type is not supported: " + _e.getMessage(), _e );
        }
        catch( IllegalArgumentException _e ) {
            return forgeUDPServer.notOk( "Argument problem: " + _e.getMessage(), _e );
        }
        catch( Exception _e ) {
            return forgeUDPServer.notOk( "Problem creating UDP server: " + _e.getMessage(), _e );
        }

    }


    /**
     * <p>Creates a new instance of this class that will be associated with the given networking engine, and will use the given {@link DatagramChannel}, which must be bound to
     * the interface(s) and port that this instance will listen for datagrams on.  The new instance will be able to receive datagrams from any source accepted by the (optional)
     * source filter, and these datagrams may be any length from one byte to the given maximum number of datagram bytes.  If the received datagram is longer than the given maximum
     * number of bytes, then the truncated flag is set in the datagram.  The on receipt handler is called with each datagram received, including any truncated datagrams.  The on
     * error handler is called if any errors occur.  The server is started and ready to receive datagrams as soon as it is instantiated.</p>
     *<p>Note that this constructor should not be invoked directly.  Instead, get a new instance of this class through one of the factory methods in this class.</p>
     *
     * @param _engine The {@link NetworkingEngine} to associate the new instance with.
     * @param _channel The {@link DatagramChannel} for this instance to use.  The channel must be bound and unconnected.
     * @param _onReceiptHandler The handler to call when a datagram is received.
     * @param _maxDatagramBytes The maximum number of bytes to receive in a datagram (bytes more than this are truncated).
     * @param _onErrorHandler The handler to call when an error occurs.
     * @param _sourceFilter The {@link SourceFilter} to use for filtering incoming UDP datagrams, or {@code null} to use the default source filter (accepts all).
     * @throws IOException on any I/O error.
     */
    protected UDPServer( final NetworkingEngine _engine, final DatagramChannel _channel, final OnDatagramReceiptHandler _onReceiptHandler,
                         final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler, final SourceFilter _sourceFilter ) throws IOException {
        super( _engine, _channel, _maxDatagramBytes, _onErrorHandler );

        // sanity checks...
        if( isNull( _onReceiptHandler ) )
            throw new IllegalArgumentException( "_onReceiptHandler is null" );
        if( _channel.isConnected() )
            throw new IllegalArgumentException( "_channel is connected; UDP servers may not be connected" );
        if( _channel.getLocalAddress() == null )
            throw new IllegalArgumentException( "_channel is not bound; UDP servers must be bound" );

        // squirrel it all away...
        onReceiptHandler = _onReceiptHandler;
        sourceFilter     = (_sourceFilter != null) ? _sourceFilter : (ip, port) -> true;

        // turn read interest, as we always want to read received datagrams...
        key.interestOpsOr( READ_INTEREST );
    }


    /* package-private */ void onReadable() {

        try {
            // get a read buffer with one extra byte, so we can tell if the datagram was truncated...
            var readBuffer = ByteBuffer.allocate( maxDatagramBytes + 1 );

            // read our datagram, returning the socket address of the sender...
            var socket = (InetSocketAddress) channel.receive( readBuffer );

            // if we get a non-null for the socket address, we got a datagram...
            if( socket != null ) {

                // if this datagram is accepted by the source filter, call our on receipt handler...
                if( sourceFilter.accept( IPAddress.fromInetAddress( socket.getAddress() ), socket.getPort() ) ) {

                    // handle the case of the datagram being truncated...
                    var truncated = ( readBuffer.position() == readBuffer.capacity() );
                    if( truncated ) readBuffer.limit( readBuffer.limit() - 1 );   // getting rid of the extra truncation-detection byte...

                    // make our datagram...
                    readBuffer.flip();
                    var datagram = new InboundDatagram( readBuffer, socket, truncated );

                    // call the on receipt handler in another thread...
                    engine.execute( () -> onReceiptHandler.get( datagram ) );
                }
            }
        }
        catch( Exception _e ) {
            engine.execute( () -> onErrorHandler.handle( "Problem in onReadable: " + General.toString( _e ), _e ) );
        }
        finally {

            // re-enable read interest on our selection key...
            key.interestOpsOr( SelectionKey.OP_READ );
            engine.wakeSelector();
        }
    }
}
