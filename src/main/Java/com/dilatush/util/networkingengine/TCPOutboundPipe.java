package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Instances of this class (or its subclasses) are used to communicate over the TCP protocol.  Instances may be created by a listener ({@link TCPListener} to respond to inbound
 * TCP connections, or they may be created by application code to initiate an outbound TCP connection.
 */
public class TCPOutboundPipe extends TCPPipe {

    private static final Logger                      LOGGER                                      = getLogger();

    private static final int                         DEFAULT_FINISH_CONNECTION_TIMEOUT_MS        = 2000;
    private static final int                         MAX_FINISH_CONNECTION_INTERVAL_INCREMENT_MS = 100;

    private static final Outcome.Forge<TCPOutboundPipe>   forgeTCPOutboundPipe = new Outcome.Forge<>();
    private static final Outcome.Forge<?>                 forge                = new Outcome.Forge<>();


    private final AtomicBoolean connectFlag;
    private final int           finishConnectionTimeoutMs;

    private int  finishConnectionIntervalMs;  // delay (in milliseconds) until the next finish connection check...
    private long finishConnectionStartTime;   // when we started the finish connection process...


    public static Outcome<TCPOutboundPipe> getTCPOutboundPipe( final NetworkingEngine _engine,
                                                               final IPAddress _bindToIP, final int _bindToPort, final int _finishConnectionTimeoutMs ) {
        try {
            // sanity checks...
            if( isNull( _engine, _bindToIP ) )   return forgeTCPOutboundPipe.notOk( "_engine or _bindToIP is null" );
            if( _finishConnectionTimeoutMs < 1 ) return forgeTCPOutboundPipe.notOk( "_finishConnectionTimeoutMs is not valid: " + _finishConnectionTimeoutMs );

            // open our channel and configure it...
            var channel = SocketChannel.open();
            channel.configureBlocking( false );
            channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );  // reuse connections in TIME_WAIT (e.g., after close and reconnect)...
            channel.setOption( StandardSocketOptions.SO_KEEPALIVE, true );  // enable keep-alive packets on this connection (mainly to detect broken connections)...

            // get our new instance...
            var pipe = new TCPOutboundPipe( _engine, channel, _bindToIP, _bindToPort, _finishConnectionTimeoutMs );
            return forgeTCPOutboundPipe.ok( pipe );
        }
        catch( Exception _e ) {
            return forgeTCPOutboundPipe.notOk( "Problem creating or configuring TCP outbound pipe: " + _e.getMessage(), _e );
        }
    }


    public static Outcome<TCPOutboundPipe> getTCPOutboundPipe( final NetworkingEngine _engine, final IPAddress _bindToIP, final int _bindToPort ) {
        return getTCPOutboundPipe( _engine, _bindToIP, _bindToPort, DEFAULT_FINISH_CONNECTION_TIMEOUT_MS );
    }


    protected TCPOutboundPipe( final NetworkingEngine _engine, final SocketChannel _channel,
                               final IPAddress _bindToIP, final int _bindToPort, final int _finishConnectionTimeoutMs ) throws IOException {
        super( _engine, _channel );

        finishConnectionTimeoutMs = _finishConnectionTimeoutMs;
        connectFlag               = new AtomicBoolean( false );
    }


    // ip address may NOT be wildcard
    public void connect( final IPAddress _ip, final int _port, final Consumer<Outcome<?>> _completionHandler ) {

        // if there's no completion handler, then we really can't do anything at all...
        if( isNull( _completionHandler ) ) throw new IllegalArgumentException( "_completionHandler is null" );

        try {
            // sanity checks...
            if( isNull( _ip ) )                   { postConnectionCompletion( _completionHandler, forge.notOk( "_ip is null" ));                      return; }
            if( (_port < 1) || (_port > 0xFFFF) ) { postConnectionCompletion( _completionHandler, forge.notOk( "_port is out of range: " + _port ) ); return; }

            // make sure we're not trying to connect more than once...
            if( connectFlag.getAndSet( true ) )   { postConnectionCompletion( _completionHandler, forge.notOk( "Connect has already been called" ) ); return; }

            // initiate the connection attempt, which may complete immediately...
            if( channel.connect( new InetSocketAddress( _ip.toInetAddress(), _port ) ) || channel.finishConnect() ) {
                postConnectionCompletion( _completionHandler, forge.ok() );
                LOGGER.finest( "Connected immediately to " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort() );
                return;
            }

            // we get here if the connection did not complete immediately, and so must be finished, which could take some time...
            finishConnectionStartTime = System.currentTimeMillis();     // record when we started the process of finishing the completion...
            finishConnectionIntervalMs = 1;  // we're going to check for connection completion in about a millisecond...
            engine.schedule( () -> checkConnection( _completionHandler ), Duration.ofMillis( finishConnectionIntervalMs ) );
        }
        catch( Exception _e ) {
            _completionHandler.accept( forge.notOk( "Problem connecting: " + _e.getMessage(), _e ) );
        }
    }


    public Outcome<?> connect( final IPAddress _ip, final int _port ) {
        Waiter<Outcome<?>> waiter = new Waiter<>();
        connect( _ip, _port, waiter::complete );
        return waiter.waitForCompletion();
    }


    public void close() {

    }


    private void postConnectionCompletion( final Consumer<Outcome<?>> _completionHandler, final Outcome<?> _outcome ) {
        engine.execute( () -> _completionHandler.accept( _outcome ) );
    }


    private void checkConnection( final Consumer<Outcome<?>> _completionHandler ) {

        try {

            LOGGER.finest( "checkConnection after " + finishConnectionIntervalMs + "ms" );

            // compute how long we have taken so far...
            var finishConnectionTimeMs = System.currentTimeMillis() - finishConnectionStartTime;

            // increase the interval between checks by 50% - but at least 1ms, and no more than 100ms...
            finishConnectionIntervalMs = Math.min( MAX_FINISH_CONNECTION_INTERVAL_INCREMENT_MS, finishConnectionIntervalMs + Math.max( 1, (finishConnectionIntervalMs >>> 1) ) );

            // if we've finished connecting, send the notice...
            if( channel.finishConnect() ) {
                LOGGER.finest( "Connected to " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort() + " in " + finishConnectionTimeMs + "ms" );
                _completionHandler.accept( forge.ok() );
            }

            // then see if we've run out of time...
            else if( finishConnectionTimeMs >= finishConnectionTimeoutMs ) {
                LOGGER.finest( "Connection attempt timed out" );
                _completionHandler.accept( forge.notOk( "Connection timed out: " + finishConnectionTimeMs + "ms" ) );
            }

            // otherwise, schedule another check...
            else {
                engine.schedule( () -> checkConnection( _completionHandler ), Duration.ofMillis( finishConnectionIntervalMs ) );
            }

        }
        catch( Exception _e ) {
            _completionHandler.accept( forge.notOk( "Problem connecting: " + _e.getMessage(), _e ) );
        }
    }


    /* package-private */ SocketChannel getChannel() {
        return channel;
    }


    public String toString() {
        return "TCPPipe: " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();
    }
}
