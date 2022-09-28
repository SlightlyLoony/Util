package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Abstract base class for all TCP pipes, each of which provides communications on a single TCP connection; subclasses implement specific kinds of TCP pipes.
 */
@SuppressWarnings( "unused" )
public abstract class TCPPipe {

    private static final Logger                      LOGGER                                      = getLogger();

    protected static final Outcome.Forge<?>            forge              = new Outcome.Forge<>();
    protected static final Outcome.Forge<ByteBuffer>   forgeByteBuffer    = new Outcome.Forge<>();

    // constants related to SelectionKey interest operations...
    protected static final int NO_INTEREST    = 0;
    protected static final int READ_INTEREST  = SelectionKey.OP_READ;
    protected static final int WRITE_INTEREST = SelectionKey.OP_WRITE;

    protected final SocketChannel                 channel;          // the channel for the TCP connection this instance abstracts...
    protected final NetworkingEngine              engine;           // the networking engine whose Selector our channel's SelectionKey is registered with...
    protected final SelectionKey                  key;              // the SelectionKey for our channel...
    protected final AtomicBoolean                 readInProgress;   // true when a read operation is in progress...
    protected final AtomicBoolean                 writeInProgress;  // true when a write operation is in progress...

    private ByteBuffer                    readBuffer;
    private int                           minBytes;
    private Consumer<Outcome<ByteBuffer>> onReadCompleteHandler;


    /**
     * Creates a new instance of this abstract base class, configured for inbound connections that have already been completed by the {@link TCPListener}. Socket options
     * SO_REUSEADDR and SO_KEEPALIVE are both set to {@code true}, and the channel's selection key (with no interests) is registered with the {@link NetworkingEngine}'s selector.
     *
     * @param _engine The {@link NetworkingEngine} whose selector our channel's selection key should be registered with.
     * @param _channel The {@link SocketChannel} accepted by {@link TCPListener}.
     * @throws IOException if any I/O error occurs
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws IllegalStateException if the channel is not already connected
     */
    protected TCPPipe( final NetworkingEngine _engine, final SocketChannel _channel ) throws IOException {

        // sanity checks...
        if( isNull( _engine, _channel ))
            throw new IllegalArgumentException( "_engine or _channel is null" );
        if( !_channel.isConnected() )
            throw new IllegalStateException( "_channel is not connected" );

        // some initialization...
        engine                    = _engine;
        channel                   = _channel;

        // configure our channel...
        channel.configureBlocking( false );
        channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );  // reuse connections in TIME_WAIT (e.g., after close and reconnect)...
        channel.setOption( StandardSocketOptions.SO_KEEPALIVE, true );  // enable keep-alive packets on this connection (mainly to detect broken connections)...

        // attempt to register a key for this instance with our engine, with (for now) no interest in any notifications...
        key                       = engine.register( channel, NO_INTEREST, this );

