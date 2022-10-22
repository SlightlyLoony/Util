package com.dilatush.util.networkingengine;

import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;
import com.dilatush.util.ip.IPAddress;
import com.dilatush.util.networkingengine.interfaces.OnConnectionCompletionHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Instances of this class (or its subclasses) allow network communications with the TCP protocol when the initial connection is outbound (that is, this computer is initiating the
 * connection).  Generally this kind of TCP pipe is what's needed to implement an application that acts as a networking client (as opposed to a server).  When an instance of this
 * class is first created, it <i>may</i> be bound to a specific local network interface and TCP port, but more commonly the TCP port is left up to the operating system to assign
 * (this is often called an ephemeral port) which generally the users of this class don't care about.  The new instance cannot be used for communications until it has been
 * connected to a remote IP address and TCP port, via one of the connect methods provided by this class.
 */
public class TCPOutboundPipe extends TCPPipe {

    private static final Logger                      LOGGER                                      = getLogger();

    private static final int                         DEFAULT_FINISH_CONNECTION_TIMEOUT_MS        = 2000;
    private static final int                         MAX_FINISH_CONNECTION_INTERVAL_INCREMENT_MS = 100;

    private static final Outcome.Forge<TCPOutboundPipe>   forgeTCPOutboundPipe = new Outcome.Forge<>();
    private static final Outcome.Forge<?>                 forge                = new Outcome.Forge<>();


    private final AtomicBoolean connectFlag;

    private int                           finishConnectionTimeoutMs;   // the maximum time a connection is allowed to take before giving up...
    private int                           finishConnectionIntervalMs;  // delay (in milliseconds) until the next finish connection check...
    private long                          finishConnectionStartTime;   // when we started the finish connection process...
    private OnConnectionCompletionHandler completionHandler;           // the connection completion handler...


