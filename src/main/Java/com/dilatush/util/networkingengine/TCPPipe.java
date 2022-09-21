package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

public class TCPPipe {

    private static final Logger                      LOGGER                                      = getLogger();
    private static final int                         DEFAULT_FINISH_CONNECTION_TIMEOUT_MS        = 2000;
    private static final int                         MAX_FINISH_CONNECTION_INTERVAL_INCREMENT_MS = 100;

    private static final Outcome.Forge<TCPPipe>      forgeTCPConnection = new Outcome.Forge<>();
    private static final Outcome.Forge<?>            forge              = new Outcome.Forge<>();


    private final SocketChannel    channel;
    private final AtomicBoolean    connectFlag;
    private final NetworkingEngine engine;
    private final int              finishConnectionTimeoutMs;

    private int  finishConnectionIntervalMs;  // delay (in milliseconds) until the next finish connection check...
    private long finishConnectionStartTime;   // when we started the finish connection process...


    /* package-private */ static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine, final SocketChannel _channel, final int _finishConnectionTimeoutMs ) {
        try {
            if( isNull( _channel, _engine ) ) return forgeTCPConnection.notOk( "_channel or _engine is null" );
            if( _finishConnectionTimeoutMs < 1 ) return forgeTCPConnection.notOk( "_finishConnectionTimeoutMs is not valid: " + _finishConnectionTimeoutMs );
            _channel.configureBlocking( false );
            _channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );  // reuse connections in TIME_WAIT (e.g., after close and reconnect)...
            _channel.setOption( StandardSocketOptions.SO_KEEPALIVE, true );  // enable keep-alive packets on this connection (mainly to detect broken connections)...
            return forgeTCPConnection.ok( new TCPPipe( _engine, _channel,_finishConnectionTimeoutMs ) );
        }
        catch( IOException _e ) {
            return forgeTCPConnection.notOk( "Problem configuring TCP pipe: " + _e.getMessage(), _e );
        }
    }


    /* package-private */ static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine, final SocketChannel _channel ) {
        return getTCPPipe( _engine, _channel, DEFAULT_FINISH_CONNECTION_TIMEOUT_MS );
    }


    /* package-private */ static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine,
                                                              final IPAddress _bindToIP, final int _bindToPort, final int _finishConnectionTimeoutMs ) {

        // sanity checks...
        if( isNull( _engine, _bindToIP ) )                return forgeTCPConnection.notOk( "_engine or _bindToIP is null" );
        if( (_bindToPort < 0) || (_bindToPort > 0xFFFF) ) return forgeTCPConnection.notOk( "_port is out of range: " + _bindToPort );
            if( _finishConnectionTimeoutMs < 1 )          return forgeTCPConnection.notOk( "_finishConnectionTimeoutMs is not valid: " + _finishConnectionTimeoutMs );

        // instantiate a socket channel...
        SocketChannel channel = null;
        try {
            channel = SocketChannel.open();
            channel.bind( new InetSocketAddress( _bindToIP.toInetAddress(), _bindToPort ) );
        }
        catch( IOException _e ) {
            return forgeTCPConnection.notOk( "Problem instantiating SocketChannel: " + _e.getMessage(), _e );
        }

        return getTCPPipe( _engine, channel, _finishConnectionTimeoutMs );
    }


    /* package-private */ static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine, final IPAddress _bindToIP, final int _bindToPort ) {
        return getTCPPipe( _engine, _bindToIP, _bindToPort, DEFAULT_FINISH_CONNECTION_TIMEOUT_MS );
    }


    private TCPPipe( final NetworkingEngine _engine, final SocketChannel _channel, final int _finishConnectionTimeoutMs ) {
        engine = _engine;
        finishConnectionTimeoutMs = _finishConnectionTimeoutMs;
        connectFlag = new AtomicBoolean( false );
        channel = _channel;
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
            engine.getScheduledExecutor().schedule( () -> checkConnection( _completionHandler ), Duration.ofMillis( finishConnectionIntervalMs ) );
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


    private void postConnectionCompletion( final Consumer<Outcome<?>> _completionHandler, final Outcome<?> _outcome ) {
        engine.getScheduledExecutor().execute( () -> _completionHandler.accept( _outcome ) );
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
                engine.getScheduledExecutor().schedule( () -> checkConnection( _completionHandler ), Duration.ofMillis( finishConnectionIntervalMs ) );
            }

        }
        catch( Exception _e ) {
            _completionHandler.accept( forge.notOk( "Problem connecting: " + _e.getMessage(), _e ) );
        }
    }


    /* package-private */ void register( final Selector _selector) throws ClosedChannelException {
        channel.register( _selector, 0, this );
    }


    /* package-private */ SocketChannel getChannel() {
        return channel;
    }


    public Socket getSocket() {
        return channel.socket();
    }


    public String toString() {
        return "TCPPipe: " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();
    }
}
