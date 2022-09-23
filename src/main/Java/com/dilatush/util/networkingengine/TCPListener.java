package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

@SuppressWarnings( "unused" )
public class TCPListener {

    private static final Logger                      LOGGER = getLogger();

    private static final Outcome.Forge<TCPListener>  forgeTCPListener = new Outcome.Forge<>();
    private static final Outcome.Forge<?>            forge            = new Outcome.Forge<>();

    private final IPAddress                     ip;               // the IP address to bind this listener to...
    private final int                           port;             // the TCP port to bind this listener to...
    private final ServerSocketChannel           channel;          // the server socket channel that implements this listener...
    private final NetworkingEngine              engine;           // the networking engine associated with this instance...
    private final Consumer<TCPPipe>             onAcceptHandler;  // the handler that will be called when an inbound TCP connection is ready to be accepted...
    private final BiConsumer<String, Exception> onErrorHandler;   // the handler that will be called if an error occurs while accepting a TCP connection...


    /**
     * Attempts to create, configure, and register a new instance of this class.  The socket option {@code SO_REUSEADDR} is set to {@code true}, it's
     * bound to the given local IP address and port, registered with the selector of the given {@link NetworkingEngine}, and calls the given handler when an inbound connection is
     * accepted.
     *
     * @param _engine The {@link NetworkingEngine} instance to register this TCP listener with.
     * @param _bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                  wildcard address for all local network interfaces.
     * @param _bindToPort The local TCP port to bind this TCP listener to.
     * @param _onAcceptHandler The handler to be called when an inbound TCP connection is accepted.
     * @param _onErrorHandler The handler to be called if an error occurs while accepting a TCP connection, or {@code null} if there is none.
     * @return The outcome of the attempt.  If ok, the info contains the new {@link TCPListener} instance, configured and registered.  If not ok, it contains an explanatory
     * message and possibly an exception that caused the problem.
     */
    /* package-private */ static Outcome<TCPListener> getInstance( final NetworkingEngine _engine, final IPAddress _bindToIP, final int _bindToPort,
                                                                   final Consumer<TCPPipe> _onAcceptHandler, final BiConsumer<String,Exception> _onErrorHandler ) {

        try {

            // sanity checks...
            if( isNull( _engine, _bindToIP, _onAcceptHandler) ) return forgeTCPListener.notOk( "_engine, _ip, or _onAcceptHandler is null" );
            if( (_bindToPort < 1) || (_bindToPort > 0xFFFF) )   return forgeTCPListener.notOk( "_port is out of range (1-65535): " + _bindToPort );

            // attempt to get our instance...
            return forgeTCPListener.ok( new TCPListener( _engine, _bindToIP, _bindToPort, _onAcceptHandler, _onErrorHandler ) );
        }
        catch( IOException _e ) {
            return forgeTCPListener.notOk( "Problem opening selector for TCPListener: " + _e.getMessage(), _e );
        }
        catch( Exception _e ) {
            return forgeTCPListener.notOk( "Problem instantiating TCPListener: " + _e.getMessage(), _e );
        }
    }


    /**
     * Protected constructor that attempts to create, configure, and register a new instance of this class.  The socket option {@code SO_REUSEADDR} is set to {@code true}, it's
     * bound to the given local IP address and port, registered with the selector of the given {@link NetworkingEngine}, and calls the given handler when an inbound connection is
     * accepted.
     *
     * @param _engine The {@link NetworkingEngine} instance to register this TCP listener with.
     * @param _bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                  wildcard address for all local network interfaces.
     * @param _bindToPort The local TCP port to bind this TCP listener to.
     * @param _onAcceptHandler The handler to be called when an inbound TCP connection is accepted.
     * @throws IOException if anything goes wrong.
     */
    protected TCPListener( final NetworkingEngine _engine, final IPAddress _bindToIP, final int _bindToPort,
                         final Consumer<TCPPipe> _onAcceptHandler, final BiConsumer<String,Exception> _onErrorHandler ) throws IOException {

        ip              = _bindToIP;
        port            = _bindToPort;
        engine          = _engine;
        onAcceptHandler = _onAcceptHandler;
        onErrorHandler  = (_onErrorHandler == null) ? this::defaultOnErrorHandler : _onErrorHandler;

        // open and configure our server socket channel...
        channel = ServerSocketChannel.open();
        channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );
        channel.bind( new InetSocketAddress( ip.toInetAddress(), port ) );
        channel.configureBlocking( false );

        // register our channel with the selector, with ourselves as the attachment...
        engine.register( channel, channel.validOps(), this );
    }


    /**
     * Called by the associated {@link NetworkingEngine} when there is an incoming TCP connection that can be accepted.  This method calls {@link #getPipe()} to obtain a new
     * instance of {@link TCPPipe} (or a subclass of it), then calls the {@code onAcceptHandler} for this listener with the new instance of {@link TCPPipe}.  If anything goes
     * wrong with this process, the {@code onErrorHandler} is called (the default {@code onErrorHandler} just logs the error as a warning).
     */
    /* package-private */ void onAcceptable() {

        try {
            var getPipeOutcome = getPipe();
            if( getPipeOutcome.ok() ) {
                var pipe = getPipeOutcome.info();
                if( onAcceptHandler != null ) onAcceptHandler.accept( pipe );
                LOGGER.finest( "Accepted TCP connection from " + pipe );
            }
            else
                onErrorHandler.accept( "Problem getting TCPPipe: " + getPipeOutcome.msg(), (Exception) getPipeOutcome.cause() );
        }
        catch( IOException _e ) {
            onErrorHandler.accept( "Problem accepting inbound TCP connection: " + _e.getMessage(), _e );
        }
    }


    /**
     * Attempt to accept the incoming connection and create a new instance of {@link TCPPipe} (or a subclass of it), using this code:
     * <pre> {@code
     * return TCPPipe.getTCPPipe( engine, channel.accept() );}
     * </pre>
     * This method exists to facilitate subclassing both {@link TCPListener} and {@link TCPPipe}.  For instance, if you were building a web server, you might subclass
     * {@link TCPListener} to make {@code HTTPListener}, and {@link TCPPipe} to make {@code HTTPPipe}.  In {@code HTTPListener}, you would then override this method to create and
     * return a new instance of {@code HTTPPipe}.
     * @return The outcome of the attempt to accept the incoming connection.  If ok, the info contains the new instance of {@code TCPPipe} (or a subclass of it).  If not ok
     * there is an explanatory message and possibly the exception that caused the problem.
     * @throws IOException if there was a problem accepting the connection.
     */
    protected Outcome<TCPPipe> getPipe() throws IOException {
        return TCPPipe.getTCPPipe( engine, channel.accept() );
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


    /**
     * Attempts to set the given socket option to the given value.
     *
     * @param name The socket option.
     * @param value The value for the socket option.
     * @param <T> The type of the socket option value.
     * @return If ok, the socket option value was successfully set.  If not ok, there is an explanatory message and the exception that caused the problem.
     */
    public <T> Outcome<?> setOption( final SocketOption<T> name, final T value ) {

        try {

            // try to set the option...
            channel.setOption( name, value );
            return forge.ok();
        }
        catch( Exception _e ) {
            return forge.notOk( "Problem setting socket option: " + _e.getMessage(), _e );
        }
    }


    /**
     * Returns a string representing this instance.
     * @return a string representing this instance.
     */
    public String toString() {
        return "TCPListener (" + ip.toString() + ", port " + port + ")";
    }
}