    /**
     * Attempts to create a new instance of this class, associated with the given {@link NetworkingEngine} with a new {@link SocketChannel} that is bound to the network
     * interface(s) with the given IP address and local TCP port.  This new instance can then initiate and complete a TCP connection to a remote TCP listener (which could be either
     * on another device or on this computer).  Once connected, data can be read from and written to the network.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _bindToIP The IP address of the local network interface to bind this connection to.  The IP address can be either IPv4 or IPv6.  If it is the wildcard address
     *                  (0.0.0.0 or ::), then the connection will be bound to <i>all</i> local network interfaces.
     * @param _bindToPort The local TCP port to bind this connection to.  If the given port is zero (0), then the TCP/IP stack will choose an available ephemeral port.
     * @return The outcome of the attempt to create a new instance of {@link TCPOutboundPipe}.  If ok, the info contains the new instance.  If not ok, there is an explanatory
     * message and possibly the exception that caused the problem.
     */
    public static Outcome<TCPOutboundPipe> getTCPOutboundPipe( final NetworkingEngine _engine, final IPAddress _bindToIP, final int _bindToPort ) {
        try {
            // sanity checks...
            if( isNull( _engine, _bindToIP ) )   return forgeTCPOutboundPipe.notOk( "_engine or _bindToIP is null" );
            if( (_bindToPort < 0) || (_bindToPort > 65535) ) return forgeTCPOutboundPipe.notOk( "_bindToPort is out of range: " + _bindToPort );

            // open our channel, bind it, and configure it...
            var channel = SocketChannel.open();
            channel.bind( new InetSocketAddress( _bindToIP.toInetAddress(), _bindToPort ) );
            channel.configureBlocking( false );
            channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );  // reuse connections in TIME_WAIT (e.g., after close and reconnect)...
            channel.setOption( StandardSocketOptions.SO_KEEPALIVE, true );  // enable keep-alive packets on this connection (mainly to detect broken connections)...

            // get our new instance...
            var pipe = new TCPOutboundPipe( _engine, channel );
            return forgeTCPOutboundPipe.ok( pipe );
        }
        catch( Exception _e ) {
            return forgeTCPOutboundPipe.notOk( "Problem creating or configuring TCP outbound pipe: " + General.toString( _e ), _e );
        }
    }


    /**
     * Attempts to create a new instance of this class, associated with the given {@link NetworkingEngine} with a new {@link SocketChannel} that is bound to the network
     * interface(s) with the given IP address.  The TCP/IP stack will choose an ephemeral port to bind locally.  This new instance can then initiate and complete a TCP connection
     * to a TCP listener (which could be either on another device or on this computer).  Once connected, data can be read from and written to the network.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _bindToIP The IP address of the local network interface to bind this connection to.  The IP address can be either IPv4 or IPv6.  If it is the wildcard address
     *                  (0.0.0.0 or ::), then the connection will be bound to <i>all</i> local network interfaces.
     * @return The outcome of the attempt to create a new instance of {@link TCPOutboundPipe}.  If ok, the info contains the new instance.  If not ok, there is an explanatory
     * message and possibly the exception that caused the problem.
     */
    public static Outcome<TCPOutboundPipe> getTCPOutboundPipe( final NetworkingEngine _engine, final IPAddress _bindToIP ) {
        return getTCPOutboundPipe( _engine, _bindToIP, 0 );
    }


    /**
     * Creates a new instance of {@link TCPOutboundPipe} that is associated with the given {@link NetworkingEngine}, uses the given channel (that has been bound to network
     * interfaces), with the given finish connection timeout.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _channel The {@link SocketChannel} for the new instance to use.
     * @throws IOException on any I/O problem.
     */
    protected TCPOutboundPipe( final NetworkingEngine _engine, final SocketChannel _channel ) throws IOException {
        super( _engine, _channel );
        connectFlag = new AtomicBoolean( false );
    }


    /**
     * Attempts to connect to the TCP listener at the given IP address and TCP port, taking up to the given time (in milliseconds) before giving up.  This is an asynchronous
     * (non-blocking) method; the given completion handler is called when the attempt is complete (whether that attempt completed normally, failed, or timed out).  Once this
     * method has been called on a given instance of this class, any connect method will fail if called again.  In particular, if a connection attempt fails and you want to try
     * again (whether with the same IP and port, or different ones), you must do so with a new instance of this class.
     *
     * @param _remoteIP The remote IP address to connect to, which may be IPv4 or IPv6, but which may not be the wildcard address (0.0.0.0 or ::).
     * @param _remotePort The remote TCP port to connect to, which must be in the range [1..65535].
     * @param _finishConnectionTimeoutMs The number of milliseconds to wait for a connection to complete before failing.
     * @param _completionHandler The handler to call when the connection attempt has completed (whether that attempt completed normally, failed, or timed out).
     */
    public void connect( final IPAddress _remoteIP, final int _remotePort, final int _finishConnectionTimeoutMs, final OnConnectionCompletionHandler _completionHandler ) {

        // if there's no completion handler, then we really can't do anything at all...
        if( isNull( _completionHandler ) ) throw new IllegalArgumentException( "_completionHandler is null" );

        try {
            // sanity checks...
            if( isNull( _remoteIP ) || _remoteIP.isWildcard() ) {
                postConnectionCompletion( _completionHandler, forge.notOk( "_remoteIP is null or is the wildcard address" ));
                return;
            }
            if( (_remotePort < 1) || (_remotePort > 0xFFFF) ) {
                postConnectionCompletion( _completionHandler, forge.notOk( "_remotePort is out of range: " + _remotePort ) );
                return;
            }
            if( _finishConnectionTimeoutMs < 1 ) {
                postConnectionCompletion( _completionHandler, forge.notOk( "_finishConnectionTimeoutMs is not valid: " + _finishConnectionTimeoutMs ));
                return;
            }

            // make sure we're not trying to connect more than once...
            if( connectFlag.getAndSet( true ) ) {
                postConnectionCompletion( _completionHandler, forge.notOk( "Connect has already been called" ) );  // using the given handler, as the stored one must be saved...
                return;
            }

            // save our handler and our timeout, 'cause we're going to be needing them later...
            completionHandler = _completionHandler;
            finishConnectionTimeoutMs = _finishConnectionTimeoutMs;

            // initiate the connection attempt, which may complete immediately...
            if( channel.connect( new InetSocketAddress( _remoteIP.toInetAddress(), _remotePort ) ) || channel.finishConnect() ) {
                postConnectionCompletion( completionHandler, forge.ok() );
                LOGGER.finest( "Connected immediately to " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort() );
                return;
            }

            // we get here if the connection did not complete immediately, and so must be finished, which could take some time...
            finishConnectionStartTime = System.currentTimeMillis();     // record when we started the process of finishing the completion...
            finishConnectionIntervalMs = 1;  // we're going to check for connection completion in about a millisecond...
            engine.schedule( this::checkConnection, Duration.ofMillis( finishConnectionIntervalMs ) );
        }
        catch( Exception _e ) {
            postConnectionCompletion( completionHandler, forge.notOk( "Problem connecting: " + General.toString( _e ), _e ) );
        }
    }


    /**
     * Attempts to connect to the TCP listener at the given IP address and TCP port, taking up to 2,000 (2 seconds) before giving up.  This is an asynchronous
     * (non-blocking) method; the given completion handler is called when the attempt is complete (whether that attempt completed normally, failed, or timed out).  Once this
     * method has been called on a given instance of this class, any connect method will fail if called again.  In particular, if a connection attempt fails and you want to try
     * again (whether with the same IP and port, or different ones), you must do so with a new instance of this class.
     *
     * @param _remoteIP The remote IP address to connect to, which may be IPv4 or IPv6, but which may not be the wildcard address (0.0.0.0 or ::).
     * @param _remotePort The remote TCP port to connect to, which must be in the range [1..65535].
     * @param _completionHandler The handler to call when the connection attempt has completed (whether that attempt completed normally, failed, or timed out).
     */
    @SuppressWarnings( "unused" )
    public void connect( final IPAddress _remoteIP, final int _remotePort, final OnConnectionCompletionHandler _completionHandler ) {
        connect( _remoteIP, _remotePort, DEFAULT_FINISH_CONNECTION_TIMEOUT_MS, _completionHandler );
    }


    /**
     * Attempts to connect to the TCP listener at the given IP address and TCP port, taking up to the given time (in milliseconds) before giving up.  This is a synchronous
     * (blocking) method that blocks until the connection attempt is completed, whether it was successful, had an error, or timed out.  Once this
     * method has been called on a given instance of this class, any connect method will fail if called again.  In particular, if a connection attempt fails and you want to try
     * again (whether with the same IP and port, or different ones), you must do so with a new instance of this class.
     *
     * @param _remoteIP The remote IP address to connect to, which may be IPv4 or IPv6, but which may not be the wildcard address (0.0.0.0 or ::).
     * @param _remotePort The remote TCP port to connect to, which must be in the range [1..65535].
     * @param _finishConnectionTimeoutMs The number of milliseconds to wait for a connection to complete before failing.
     * @return The outcome of this attempt.  If ok, then the connection to the remote IP and port was successful.  If not ok, there is an explanatory message and possibly the
     * exception that caused the problem.
     */
    public Outcome<?> connect( final IPAddress _remoteIP, final int _remotePort, final int _finishConnectionTimeoutMs ) {
        Waiter<Outcome<?>> waiter = new Waiter<>();
        connect( _remoteIP, _remotePort, _finishConnectionTimeoutMs, waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * Attempts to connect to the TCP listener at the given IP address and TCP port, taking up to 2,000 (2 seconds) before giving up.  This is a synchronous
     * (blocking) method that blocks until the connection attempt is completed, whether it was successful, had an error, or timed out.  Once this
     * method has been called on a given instance of this class, any connect method will fail if called again.  In particular, if a connection attempt fails and you want to try
     * again (whether with the same IP and port, or different ones), you must do so with a new instance of this class.
     *
     * @param _remoteIP The remote IP address to connect to, which may be IPv4 or IPv6, but which may not be the wildcard address (0.0.0.0 or ::).
     * @param _remotePort The remote TCP port to connect to, which must be in the range [1..65535].
     * @return The outcome of this attempt.  If ok, then the connection to the remote IP and port was successful.  If not ok, there is an explanatory message and possibly the
     * exception that caused the problem.
     */
    public Outcome<?> connect( final IPAddress _remoteIP, final int _remotePort ) {
        return connect( _remoteIP, _remotePort, DEFAULT_FINISH_CONNECTION_TIMEOUT_MS );
    }


    private void postConnectionCompletion( final OnConnectionCompletionHandler _completionHandler, final Outcome<?> _outcome ) {
        engine.execute( () -> _completionHandler.handle( _outcome ) );
    }


    private void checkConnection() {

        try {

            LOGGER.finest( "checkConnection after " + finishConnectionIntervalMs + "ms" );

            // compute how long we have taken so far...
            var finishConnectionTimeMs = System.currentTimeMillis() - finishConnectionStartTime;

            // increase the interval between checks by 50% - but at least 1ms, and no more than 100ms...
            finishConnectionIntervalMs = Math.min( MAX_FINISH_CONNECTION_INTERVAL_INCREMENT_MS, finishConnectionIntervalMs + Math.max( 1, (finishConnectionIntervalMs >>> 1) ) );

            // if we've finished connecting, send the notice...
            if( channel.finishConnect() ) {
                LOGGER.finest( "Connected to " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort() + " in " + finishConnectionTimeMs + "ms" );
                postConnectionCompletion( completionHandler, forge.ok() );
            }

            // then see if we've run out of time...
            else if( finishConnectionTimeMs >= finishConnectionTimeoutMs ) {
                LOGGER.finest( "Connection attempt timed out" );
                postConnectionCompletion( completionHandler, forge.notOk( "Connection timed out: " + finishConnectionTimeMs + "ms" ) );
            }

            // otherwise, schedule another check...
            else {
                engine.schedule( this::checkConnection, Duration.ofMillis( finishConnectionIntervalMs ) );
            }

        }
        catch( Exception _e ) {
            postConnectionCompletion( completionHandler, forge.notOk( "Problem connecting: " + General.toString( _e ), _e ) );
        }
    }


    public String toString() {
        return "TCPOutboundPipe: (" + channel.socket().getInetAddress().getHostAddress() + " port " + channel.socket().getPort() + ")";
    }
}
