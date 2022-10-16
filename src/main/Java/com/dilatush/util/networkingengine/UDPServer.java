package com.dilatush.util.networkingengine;

import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPAddress;
import com.dilatush.util.ip.IPv4Address;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Instances of this class (or its subclasses) implement servers using the UDP protocol.  In particular, they listen for inbound UDP datagrams, calling a specified handler when
 * each datagram is received.
 */
@SuppressWarnings( "unused" )
public class UDPServer {

    private static final Outcome.Forge<UDPServer> forgeUDPServer = new Outcome.Forge<>();
    private static final Logger LOGGER = getLogger();


    protected final NetworkingEngine             engine;
    protected final DatagramChannel              channel;
    protected final Consumer<Datagram>           onReceiptHandler;
    protected final int                          maxDatagramBytes;
    protected final BiConsumer<String,Exception> onErrorHandler;
    protected final SelectionKey                 key;


    /**
     * Attempts to create a new instance of {@link UDPServer}, registered to the given networking engine, bound to the network interface with the given IP address and the given
     * UDP port, using the given handler upon the receipt of a datagram, and (optionally) the given handler upon an error.  The datagrams received may be any size at all, but any
     * bytes greater than the given max datagram bytes will be discarded, and the received datagram marked as truncated.
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
     * @return The outcome of the attempt.  If ok, the info contains the new {@link UDPServer} instance, configured and registered.  If not ok, it contains an explanatory
     * message and possibly an exception that caused the problem.
     */
    public static Outcome<UDPServer> getInstance( final NetworkingEngine _engine, final IPAddress _bindToIP, final int _bindToPort, final Consumer<Datagram> _onReceiptHandler,
                                                  final int _maxDatagramBytes, BiConsumer<String,Exception> _onErrorHandler) {

        try {
            // sanity checks...
            if( isNull( _bindToIP ) ) throw new IllegalArgumentException( "_bindToIP is null" );
            if( (_bindToPort < 1) || (_bindToPort > 65535) ) throw new IllegalArgumentException( "_bindToPort is out of range [1..65535]: " + _bindToPort );

            // get a datagram channel bound to the interface identified by the given IP, and to the given UDP port on that interface...
            var protocolFamily = (_bindToIP instanceof IPv4Address) ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;
            DatagramChannel channel = DatagramChannel.open( protocolFamily );
            var socketAddress = new InetSocketAddress( _bindToIP.toInetAddress(), _bindToPort );
            channel.bind( socketAddress );

            // time to actually create the UDP server...
            return forgeUDPServer.ok( new UDPServer( _engine, channel, _onReceiptHandler, _maxDatagramBytes, _onErrorHandler ) );
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
     * the interface(s) and port that this instance will listen for datagrams on.  The new instance will be able to receive datagrams from any source, and these datagrams may be
     * any length from one byte to the given maximum number of datagram bytes.  If the received datagram is longer than the given maximum number of bytes, then the truncated flag
     * is set in the datagram.  The on receipt handler is called with each datagram received, including any truncated datagrams.  The on error handler is called if any errors
     * occur.  The server is started and ready to receive datagrams as soon as it is instantiated.</p>
     *<p>Note that this constructor should not be invoked directly.  Instead, get a new instance of this class through one of the factory methods in this class.</p>
     *
     * @param _engine The {@link NetworkingEngine} to associate the new instance with.
     * @param _channel The {@link DatagramChannel} for this instance to use.  The channel must be bound and unconnected.
     * @param _onReceiptHandler The handler to call when a datagram is received.
     * @param _maxDatagramBytes The maximum number of bytes to receive in a datagram (bytes more than this are truncated).
     * @param _onErrorHandler The handler to call when an error occurs.
     * @throws IOException on any I/O error.
     */
    protected UDPServer( final NetworkingEngine _engine, final DatagramChannel _channel, final Consumer<Datagram> _onReceiptHandler,
                         final int _maxDatagramBytes, BiConsumer<String,Exception> _onErrorHandler ) throws IOException {

        // sanity checks...
        if( isNull( _engine, _channel, _onReceiptHandler ) )
            throw new IllegalArgumentException( "_engine, _channel, or _onReceiptHandler is null" );
        if( (_maxDatagramBytes < 1) || (_maxDatagramBytes > 65535) )
            throw new IllegalArgumentException( "_maxDatagramBytes is out of range (1..65535): " + _maxDatagramBytes );
        if( _channel.isConnected() )
            throw new IllegalArgumentException( "_channel is connected; UDP servers may not be connected" );
        if( _channel.getLocalAddress() == null )
            throw new IllegalArgumentException( "_channel is not bound; UDP servers must be bound" );

        // squirrel it all away...
        engine           = _engine;
        channel          = _channel;
        onReceiptHandler = _onReceiptHandler;
        maxDatagramBytes = _maxDatagramBytes;
        onErrorHandler   = (_onErrorHandler == null) ? this::defaultOnErrorHandler : _onErrorHandler;
        key              = _engine.register( channel, SelectionKey.OP_READ, this );
    }


    /* package-private */ void onReadable() {

        try {
            // get a read buffer with one extra byte, so we can tell if the datagram was truncated...
            var readBuffer = ByteBuffer.allocate( maxDatagramBytes + 1 );

            // read our datagram, returning the socket address of the sender...
            var socket = (InetSocketAddress) channel.receive( readBuffer );

            // if we get a non-null for the socket address, we got a datagram...
            if( socket != null ) {

                // handle the case of the datagram being truncated...
                var truncated = false;
                if( readBuffer.limit() == readBuffer.capacity() ) {
                    truncated = true;
                    readBuffer.limit( readBuffer.limit() - 1 );   // getting rid of the extra truncation-detection byte...
                }

                // make our datagram...
                readBuffer.flip();
                var datagram = new Datagram( readBuffer, socket, truncated );

                // call the on receipt handler in another thread...
                engine.execute( () -> onReceiptHandler.accept( datagram ) );
            }
        }
        catch( Exception _e ) {
            engine.execute( () -> onErrorHandler.accept( "Problem in onReadable: " + General.toString( _e ), _e ) );
        }
        finally {

            // re-enable read interest on our selection key...
            key.interestOpsOr( SelectionKey.OP_READ );
        }
    }


    /**
     * Initiate sending the given {@link Datagram}.  The remaining bytes in the datagram's data (that is, the bytes between the data {@link ByteBuffer}'s position and its limit)
     * will be written to the network and sent to the remote socket address in the datagram.  When the write is complete, the datagram's data buffer will be cleared and the
     * on send completion handler will be called with the outcome.
     *
     * @param _datagram The {@link Datagram} to send.
     * @param _onSendCompletionHandler The handler to call upon write completion.
     */
    public void send( final Datagram _datagram, final Consumer<Outcome<?>> _onSendCompletionHandler ) {

    }


    /**
     * The default {@code onErrorHandler}, which just logs the error as a warning.
     *
     * @param _message A message explaining the error.
     * @param _e The (optional) exception that caused the error.
     */
    private void defaultOnErrorHandler( final String _message, final Exception _e ) {
        LOGGER.log( Level.WARNING, _message, _e );
    }
}
