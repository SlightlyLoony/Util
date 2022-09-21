package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;
import static java.util.logging.Level.SEVERE;


/**
 * <p>Instances of this class (and associated classes in this package) implement a general purpose networking engine that supports unicast communications on TCP and UDP, using
 * either IPv4 or IPv6 IP addresses.  This class is based on NIO, which means it can support many simultaneous listeners and connections using just a few threads.  All methods
 * that may not complete immediately are available in either synchronous (blocking) or asynchronous (non-blocking) versions.</p>
 * <p>The general pattern of use is very straightforward:</p>
 * <ul>
 *     <li>First, create an instance of this class, and keep it alive.</li>
 *     <li>If you want to listen on a TCP port, create a new instance of {@link TCPListener} (using one of the {@code newTCPListener()} methods).  When a connection is made
 *     to the listener, it will automatically create and connect to an instance of {@link TCPPipe}, with which you may read and write data.</li>
 *     <li>If you want to make a connection to a listening TCP port, create a new instance of {@link TCPPipe} (using one of the {@code newTCPPipe()} methods), and use it to
 *     initiate the connection, read data, and write data.</li>
 *     <li>If you want to send or receive datagrams, create a new instance of {@link UDPPipe} (using one of the {@code newUDPPipe()} methods), then use that instance to
 *     receive or send datagrams.</li>
 * </ul>
 * <p>The three classes in this package that implement the actual communications ({@link TCPListener}, {@link TCPPipe}, and {@link UDPPipe}) may all be subclassed to provide
 * specialized communications.  For instance, you might extend {@link TCPListener} to make an {@code HTTPListener} as the core of a web server.  Similarly you might extend
 * {@link TCPPipe} to make an {@code HTTPPipe} to implement individual connections to your web server.</p>
 * <p>Note that it is permissible for a single process to have multiple instances of this class, all active at the same time.  While it is possible that doing so might increase
 * the overall networking performance, this has not been verified.</p>
 */
@SuppressWarnings( "unused" )
public final class NetworkingEngine {

    private static final Logger       LOGGER                    = getLogger();
    private static final int          DEFAULT_NUMBER_OF_THREADS = 3;   // default number of threads in the default ScheduledExecutor...

    private static final Outcome.Forge<NetworkingEngine> forgeNetworkingEngine = new Outcome.Forge<>();

    private final Thread               ioLoopThread;      // the thread that runs the I/O loop, which is carefully kept only to trivial activities...
    private final Selector             selector;          // the one and only selector for this engine...
    private final String               name;              // the name of this engine; the only intent is for human readability...
    private final ReentrantLock        selectorLock;      // a lock to prevent multiple threads changing the selector simultaneously...
    private final ScheduledExecutor    scheduledExecutor; // offloads tasks from the I/O loop, and is the basis for timeouts...


    /**
     * Attempts to create a new instance of this class with the given name and {@link ScheduledExecutor}, and returns the outcome.
     *
     * @param _name The human-readable name of this instance, which must be a string of non-zero length.  While there is no requirement for the name being unique amongst
     *              concurrent instances of this class, uniqueness is recommended.
     * @param _scheduledExecutor The {@link ScheduledExecutor} instance for this engine to use for offloading tasks from the I/O loop thread, and for scheduling timeouts.
     * @return The outcome of the attempt to create a new instance of this class.  If ok, the info is the newly created instance.  If not ok, there is an explanatory message
     * and possibly an exception that caused a problem.
     */
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


    /**
     * Attempts to create a new instance of this class with the given name and a new {@link ScheduledExecutor} with the default number of daemon worker threads (3), and returns
     * the outcome.
     *
     * @param _name The human-readable name of this instance, which must be a string of non-zero length.  While there is no requirement for the name being unique amongst
     *              concurrent instances of this class, uniqueness is recommended.
     * @return The outcome of the attempt to create a new instance of this class.  If ok, the info is the newly created instance.  If not ok, there is an explanatory message
     * and possibly an exception that caused a problem.
     */
    public static Outcome<NetworkingEngine> getInstance( final String _name ) {

        try {
            var scheduleExecutor = new ScheduledExecutor( DEFAULT_NUMBER_OF_THREADS );
            return getInstance( _name, scheduleExecutor );
        }
        catch( Exception _e ) {
            return forgeNetworkingEngine.notOk( "Problem creating ScheduledExecutor: " + _e.getMessage(), _e );
        }
    }


    /**
     * Private constructor that assumes the arguments have been verified.
     *
     * @param _name The human-readable name of this instance, which must be a string of non-zero length.  While there is no requirement for the name being unique amongst
     *              concurrent instances of this class, uniqueness is recommended.
     * @param _scheduledExecutor The {@link ScheduledExecutor} instance for this engine to use for offloading tasks from the I/O loop thread, and for scheduling timeouts.
     * @throws IOException if the selector cannot be opened.
     */
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


    public Outcome<TCPListener> newTCPListener( final IPAddress _ip, final int _port, final Consumer<TCPPipe> _onAccept ) {
        return TCPListener.getInstance( this, _ip, _port, _onAccept );
    }


    public Outcome<TCPListener> newTCPListener( final IPAddress _ip, final int _port ) {
        return TCPListener.getInstance( this, _ip, _port, null );
    }


    public SelectionKey register( final SelectableChannel _channel, final int _ops, final Object _attachment ) throws ClosedChannelException {

        selectorLock.lock();
        try {
            var key = _channel.register( selector, _ops, _attachment );
            selector.wakeup();
            return key;
        }
        finally {
            selectorLock.unlock();
        }
    }


    /**
     * Starts the I/O loop thread if it has not aleady been started.
     */
    private void start() {
        if( !ioLoopThread.isAlive() ) ioLoopThread.start();
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
