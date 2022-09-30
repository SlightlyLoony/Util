package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;


/**
 * Instances of this class (or its subclasses) implement servers using the TCP protocol.  In particular, they listen for inbound TCP connections to a particular IP address and TCP
 * port number.  When a listener detects an inbound TCP connection, it obtains and initializes a new instance of {@link TCPPipe} (or a subclass) to handle that connection.
 */
@SuppressWarnings( "unused" )
public class TCPListener {

    private static final Logger                      LOGGER = getLogger();

    protected static final Outcome.Forge<TCPListener>  forgeTCPListener = new Outcome.Forge<>();
    protected static final Outcome.Forge<?>            forge            = new Outcome.Forge<>();

    protected final IPAddress                        ip;                       // the IP address to bind this listener to...
    protected final int                              port;                     // the TCP port to bind this listener to...
    protected final ServerSocketChannel              channel;                  // the server socket channel that implements this listener...
    protected final NetworkingEngine                 engine;                   // the networking engine associated with this instance...
    protected final Consumer<TCPInboundPipe>         onAcceptHandler;          // the handler that will be called when an inbound TCP connection is ready to be accepted...
    protected final BiConsumer<String, Exception>    onErrorHandler;           // the handler that will be called if an error occurs while accepting a TCP connection...
    protected final Function<TCPInboundPipe,Boolean> rejectConnectionHandler;  // the handler that returns {@code true} if the connection {@link TCPPipe} should be rejected...


    /**
     * Attempts to create, configure, and register a new instance of this class.  The socket option {@code SO_REUSEADDR} is set to {@code true}, it's
     * bound to the given local IP address and port, registered with the selector of the given {@link NetworkingEngine}, and calls the given handler when an inbound connection is
     * accepted.
     *
     * @param _engine The {@link NetworkingEngine} instance to register this TCP listener with.
     * @param _bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                  wildcard address for all local network interfaces.
     * @param _bindToPort The local TCP port to bind this TCP listener to.
     * @param _onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications
     *                         on the accepted TCP connection.
     * @param _onErrorHandler The optional (it may be {@code null} to use the default error handler) handler to be called if an error occurs while accepting a connection.  A
     *                        message describing the problem, and possibly an exception causing the problem, are both passed to the handler.  The default error handler logs the
     *                        error (with any exception), but otherwise does nothing.
     * @param _rejectConnectionHandler The optional (it may be {@code null} to use the default reject connection handler) to be called after a TCP connection has been accepted, but
     *                                 before the {@link #onAcceptHandler} is called.  If the reject connection handler returns {@code true}, the accepted connection is closed, and
     *                                 the {@link #onAcceptHandler} is not called; otherwise the {@link #onAcceptHandler} is called with the new {@link TCPInboundPipe}.  The
     *                                 default reject connection handler always returns {@code false}.
     * @return The outcome of the attempt.  If ok, the info contains the new {@link TCPListener} instance, configured and registered.  If not ok, it contains an explanatory
     * message and possibly an exception that caused the problem.
     */
    public static Outcome<TCPListener> getInstance( final NetworkingEngine _engine, IPAddress _bindToIP, int _bindToPort, Consumer<TCPInboundPipe> _onAcceptHandler,
                                                    BiConsumer<String,Exception> _onErrorHandler, Function<TCPInboundPipe,Boolean> _rejectConnectionHandler ) {

        try {

            // sanity checks...
            if( isNull( _engine, _bindToIP, _onAcceptHandler ) )
                throw new IllegalArgumentException( "_engine, _bindToIP, or _onAcceptHandler is null" );
            if( (_bindToPort < 1) || (_bindToPort > 0xFFFF) )
                throw new IllegalArgumentException( "_bindToPort is out of range (1-65535): " + _bindToPort );

            // attempt to get our instance...
            return forgeTCPListener.ok( new TCPListener( _engine, _bindToIP, _bindToPort, _onAcceptHandler, _onErrorHandler, _rejectConnectionHandler ) );
        }
        catch( Exception _e ) {
            return forgeTCPListener.notOk( "Problem instantiating TCPListener: " + _e.getMessage(), _e );
        }
    }


    /**
     * Attempts to create, configure, and register a new instance of this class.  The socket option {@code SO_REUSEADDR} is set to {@code true}, it's
     * bound to the given local IP address and port, registered with the selector of the given {@link NetworkingEngine}, and calls the given handler when an inbound connection is
     * accepted.  Both the on error handler and the reject connection handler will be the default handlers.
     *
     * @param _engine The {@link NetworkingEngine} instance to register this TCP listener with.
     * @param _bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                  wildcard address for all local network interfaces.
     * @param _bindToPort The local TCP port to bind this TCP listener to.
     * @param _onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications
     *                         on the accepted TCP connection.
     * @return The outcome of the attempt.  If ok, the info contains the new {@link TCPListener} instance, configured and registered.  If not ok, it contains an explanatory
     * message and possibly an exception that caused the problem.
     */
    public static Outcome<TCPListener> getInstance( final NetworkingEngine _engine, IPAddress _bindToIP, int _bindToPort, Consumer<TCPInboundPipe> _onAcceptHandler ) {
        return getInstance( _engine, _bindToIP, _bindToPort, _onAcceptHandler, null, null );
    }


