package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ScheduledExecutor;

import java.io.IOException;
import java.nio.channels.*;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;
import static java.util.logging.Level.SEVERE;


/**
 * <p>Instances of this class (and associated classes in this package) implement a general purpose networking engine that supports unicast communications on TCP and UDP, using
 * either IPv4 or IPv6 IP addresses.  This class is based on NIO, which means it can support many simultaneous listeners and connections using just a few threads.  All methods
 * that may not complete immediately are available in either synchronous (blocking) or asynchronous (non-blocking) versions.</p>
 * <p>Note that it is permissible for a single process to have multiple instances of this class, all active at the same time.  While it is possible that doing so might increase
 * the overall networking performance, this has not been verified.</p>
 * <p>Internally, instance of this class use a {@link ScheduledExecutor} for two purposes: (1) to offload non-trivial tasks from the I/O loop thread, and (2) to support
 * timeouts.  The {@link ScheduledExecutor} uses a thread pool with a fixed number of threads, and an unbounded queue, so it is important to make sure that the tasks it
 * executes are not blocking or compute intensive, and that there are sufficient threads to handle the number of concurrent tasks it might be asked to process.  If a
 * {@link ScheduledExecutor} is not provided at instantiation, a default {@link ScheduledExecutor} with 3 threads will be used.</p>
 */
@SuppressWarnings( "unused" )
public final class NetworkingEngine {

    private static final Logger       LOGGER                    = getLogger();
    private static final int          DEFAULT_NUMBER_OF_THREADS = 3;   // number of threads in the default ScheduledExecutor...

    private static final int ALL_INTERESTS          = SelectionKey.OP_READ | SelectionKey.OP_ACCEPT | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
    private static final int NO_READ_INTEREST       = ALL_INTERESTS ^ SelectionKey.OP_READ;
    private static final int NO_WRITE_INTEREST      = ALL_INTERESTS ^ SelectionKey.OP_WRITE;
    private static final int NO_ACCEPTABLE_INTEREST = ALL_INTERESTS ^ SelectionKey.OP_ACCEPT;

    private static final Outcome.Forge<NetworkingEngine> forgeNetworkingEngine = new Outcome.Forge<>();

