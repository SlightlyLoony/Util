package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
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

    private static final Logger                      LOGGER                               = getLogger();
    private static final int                         DEFAULT_FINISH_CONNECTION_TIMEOUT_MS = 2000;

    private static final Outcome.Forge<TCPPipe>      forgeTCPConnection = new Outcome.Forge<>();
    private static final Outcome.Forge<?>            forge              = new Outcome.Forge<>();


    private final SocketChannel    channel;
    private final AtomicBoolean    connectFlag;
    private final NetworkingEngine engine;

    private int finishConnectionIntervalMs;
    private int finishConnectionTimeoutMs;
    private int finishConnectionTimeMs;


    public static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine, final SocketChannel _channel, final int _finishConnectionTimeoutMs ) {
        try {
            if( isNull( _channel, _engine ) ) return forgeTCPConnection.notOk( "_channel or _engine is null" );
            if( _finishConnectionTimeoutMs < 1 ) return forgeTCPConnection.notOk( "_finishConnectionTimeoutMs is not valid: " + _finishConnectionTimeoutMs );
            _channel.configureBlocking( false );
            return forgeTCPConnection.ok( new TCPPipe( _engine, _channel, _finishConnectionTimeoutMs ) );
        }
        catch( IOException _e ) {
            return forgeTCPConnection.notOk( "Problem configuring TCP pipe: " + _e.getMessage(), _e );
        }
    }


    public static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine, final SocketChannel _channel ) {
        return getTCPPipe( _engine, _channel, DEFAULT_FINISH_CONNECTION_TIMEOUT_MS );
    }


    public static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine, final int _finishConnectionTimeoutMs ) {

        // instantiate a socket channel...
        SocketChannel channel = null;
        try {
            channel = SocketChannel.open();
        }
        catch( IOException _e ) {
            return forgeTCPConnection.notOk( "Problem instantiating SocketChannel: " + _e.getMessage(), _e );
        }

        return getTCPPipe( _engine, channel, _finishConnectionTimeoutMs );
    }


    public static Outcome<TCPPipe> getTCPPipe( final NetworkingEngine _engine ) {
        return getTCPPipe( _engine, DEFAULT_FINISH_CONNECTION_TIMEOUT_MS );
    }


    private TCPPipe( final NetworkingEngine _engine, final SocketChannel _channel, final int _finishConnectionTimeoutMs ) {
        engine = _engine;
        finishConnectionTimeoutMs = _finishConnectionTimeoutMs;
        connectFlag = new AtomicBoolean( false );
        channel = _channel;
    }


    public void connect( final IPAddress _ip, final int _port, final Consumer<Outcome<?>> _completionHandler ) {

        // sanity checks...
        if( isNull( _ip ) ) {
            _completionHandler.accept( forge.notOk( "_ip is null" ) );
            return;
        }
        if( (_port < 1) || (_port > 0xFFFF)) {
            _completionHandler.accept( forge.notOk( "_port is out of range: " + _port ) );
            return;
        }

        // make sure we're not trying to connect more than once...
        if( connectFlag.getAndSet( true ) ) _completionHandler.accept( forge.notOk( "Connect has already been called" ) );

        try {

            // initiate the connection attempt, which may complete immediately...
            if( channel.connect( new InetSocketAddress( _ip.toInetAddress(), _port ) ) ) {
                _completionHandler.accept( forge.ok() );
                LOGGER.finest( "Connected immediately to " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort() );
            }

            // we get here if the connection did not complete immediately, and so must be finished, which could take some time...
            else {

                // start checking with an interval of 1 millisecond...
                finishConnectionIntervalMs = 1;
                finishConnectionTimeMs = 0;
                engine.getScheduledExecutor().schedule( () -> checkConnection( _completionHandler ), Duration.ofMillis( finishConnectionIntervalMs ) );
            }
        }
        catch( IOException _e ) {
            _e.printStackTrace();
        }
    }


    public Outcome<?> connect( final IPAddress _ip, final int _port ) {
        Waiter<Outcome<?>> waiter = new Waiter<>();
        connect( _ip, _port, waiter::complete );
        return waiter.waitForCompletion();
    }


    private void checkConnection( final Consumer<Outcome<?>> _completionHandler ) {

        try {
            // if we've finished connecting, send the notice...
            if( channel.finishConnect() ) {
                LOGGER.finest( "Connected to " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort()
                        + " in " + (finishConnectionTimeMs + finishConnectionIntervalMs) + "ms" );
                _completionHandler.accept( forge.ok() );
            }

            // then see if we've run out of time...
            else if( finishConnectionTimeMs >= finishConnectionTimeoutMs ) {
                _completionHandler.accept( forge.notOk( "Connection timed out: " + finishConnectionTimeMs + "ms" ) );
            }

            // otherwise, schedule another check...
            else {
                finishConnectionTimeMs += finishConnectionIntervalMs;
                finishConnectionIntervalMs += Math.min( 100, Math.max( 1, finishConnectionIntervalMs + (finishConnectionIntervalMs >>> 1) ) );
                engine.getScheduledExecutor().schedule( () -> checkConnection( _completionHandler ), Duration.ofMillis( finishConnectionIntervalMs ) );
            }

        }
        catch( IOException _e ) {
            _e.printStackTrace();
        }
    }


    /* package-private */ void register( final Selector _selector) throws ClosedChannelException {
        channel.register( _selector, 0, this );
    }


    /* package-private */ SocketChannel getChannel() {
        return channel;
    }


    public String toString() {
        return "TCPPipe: " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();
    }
}
