package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;
import static java.util.logging.Level.SEVERE;

// TODO: check out options on SocketChannel; definitely want keep-alive...
// TODO: external-facing API must have both blocking and non-blocking methods...
// TODO: make class that makes it easy to write blocking methods that use non-blocking methods...

public final class NetworkingEngine {

    private static final Logger       LOGGER                    = getLogger();
    private static final int          DEFAULT_NUMBER_OF_THREADS = 3;

    private static final Outcome.Forge<NetworkingEngine> forgeNetworkingEngine = new Outcome.Forge<>();
    private static final Outcome.Forge<TCPListener>      forgeTCPListener = new Outcome.Forge<>();

    private final Thread               ioLoopThread;
    private final Selector             selector;
    private final String               name;
    private final ReentrantLock        selectorLock;
    private final ScheduledExecutor    scheduledExecutor;


    public static Outcome<NetworkingEngine> getInstance( final String _name, final ScheduledExecutor _scheduledExecutor ) {

        // sanity checks...
        if( isEmpty( _name ) ) return forgeNetworkingEngine.notOk( "no name" );
        if( isNull( _scheduledExecutor ) ) return forgeNetworkingEngine.notOk( "_scheduledExecutor is null" );

        // get an engine and start it up...
        try {
            var engine = new NetworkingEngine( _name, _scheduledExecutor );
            engine.start();
            return forgeNetworkingEngine.ok( engine );
        }
        catch( IOException _e ) {
            return forgeNetworkingEngine.notOk( "Could not open selector", _e );
        }
        catch( Exception _e ) {
            return forgeNetworkingEngine.notOk( "Problem instantiating or starting networking engine", _e );
        }
    }


    public static Outcome<NetworkingEngine> getInstance( final String _name ) {
        return getInstance( _name, new ScheduledExecutor( DEFAULT_NUMBER_OF_THREADS ) );
    }


    private NetworkingEngine( final String _name, final ScheduledExecutor _scheduledExecutor ) throws IOException {

        name = _name;
        scheduledExecutor = _scheduledExecutor;

        // get our selector...
        selector = Selector.open();

        // get our I/O loop thread...
        ioLoopThread = new Thread( this::ioLoop, _name + "-I/O Loop" );
        ioLoopThread.setDaemon( true );

        // some setup...
        selectorLock = new ReentrantLock();
    }


    public Outcome<TCPListener> newTCPListener( final IPAddress _ip, final int _port ) {

        // sanity checks...
        if( isNull( _ip ) ) return forgeTCPListener.notOk( "_ip is null" );
        if( (_port < 1) || (_port > 0xFFFF)) return forgeTCPListener.notOk( "_port is out of range: " + _port );

        // return with our listener...
        try {
            return forgeTCPListener.ok( new TCPListener( this, _ip, _port ) );
        }
        catch( IOException _e ) {
            return forgeTCPListener.notOk( "Problem instantiating TCPListener: " + _e.getMessage(), _e );
        }
    }


    public SelectionKey register( final SelectableChannel _channel, final int _ops, final Object _attachment ) throws ClosedChannelException {

        selectorLock.lock();
        try {
            selector.wakeup();
            return _channel.register( selector, _ops, _attachment );
        }
        finally {
            selectorLock.unlock();
        }
    }


    private void start() {
        ioLoopThread.start();
    }


    /**
     * The main I/O loop for the network engine.  In normal operation the {@code while()} loop will run forever.
     */
    private void ioLoop() {

        LOGGER.finest( "I/O Loop starting..." );

        // we're going to loop here basically forever...
        while( !ioLoopThread.isInterrupted() ) {

            // any exceptions in this code are a serious problem; if we get one, we just log it and make no attempt to recover...
            try {

                LOGGER.finest( "Selecting..." );

                // select and get any keys...
                selector.select();

                // iterate over any selected keys, and handle them...
                Set<SelectionKey> keys = selector.selectedKeys();
                LOGGER.finest( "Selected keys: " + keys.size() );
                Iterator<SelectionKey> keyIterator = keys.iterator();
                while( keyIterator.hasNext() ) {

                    // get the next key...
                    SelectionKey key = keyIterator.next();

                    // handle accepting connection (TCP listeners only)...
                    if( key.isValid() && key.isAcceptable() ) {

                        if( key.attachment() instanceof TCPListener listener ) {
                            scheduledExecutor.execute( listener::onAcceptable );
                            LOGGER.finest( "Acceptable with TCPListener: " + listener );
                        }
                        else
                            LOGGER.severe( "Key is acceptable, but attachment is not a TCPListener" );
                    }

                    // handle connecting (TCP only)...
                    if( key.isValid() && key.isConnectable() ) {
                        LOGGER.finest( "Connectable" );
                    }

                    // handle writing to the network...
                    if( key.isValid() && key.isWritable() ) {
                        LOGGER.finest( "Writable" );
                    }

                    // handle reading from the network...
                    if( key.isValid() && key.isReadable() ) {
                        LOGGER.finest( "Readable" );
                        SocketChannel serverClient = (SocketChannel) key.channel();
                        ByteBuffer serverBuffer = ByteBuffer.allocate(256);
//                        serverClient.read( serverBuffer );
                        LOGGER.finest( "Read: " + serverBuffer.position() );

                        serverClient.hashCode();
                    }

                    // get rid the key we just processed...
                    keyIterator.remove();
                }
            }

            // getting here means something seriously wrong happened; log and let the loop die...
            catch( Throwable _e ) {

                LOGGER.log( SEVERE, "Unhandled exception in NIO selector loop", _e );

                // this will cause the IO Runner thread to exit, and all I/O will cease...
                break;
            }
        }

        LOGGER.finest( "I/O Loop terminating" );
    }


    public Selector getSelector() {

        return selector;
    }


    public String getName() {

        return name;
    }


    public ScheduledExecutor getScheduledExecutor() {

        return scheduledExecutor;
    }


    public String toString() {
        return "Networking Engine " + name;
    }
}