    private final Thread               ioLoopThread;      // the thread that runs the I/O loop, which is carefully kept only to trivial activities...
    private final Selector             selector;          // the one and only selector for this engine...
    private final String               name;              // the name of this engine; the only intent is for human readability...
    private final ReentrantLock        selectorLock;      // a lock to prevent multiple threads changing the selector simultaneously...
    private final ScheduledExecutor    scheduledExecutor; // offloads tasks from the I/O loop, and is the basis for timeouts...
    private final AtomicBoolean        isShutdown;          // set true if this engine has been shutdown...


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
            engine.ioLoopThread.start();
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
        isShutdown = new AtomicBoolean( false );
    }


    /**
     * Shutdown the networking engine, ending all related I/O.  If this method is called before any associated channels are closed, those channels may get errors.
     */
    public void shutdown() {

        isShutdown.set( true );
        ioLoopThread.interrupt();
        try {
            selector.close();
        }
        catch( IOException _e ) {
            // ignore...
        }
    }


    /**
     * Register a new selection key for the given channel, with the given initial operations interest set and the given (optional) attachment.  This method both registers the new
     * selection key and wakes up the selector to ensure the key takes immediate effect.
     *
     * @param _channel The channel to register a new selection key for.
     * @param _ops The initial operations interest set for the new key.
     * @param _attachment The optional attachment on the new key (the attachment may be null).
     * @return The new {@link SelectionKey}.
     * @throws ClosedChannelException if this method is called when a key for the given channel has already been registered, but was cancelled.
     */
    /* package-private */ SelectionKey register( final SelectableChannel _channel, final int _ops, final Object _attachment ) throws ClosedChannelException {

        try {
            // lock out concurrent access for other threads...
            selectorLock.lock();

            // the actual registration...
            var key = _channel.register( selector, _ops, _attachment );

            // make certain the new key has immediate effect...
            selector.wakeup();

            // and we're done...
            return key;
        }
        finally {
            // release the lock...
            selectorLock.unlock();
        }
    }


    /**
     * The main I/O loop for the network engine.  In normal operation the {@code while()} loop will run forever.  Note that all code executed in this loop is trivially simple.
     * This is intentional, to guarantee the performance within the I/O loop.  Do not make changes that upset this apple cart!
     */
    @SuppressWarnings( "ConstantConditions" )
    private void ioLoop() {

        LOGGER.finest( "I/O Loop starting..." );

        try {

            // we're going to loop here basically forever, unless something goes horribly wrong...
            while( !ioLoopThread.isInterrupted() ) {

                LOGGER.finest( "Selecting..." );

                // select and get any keys...
                try {
                    selector.select();
                    if( !selector.isOpen() ) break;
                }
                catch( IOException _e ) {
                    LOGGER.log( SEVERE, "I/O error when selecting", _e );
                    throw new IllegalStateException( "Selector I/O errors doom the NetworkingEngine" );
                }

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
                            key.interestOpsAnd( NO_ACCEPTABLE_INTEREST );
                            LOGGER.finest( "Acceptable with TCPListener: " + listener );
                            scheduledExecutor.execute( listener::onAcceptable );
                        }
                        else {
                            LOGGER.warning( "Acceptable interest with unknown attachment type: " + key.attachment().getClass().getName() );
                        }
                    }

                    // handle connecting (TCP only)...
                    if( key.isValid() && key.isConnectable() ) {
                        LOGGER.finest( "Connectable" );
                    }

                    // handle writing to the network...
                    if( key.isValid() && key.isWritable() ) {
                        LOGGER.finest( "Writable" );
                        if( key.attachment() instanceof TCPPipe pipe ) {
                            key.interestOpsAnd( NO_WRITE_INTEREST );
                            scheduledExecutor.execute( pipe::onWriteable );
                        }
                        else if( key.attachment() instanceof UDPBase udpBase ) {
                            key.interestOpsAnd( NO_WRITE_INTEREST );
                            scheduledExecutor.execute( udpBase::onWriteable );
                        }
                        else {
                            LOGGER.warning( "Writeable interest with unknown attachment type: " + key.attachment().getClass().getName() );
                        }
                    }

                    // handle reading from the network...
                    if( key.isValid() && key.isReadable() ) {
                        LOGGER.finest( "Readable" );
                        if( key.attachment() instanceof TCPPipe pipe ) {
                            key.interestOpsAnd( NO_READ_INTEREST );
                            scheduledExecutor.execute( pipe::onReadable );
                        }
                        else if( key.attachment() instanceof UDPBase udpBase ) {
                            key.interestOpsAnd( NO_READ_INTEREST );
                            scheduledExecutor.execute( udpBase::onReadable );
                        }
                        else {
                            LOGGER.warning( "Readable interest with unknown attachment type: " + key.attachment().getClass().getName() );
                        }
                    }

                    // get rid the key we just processed...
                    keyIterator.remove();
                }
            }
        }
        catch( ClosedSelectorException _e ) {
            LOGGER.log( SEVERE, "Selector closed", _e );
        }

        // getting here means something seriously wrong happened; log and let the loop die...
        catch( Exception _e ) {
            LOGGER.log( SEVERE, "Unhandled exception in NIO selector loop", _e );
        }
        finally {
            if( isShutdown.get() )
                LOGGER.finest( "Network engine shut down" );
            else
                LOGGER.severe( "Fatal NetworkingEngine error; exiting I/O loop" );

            // all hope is lost; terminate this thread...
            ioLoopThread.interrupt();
        }
    }


    /* package-private */ public void wakeSelector() {
        selector.wakeup();
    }


    /**
     * Returns the name of this instance.
     *
     * @return The name of this instance.
     */
    public String getName() {
        return name;
    }


    /**
     * Schedule the given task to execute in another thread one time after the given delay.
     *
     * @param _task The {@link Runnable} task to execute.
     * @param _delay The {@link Duration} minimum delay until the task will be executed.
     * @return The {@link ScheduledFuture} representing pending completion of the task and whose get() method will return null upon completion.
     */
    /* package-private */
    @SuppressWarnings( "UnusedReturnValue" )
    ScheduledFuture<?> schedule( final Runnable _task, final Duration _delay ) {
        return scheduledExecutor.schedule( _task, _delay );
    }


    /**
     * Execute the given task in another thread as soon as one becomes available.
     *
     * @param _task The {@link Runnable} task to execute.
     */
    /* package-private */ void execute( final Runnable _task ) {
        scheduledExecutor.execute( _task );
    }


    /**
     * Returns a string representing this instance.
     * @return a string representing this instance.
     */
    public String toString() {
        return "NetworkingEngine " + name;
    }


    @Override
    public boolean equals( final Object _o ) {

        if( this == _o ) return true;
        if( _o == null || getClass() != _o.getClass() ) return false;
        NetworkingEngine that = (NetworkingEngine) _o;
        return ioLoopThread.equals( that.ioLoopThread ) &&
                selector.equals( that.selector ) &&
                name.equals( that.name ) &&
                selectorLock.equals( that.selectorLock ) &&
                scheduledExecutor.equals( that.scheduledExecutor );
    }


    @Override
    public int hashCode() {
        return Objects.hash( ioLoopThread, selector, name, selectorLock, scheduledExecutor );
    }
}