        // create and initialize our operation in progress and cancel flags...
        readInProgress  = new AtomicBoolean( false );
        writeInProgress = new AtomicBoolean( false );
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  The read buffer is cleared (via {@link ByteBuffer#clear()}) before reading, so the first byte read from the network is always at index
     * 0 in the read buffer, and the read buffer's position ({@link ByteBuffer#position()}) is always equal to the number of bytes read.  The {@code _onReadCompleteHandler} is
     * called when the read operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> the total number of bytes read is at least equal to the minimum number of bytes
     * specified in {@code _minBytes}.  If {@code _minBytes} is 1, that means the read operation will complete after the first packet is read.</p>
     * <p>Note that if the read buffer's capacity is less than the received data queued by the TCP/IP stack, then a read operation may fill the read buffer to its capacity, but
     * leave some data queued by the TCP/IP stack.  In cases like this, only <i>part</i> of a packet's data may be in the read buffer.  You cannot safely depend on the end of the
     * data in the read buffer being the same as the end of a packet.  Even if the read buffer wasn't completely filled, the TCP/IP stack can split the data any way it feels like
     * doing.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer is cleared by this method before any data is read.  While the read operation is in
     *                    progress (i.e, before the {@code _onReadCompleteHandler} is called), the read buffer must not be manipulated other than by this instance - hands off the
     *                    read buffer!
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  This must be at least 1, and no greater than the capacity
     *                  of the read buffer.
     * @param _onReadCompleteHandler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                            canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the network data read.  If not
     *                            ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public void read( final ByteBuffer _readBuffer, final int _minBytes, final Consumer<Outcome<ByteBuffer>> _onReadCompleteHandler ) {

        // if we didn't get a read ready handler, then we really don't have any choice but to throw an exception...
        if( isNull( _onReadCompleteHandler ) ) throw new IllegalArgumentException( "_onReadCompleteHandler is null" );

        // in the code below, the TCPPipeException exists only to make the code easier to read and understand...
        try {

            // sanity checks...
            if( isNull( _readBuffer ) || (_readBuffer.capacity() == 0) ) throw new TCPPipeException( "_readBuffer is null or has zero capacity" );
            if( (_minBytes < 1) || (_minBytes > _readBuffer.capacity())) throw new TCPPipeException( "_minBytes out of range: " + _minBytes );

            // make sure we haven't already got a read operation in progress...
            if( readInProgress.getAndSet( true ) ) throw new TCPPipeException( "Read operation already in progress" );

            // clear the read buffer, so we know it's ready to read into, and squirrel away our read state...
            _readBuffer.clear();
            minBytes              = _minBytes;
            readBuffer            = _readBuffer;
            onReadCompleteHandler = _onReadCompleteHandler;

            // initiate the actual read process...
            read();
        }
        catch( TCPPipeException _e ) {
            postReadCompletion( forgeByteBuffer.notOk( _e.getMessage() ) );
        }
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  The read buffer is cleared (via {@link ByteBuffer#clear()}) before reading, so the first byte read from the network is always at index
     * 0 in the read buffer, and the read buffer's position ({@link ByteBuffer#position()}) is always equal to the number of bytes read.  The {@code _onReadCompleteHandler} is
     * called when the read operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> at least one byte is read, which means the read operation will complete after
     * the first packet is read.</p>
     * <p>Note that if the read buffer's capacity is less than the received data queued by the TCP/IP stack, then a read operation may fill the read buffer to its capacity, but
     * leave some data queued by the TCP/IP stack.  In cases like this, only <i>part</i> of a packet's data may be in the read buffer.  You cannot safely depend on the end of the
     * data in the read buffer being the same as the end of a packet.  Even if the read buffer wasn't completely filled, the TCP/IP stack can split the data any way it feels like
     * doing.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer is cleared by this method before any data is read.  While the read operation is in
     *                    progress (i.e, before the {@code _onReadCompleteHandler} is called), the read buffer must not be manipulated other than by this instance - hands off the
     *                    read buffer!
     * @param _onReadCompleteHandler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                            canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the network data read.  If not
     *                            ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public void read( final ByteBuffer _readBuffer, final Consumer<Outcome<ByteBuffer>> _onReadCompleteHandler ) {
        read( _readBuffer, 1, _onReadCompleteHandler );
    }


    /**
     * <p>Performs a synchronous (blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  The read buffer is cleared (via {@link ByteBuffer#clear()}) before reading, so the first byte read from the network is always at index
     * 0 in the read buffer, and the read buffer's position ({@link ByteBuffer#position()}) is always equal to the number of bytes read.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> the total number of bytes read is at least equal to the minimum number of bytes
     * specified in {@code _minBytes}.  If {@code _minBytes} is 1, that means the read operation will complete after the first packet is read.</p>
     * <p>Note that if the read buffer's capacity is less than the received data queued by the TCP/IP stack, then a read operation may fill the read buffer to its capacity, but
     * leave some data queued by the TCP/IP stack.  In cases like this, only <i>part</i> of a packet's data may be in the read buffer.  You cannot safely depend on the end of the
     * data in the read buffer being the same as the end of a packet.  Even if the read buffer wasn't completely filled, the TCP/IP stack can split the data any way it feels like
     * doing.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer is cleared by this method before any data is read.  While the read operation is in
     *                    progress, the read buffer must not be manipulated other than by this instance - hands off the read
     *                    buffer!
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  This must be at least 1, and no greater than the capacity
     *                  of the read buffer.
     * @return The outcome of this operation.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the network data read.  If not
     *         ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public Outcome<ByteBuffer> read( final ByteBuffer _readBuffer, final int _minBytes ) {

        Waiter<Outcome<ByteBuffer>> waiter = new Waiter<>();
        read( _readBuffer, _minBytes, waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * <p>Performs a synchronous (blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  The read buffer is cleared (via {@link ByteBuffer#clear()}) before reading, so the first byte read from the network is always at index
     * 0 in the read buffer, and the read buffer's position ({@link ByteBuffer#position()}) is always equal to the number of bytes read.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> at least one byte is read, which means the read operation will complete after
     * the first packet is read.</p>
     * <p>Note that if the read buffer's capacity is less than the received data queued by the TCP/IP stack, then a read operation may fill the read buffer to its capacity, but
     * leave some data queued by the TCP/IP stack.  In cases like this, only <i>part</i> of a packet's data may be in the read buffer.  You cannot safely depend on the end of the
     * data in the read buffer being the same as the end of a packet.  Even if the read buffer wasn't completely filled, the TCP/IP stack can split the data any way it feels like
     * doing.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer is cleared by this method before any data is read.  While the read operation is in
     *                    progress, the read buffer must not be manipulated other than by this instance - hands off the read
     *                    buffer!
     * @return The outcome of this operation.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the network data read.  If not
     *         ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public Outcome<ByteBuffer> read( final ByteBuffer _readBuffer ) {
        return read( _readBuffer, 1 );
    }


    /**
     * Called by the {@link NetworkingEngine} whenever read interest is indicated in the selection key, and the engine detects that the connection is readable.
     */
    /* package-private */ void onReadable() {

        // if there's no read in progress, we just log and ignore this call...
        if( !readInProgress.get() ) {
            LOGGER.log( Level.WARNING, "TCPPipe::onReadable() called when no read was in progress; ignoring" );
            return;
        }

        // handle the readable event...
        read();
    }


    /**
     * Implements the core of the read operation.  It is called once when the read is initiated, and again for each readable event detected by the engine...
     */
    private void read() {

        try {
            // read what data we can...
            var bytesRead = channel.read( readBuffer );  // this can throw an IOException...

            // if the total bytes read is at least the minimum number of bytes, then we're done...
            if( readBuffer.position() >= minBytes ) {
                postReadCompletion( forgeByteBuffer.ok( readBuffer ) );
            }

            // otherwise, we need to express read interest...
            else {
                key.interestOpsOr( READ_INTEREST );
                engine.wakeSelector();  // this guarantees that the key change will be effective immediately...
            }
        }
        catch( IOException _e ) {
            postReadCompletion( forgeByteBuffer.notOk( "Problem reading from channel: " + _e.getMessage(), _e ) );
        }
    }


    /**
     * If a read operation is still in progress, marks it as complete (i.e., {@code readInProgress} set to false) and posts the given {@link Outcome} to the
     * {@code onReadReadyHandler}.  If no read operation was in progress, this method does nothing.
     *
     * @param _outcome The {@link Outcome} to post.
     */
    private void postReadCompletion( final Outcome<ByteBuffer> _outcome ) {

        // if there was no read in progress, just return...
        // this catches a race condition when a cancelRead occurs just as a read is completing normally...
        // or when a read "completes" just after a cancelRead...
        if( !readInProgress.getAndSet( false ) ) return;

        // otherwise, send the completion...
        engine.execute( () -> onReadCompleteHandler.accept( _outcome ) );
    }


    /**
     * Cancels a read operation in progress.
     */
    public void cancelRead() {
        postReadCompletion( forgeByteBuffer.notOk( "Read canceled" ) );
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


    public void close() {

    }


    protected static class TCPPipeException extends Exception {
        public TCPPipeException( final String message ) {
            super( message );
        }
    }
}