    /**
     * Attempts to create, configure, and register a new instance of this class.  The socket option {@code SO_REUSEADDR} is set to {@code true}, it's
     * bound to the given local IP address and port, registered with the selector of the given {@link NetworkingEngine}, and calls the given handler when an inbound connection is
     * accepted.  The reject connection handler will be the default handler.
     *
     * @param _engine The {@link NetworkingEngine} instance to register this TCP listener with.
     * @param _bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                  wildcard address for all local network interfaces.
     * @param _bindToPort The local TCP port to bind this TCP listener to.
     * @param _onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications
     *                         on the accepted TCP connection.
     * @param _onErrorHandler The optional (it may be {@code null} to use the default error handler) handler to be called if an error occurs while accepting a connection.  A
     *                        message describing the problem, and possibly an exception causing the problem, are both passed to the handler.  The default error handler logs the
     *                        error (with any exception), but otherwise does nothing.
     * @return The outcome of the attempt.  If ok, the info contains the new {@link TCPListener} instance, configured and registered.  If not ok, it contains an explanatory
     * message and possibly an exception that caused the problem.
     */
    public static Outcome<TCPListener> getInstance( final NetworkingEngine _engine, IPAddress _bindToIP, int _bindToPort,
                                                    Consumer<TCPInboundPipe> _onAcceptHandler, BiConsumer<String,Exception> _onErrorHandler ) {
        return getInstance( _engine, _bindToIP, _bindToPort, _onAcceptHandler, _onErrorHandler, null );
    }


    /**
     * Attempts to create, configure, and register a new instance of this class.  The socket option {@code SO_REUSEADDR} is set to {@code true}, it's
     * bound to the given local IP address and port, registered with the selector of the given {@link NetworkingEngine}, and calls the given handler when an inbound connection is
     * accepted.  Both the on error handler and the reject connection handler will be the default handlers.
     *
     * @param _engine The {@link NetworkingEngine} instance to register this TCP listener with.
     * @param _bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                  wildcard address for all local network interfaces.
     * @param _bindToPort The local TCP port to bind this TCP listener to.
     * @param _onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications
     *                         on the accepted TCP connection.
     * @param _rejectConnectionHandler The optional (it may be {@code null} to use the default reject connection handler) to be called after a TCP connection has been accepted, but
     *                                 before the {@link #onAcceptHandler} is called.  If the reject connection handler returns {@code true}, the accepted connection is closed, and
     *                                 the {@link #onAcceptHandler} is not called; otherwise the {@link #onAcceptHandler} is called with the new {@link TCPInboundPipe}.  The
     *                                 default reject connection handler always returns {@code false}.
     * @return The outcome of the attempt.  If ok, the info contains the new {@link TCPListener} instance, configured and registered.  If not ok, it contains an explanatory
     * message and possibly an exception that caused the problem.
     */
    public static Outcome<TCPListener> getInstance( final NetworkingEngine _engine, IPAddress _bindToIP, int _bindToPort,
                                                    Consumer<TCPInboundPipe> _onAcceptHandler, Function<TCPInboundPipe,Boolean> _rejectConnectionHandler ) {
        return getInstance( _engine, _bindToIP, _bindToPort, _onAcceptHandler, null, _rejectConnectionHandler );
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
     * @param _onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications
     *                         on the accepted TCP connection.
     * @param _onErrorHandler The optional (it may be {@code null} to use the default error handler) handler to be called if an error occurs while accepting a connection.  A
     *                        message describing the problem, and possibly an exception causing the problem, are both passed to the handler.  The default error handler logs the
     *                        error (with any exception), but otherwise does nothing.
     * @param _rejectConnectionHandler The optional (it may be {@code null} to use the default reject connection handler) to be called after a TCP connection has been accepted, but
     *                                 before the {@link #onAcceptHandler} is called.  If the reject connection handler returns {@code true}, the accepted connection is closed, and
     *                                 the {@link #onAcceptHandler} is not called; otherwise the {@link #onAcceptHandler} is called with the new {@link TCPInboundPipe}.  The
     *                                 default reject connection handler always returns {@code false}.
     * @throws IllegalArgumentException if any arguments are invalid.
     * @throws IOException if there is a problem opening or configuring the {@link ServerSocketChannel} for this instance, or in registering it with the network engine's selector.
     */
    protected TCPListener( final NetworkingEngine _engine, IPAddress _bindToIP, int _bindToPort, Consumer<TCPInboundPipe> _onAcceptHandler,
                           BiConsumer<String,Exception> _onErrorHandler, Function<TCPInboundPipe,Boolean> _rejectConnectionHandler ) throws IOException {

        ip                      = _bindToIP;
        port                    = _bindToPort;
        engine                  = _engine;
        onAcceptHandler         = _onAcceptHandler;
        onErrorHandler          = (_onErrorHandler == null) ? this::defaultOnErrorHandler : _onErrorHandler;
        rejectConnectionHandler = (_rejectConnectionHandler == null) ? this::defaultRejectConnectionHandler : _rejectConnectionHandler;

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
                if( rejectConnectionHandler.apply( pipe ) ) {
                    LOGGER.finest( "Rejected TCP connection from " + pipe );
                    pipe.close();
                    return;
                }
                onAcceptHandler.accept( pipe );
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
     * This method exists to facilitate subclassing both {@link TCPListener} and {@link TCPPipe}.  For instance, if you were building a web server, you might extend
     * {@link TCPListener} to make {@code HTTPListener}, and {@link TCPInboundPipe} to make {@code HTTPPipe}.  In {@code HTTPListener}, you would then override this method to
     * create and return a new instance of {@code HTTPPipe}.
     * @return The outcome of the attempt to accept the incoming connection.  If ok, the info contains the new instance of {@code TCPPipe} (or a subclass of it).  If not ok
     * there is an explanatory message and possibly the exception that caused the problem.
     * @throws IOException if there was a problem accepting the connection.
     */
    protected Outcome<TCPInboundPipe> getPipe() throws IOException {
        return TCPInboundPipe.getTCPInboundPipe( engine, channel.accept() );
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
     * The default {@code rejectConnectionHandler}, which always returns {@code false} (meaning do <i>not</i> reject the given connection.
     *
     * @param _pipe The {@link TCPPipe} instance with the inbound connection.
     * @return {@code true} to reject the connection, {@code false} to accept it.
     */
    private boolean defaultRejectConnectionHandler( final TCPPipe _pipe ) {
        return false;
    }


    /**
     * Attempts to set the given socket option to the given value.
     *
     * @param _name The socket option.
     * @param _value The value for the socket option.
     * @param <T> The type of the socket option value.
     * @return The result of this operation.  If ok, the socket option value was successfully set.  If not ok, there is an explanatory message and possibly the exception that
     * caused the problem.
     */
    public <T> Outcome<?> setOption( final SocketOption<T> _name, final T _value ) {

        try {
            // sanity checks...
            if( isNull( _name, _value ) ) return forge.notOk( "_name or _value is null" );

            // try to set the option...
            channel.setOption( _name, _value );
            return forge.ok();
        }
        catch( Exception _e ) {
            return forge.notOk( "Problem setting socket option: " + _e.getMessage(), _e );
        }
    }


    /**
     * Attempts to get the value of the given socket option.
     *
     * @param _name The socket option.
     * @return The result of this operation.  If ok, the info contains the socket option's value.  If not ok, there is an explanatory message and possibly the exception that
     * caused the problem.
     * @param <T> The type of the socket option's value.
     */
    @SuppressWarnings( "DuplicatedCode" )
    public <T> Outcome<T> getOption( final SocketOption<T> _name ) {

        // get the typed forge...
        var forgeT = new Outcome.Forge<T>();

        try {
            // sanity checks...
            if( isNull( _name ) ) return forgeT.notOk( "_name is null" );

            // attempt to get the option's value...
            T value = channel.getOption( _name );

            return forgeT.ok( value );
        }
        catch( Exception _e ) {
            return forgeT.notOk( "Problem getting socket option: " + _e.getMessage(), _e );
        }
    }


    /**
     * Returns a set containing all the socket options supported by this instance.
     *
     * @return A set containing all the socket options supported by this instance.
     */
    public Set<SocketOption<?>> getSupportedOptions() {
        return channel.supportedOptions();
    }


    /**
     * Close the channel.  This instance is not usable once it has been closed.
     */
    public void close() {

        // do the actual close...
        try {
            channel.close();
        }
        catch( IOException _e ) {
            // ignore...
        }

        // wake up the selector to make sure the close has immediate effect...
        engine.wakeSelector();
    }


    /**
     * Returns a string representing this instance.
     * @return a string representing this instance.
     */
    public String toString() {
        return "TCPListener (" + ip.toString() + ", port " + port + ")";
    }
}
