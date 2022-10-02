package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.Waiter;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    protected final SocketChannel    channel;                 // the channel for the TCP connection this instance abstracts...
    protected final NetworkingEngine engine;                  // the networking engine whose Selector our channel's SelectionKey is registered with...
    protected final SelectionKey     key;                     // the SelectionKey for our channel...

    private final AtomicBoolean    readInProgress;          // true when a read operation is in progress...
    private final AtomicBoolean    writeInProgress;         // true when a write operation is in progress...
    private final AtomicLong       cumulativeBytesRead;     // total number of bytes read by this instance since instantiation or counters cleared...
    private final AtomicLong       cumulativeBytesWritten;  // total number of bytes written by this instance since instantiation or counters cleared...
    private final AtomicInteger    bytesRead;               // the number of bytes read in the current read operation...
    private final AtomicInteger    bytesWritten;            // the number of bytes written in the current write operation...

    private ByteBuffer                    readBuffer;
    private ByteBuffer                    writeBuffer;
    private int                           minBytes;
    private Consumer<Outcome<ByteBuffer>> onReadCompleteHandler;
    private Consumer<Outcome<?>>          onWriteCompleteHandler;


    /**
     * Creates a new instance of this abstract base class. Socket options SO_REUSEADDR and SO_KEEPALIVE are both set to {@code true}, and the channel's selection key (with no
     * interests) is registered with the {@link NetworkingEngine}'s selector.
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

        // create and initialize our bytes read and written accumulators...
        cumulativeBytesRead    = new AtomicLong( 0 );
        cumulativeBytesWritten = new AtomicLong( 0 );
        bytesRead              = new AtomicInteger( 0 );
        bytesWritten           = new AtomicInteger( 0 );
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  The {@code _onReadCompleteHandler} is called when the read operation completes, whether that operation completed normally, was
     * terminated because of an error, or was canceled.  Note that the {@code _onReadCompleteHandler} will always be called in one of the threads from the associated
     * {@link NetworkingEngine}'s {@link ScheduledExecutor}, never in the thread that calls this method.</p>
     * <p>If the given read buffer's position is zero and its limit is equal to its capacity (in other words, the buffer appears to be cleared), then the read buffer is used as is.
     * However, if the position is not zero, or the limit is less than the capacity, then the buffer is assumed to have valid (but not processed) data between its position and its
     * limit.  In that case, the buffer will be compacted (i.e., {@link ByteBuffer#compact()} will be called) before reading, so those unprocessed bytes will be the first bytes in
     * the buffer after the read.  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()} is called), so the buffer's position upon completion
     * will always be zero, and its limit will indicate the end of the data previously unprocessed along with the network data just read.  Because the read buffer may have already
     * contained data when this method is called, the number of bytes in the read buffer upon completion may be greater than the number of bytes actually read.  If the number of
     * bytes actually read is needed, the {@link #getBytesRead()} method can be used (upon the read operation completing) to obtain that value.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> the total number of bytes read is at least equal to the minimum number of bytes
     * specified in {@code _minBytes}.  If {@code _minBytes} is 1, that means the read operation will complete after the first successful read operation - which could be part of
     * a packet, exactly one packet, or multiple packets with the last one possibly not completely read.  The code calling this method should always treat the data read as a
     * stream, and not a series of discrete packets.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer may be compacted by this method before any data is read.  While the read operation is
     *                    in  progress (i.e, before the {@code _onReadCompleteHandler} is called), the read buffer must not be manipulated other than by this instance - hands off
     *                    the read buffer!
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  This must be at least 1, and no greater than the capacity
     *                  of the read buffer.
     * @param _onReadCompleteHandler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                            canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the network data read.  If not
     *                            ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public void read( final ByteBuffer _readBuffer, final int _minBytes, final Consumer<Outcome<ByteBuffer>> _onReadCompleteHandler ) {

        // if we didn't get a read complete handler, then we really don't have any choice but to throw an exception...
        if( isNull( _onReadCompleteHandler ) ) throw new IllegalArgumentException( "_onReadCompleteHandler is null" );

        // in the code below, the TCPPipeException exists only to make the code easier to read and understand...
        try {

            // sanity checks...
            if( isNull( _readBuffer ) || (_readBuffer.capacity() == 0) ) throw new TCPPipeException( "_readBuffer is null or has zero capacity" );
            if( (_minBytes < 1) || (_minBytes > _readBuffer.capacity())) throw new TCPPipeException( "_minBytes out of range: " + _minBytes );

            // make sure we haven't already got a read operation in progress...
            if( readInProgress.getAndSet( true ) ) throw new TCPPipeException( "Read operation already in progress" );

            // ready the read buffer and squirrel away our read state...
            if( (_readBuffer.position() > 0) || (_readBuffer.limit() < _readBuffer.capacity()) )
                _readBuffer.compact();
            minBytes              = _minBytes;
            readBuffer            = _readBuffer;
            onReadCompleteHandler = _onReadCompleteHandler;
            bytesRead.set( 0 );

            // initiate the actual read process...
            read();
        }
        catch( TCPPipeException _e ) {
            postReadCompletion( forgeByteBuffer.notOk( _e.getMessage() ) );
        }
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  The {@code _onReadCompleteHandler} is called when the read operation completes, whether that operation completed normally, was
     * terminated because of an error, or was canceled.  Note that the {@code _onReadCompleteHandler} will always be called in one of the threads from the associated
     * {@link NetworkingEngine}'s {@link ScheduledExecutor}, never in the thread that calls this method.</p>
     * <p>If the given read buffer's position is zero and its limit is equal to its capacity (in other words, the buffer appears to be cleared), then the read buffer is used as is.
     * However, if the position is not zero, or the limit is less than the capacity, then the buffer is assumed to have valid (but not processed) data between its position and its
     * limit.  In that case, the buffer will be compacted (i.e., {@link ByteBuffer#compact()} will be called) before reading, so those unprocessed bytes will be the first bytes in
     * the buffer after the read.  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()} is called), so the buffer's position upon completion
     * will always be zero, and its limit will indicate the end of the data previously unprocessed along with the network data just read.  Because the read buffer may have already
     * contained data when this method is called, the number of bytes in the read buffer upon completion may be greater than the number of bytes actually read.  If the number of
     * bytes actually read is needed, the {@link #getBytesRead()} method can be used (upon the read operation completing) to obtain that value.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> the at least one byte has been read from the network.  That means the read
     * operation will complete after the first successful read operation - which could be part of a packet, exactly one packet, or multiple packets with the last one possibly not
     * completely read.  The code calling this method should always treat the data read as a stream, and not a series of discrete packets.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer may be compacted by this method before any data is read.  While the read operation is
     *                    in  progress (i.e, before the {@code _onReadCompleteHandler} is called), the read buffer must not be manipulated other than by this instance - hands off
     *                    the read buffer!
     * @param _onReadCompleteHandler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                            canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the network data read.  If not
     *                            ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public void read( final ByteBuffer _readBuffer, final Consumer<Outcome<ByteBuffer>> _onReadCompleteHandler ) {
        read( _readBuffer, 1, _onReadCompleteHandler );
    }


    /**
     * <p>Initiates a synchronous (blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  This method will return when the operation is complete, whether it completed normally, with an error, or was canceled</p>
     * <p>If the given read buffer's position is zero and its limit is equal to its capacity (in other words, the buffer appears to be cleared), then the read buffer is used as is.
     * However, if the position is not zero, or the limit is less than the capacity, then the buffer is assumed to have valid (but not processed) data between its position and its
     * limit.  In that case, the buffer will be compacted (i.e., {@link ByteBuffer#compact()} will be called) before reading, so those unprocessed bytes will be the first bytes in
     * the buffer after the read.  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()} is called), so the buffer's position upon completion
     * will always be zero, and its limit will indicate the end of the data previously unprocessed along with the network data just read.  Because the read buffer may have already
     * contained data when this method is called, the number of bytes in the read buffer upon completion may be greater than the number of bytes actually read.  If the number of
     * bytes actually read is needed, the {@link #getBytesRead()} method can be used (upon the read operation completing) to obtain that value.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> the total number of bytes read is at least equal to the minimum number of bytes
     * specified in {@code _minBytes}.  If {@code _minBytes} is 1, that means the read operation will complete after the first successful read operation - which could be part of
     * a packet, exactly one packet, or multiple packets with the last one possibly not completely read.  The code calling this method should always treat the data read as a
     * stream, and not a series of discrete packets.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer may be compacted by this method before any data is read.  While the read operation is
     *                    in  progress (i.e, before the {@code _onReadCompleteHandler} is called), the read buffer must not be manipulated other than by this instance - hands off
     *                    the read buffer!
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
     * <p>Initiates a synchronous (blocking) operation to read network data from the TCP connection represented by this instance.  The data is received from the network and
     * copied into the given read buffer.  This method will return when the operation is complete, whether it completed normally, with an error, or was canceled</p>
     * <p>If the given read buffer's position is zero and its limit is equal to its capacity (in other words, the buffer appears to be cleared), then the read buffer is used as is.
     * However, if the position is not zero, or the limit is less than the capacity, then the buffer is assumed to have valid (but not processed) data between its position and its
     * limit.  In that case, the buffer will be compacted (i.e., {@link ByteBuffer#compact()} will be called) before reading, so those unprocessed bytes will be the first bytes in
     * the buffer after the read.  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()} is called), so the buffer's position upon completion
     * will always be zero, and its limit will indicate the end of the data previously unprocessed along with the network data just read.  Because the read buffer may have already
     * contained data when this method is called, the number of bytes in the read buffer upon completion may be greater than the number of bytes actually read.  If the number of
     * bytes actually read is needed, the {@link #getBytesRead()} method can be used (upon the read operation completing) to obtain that value.</p>
     * <p>The number of bytes that will actually be read into the read buffer depends on a number of factors, not all of which are predictable or controllable.  In general, the
     * read operation will complete after reading the contents of a received TCP packet <i>and</i> the at least one byte has been read from the network.  That means the read
     * operation will complete after the first successful read operation - which could be part of a packet, exactly one packet, or multiple packets with the last one possibly not
     * completely read.  The code calling this method should always treat the data read as a stream, and not a series of discrete packets.</p>
     *
     * @param _readBuffer The read buffer to read network data into.  Note that this buffer may be compacted by this method before any data is read.  While the read operation is
     *                    in  progress (i.e, before the {@code _onReadCompleteHandler} is called), the read buffer must not be manipulated other than by this instance - hands off
     *                    the read buffer!
     * @return The outcome of this operation.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the network data read.  If not
     *         ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public Outcome<ByteBuffer> read( final ByteBuffer _readBuffer ) {
        return read( _readBuffer, 1 );
    }


    /**
     * Called by the {@link NetworkingEngine} whenever read interest is indicated in the selection key, and the engine detects that the connection is readable.  Note that this
     * method is <i>always</i> called in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
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
     * Implements the core of the read operation.  It is called once when the read is initiated, and again for each readable event detected by the engine.  Note that this method
     * may be called in the user's thread <i>or</i> in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
     */
    private void read() {

        try {
            // read what data we can...
            var bc = channel.read( readBuffer );  // this can throw an IOException...
            bytesRead.addAndGet( bc );
            LOGGER.finest( "Read " + bc + " bytes from " + this );

            // if the total bytes read is at least the minimum number of bytes, then we're done...
            if( bytesRead.get() >= minBytes ) {

                // set up the read buffer for use by our caller, accumulate the bytes read, and post our completion...
                readBuffer.flip();
                cumulativeBytesRead.addAndGet( 0xFFFFFFFFL & bytesRead.get() );
                postReadCompletion( forgeByteBuffer.ok( readBuffer ) );
            }

            // otherwise, we need to express read interest...
            else {
                key.interestOpsOr( READ_INTEREST );
                engine.wakeSelector();  // this guarantees that the key change will be effective immediately...
            }
        }
        catch( Exception _e ) {
            postReadCompletion( forgeByteBuffer.notOk( "Problem reading from channel: " + _e.getMessage(), _e ) );
        }
    }


    /**
     * If a read operation is still in progress, marks it as complete (i.e., {@code readInProgress} set to false) and posts the given {@link Outcome} to the
     * {@code onReadCompleteHandler}.  If no read operation was in progress, this method does nothing.
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
     * <p>Initiates an asynchronous (non-blocking) operation to write network data to the TCP connection represented by this instance, from the given write buffer.  The
     * {@code _onWriteCompleteHandler} is called when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.
     * Note that the {@code _onWriteCompleteHandler} will always be called in one of the threads from the associated {@link NetworkingEngine}'s {@link ScheduledExecutor}, never in
     * the thread that calls this method.</p>
     * <p>The data remaining in the given write buffer (i.e., the bytes between the position and the limit) will be written to the network.  When the write operation completes
     * normally, the write buffer will be cleared.  Otherwise, the buffer's position is set to the first byte that was <i>not</i> successfully written.  Note that this is not
     * a guarantee in any way that the bytes that were successfully written actually reached the destination - only that they were successfully written to the local TCP/IP
     * queue.</p>
     *
     * @param _writeBuffer The write buffer to write network data from.  While the write operation is in  progress (i.e, before the {@code _onWriteCompleteHandler} is called),
     *                     the write buffer must not be manipulated other than by this instance - hands off the write buffer!
     * @param _onWriteCompleteHandler This handler is called with the outcome of the write operation, when the write operation completes, whether normally, terminated by an error, or
     *                            canceled.  If the outcome is ok, then the operation completed normally.  If not ok, then there is an explanatory message and possibly the
     *                                exception that caused the problem.
     */
    public void write( final ByteBuffer _writeBuffer, final Consumer<Outcome<?>> _onWriteCompleteHandler ) {

        // set our write buffer to null, so in the postWriteCompletion we can tell if the mark has been set...
        writeBuffer = null;

        // if we didn't get a write complete handler, then we really don't have any choice but to throw an exception...
        if( isNull( _onWriteCompleteHandler ) ) throw new IllegalArgumentException( "_onWriteCompleteHandler is null" );

        // in the code below, the TCPPipeException exists only to make the code easier to read and understand...
        try {

            // sanity checks...
            if( isNull( _writeBuffer ) || (_writeBuffer.remaining() == 0) ) throw new TCPPipeException( "_writeBuffer is null or has no data to write" );

            // make sure we haven't already got a write operation in progress...
            if( writeInProgress.getAndSet( true ) ) throw new TCPPipeException( "Write operation already in progress" );

            // mark the current position, so that we may return the buffer to its initial state if the write does not complete normally...
            _writeBuffer.mark();

            // squirrel away our write state...
            writeBuffer            = _writeBuffer;
            onWriteCompleteHandler = _onWriteCompleteHandler;
            bytesWritten.set( 0 );

            // initiate the actual write process...
            write();
        }
        catch( TCPPipeException _e ) {
            postWriteCompletion( forge.notOk( _e.getMessage() ) );
        }
    }


    /**
     * <p>Initiates a synchronous (blocking) operation to write network data to the TCP connection represented by this instance, from the given write buffer.  This method returns
     * when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>The data remaining in the given write buffer (i.e., the bytes between the position and the limit) will be written to the network.  When the write operation completes
     * normally, the write buffer will be cleared.  Otherwise, the buffer's position is set to the first byte that was <i>not</i> successfully written.  Note that this is not
     * a guarantee in any way that the bytes that were successfully written actually reached the destination - only that they were successfully written to the local TCP/IP
     * queue.</p>
     *
     * @param _writeBuffer The write buffer to write network data from.  While the write operation is in  progress (i.e, before the {@code _onWriteCompleteHandler} is called),
     *                     the write buffer must not be manipulated other than by this instance - hands off the write buffer!
     */
    public Outcome<?> write( final ByteBuffer _writeBuffer ) {
        Waiter<Outcome<?>> waiter = new Waiter<>();
        write( _writeBuffer, waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * Called by the {@link NetworkingEngine} whenever write interest is indicated in the selection key, and the engine detects that the connection is writeable.  Note that this
     * method is <i>always</i> called in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
     */
    /* package-private */ void onWriteable() {

        // if there's no write in progress, we just log and ignore this call...
        if( !writeInProgress.get() ) {
            LOGGER.log( Level.WARNING, "TCPPipe::onWriteable() called when no write was in progress; ignoring" );
            return;
        }

        // handle the writeable event...
        write();
    }


    /**
     * Implements the core of the write operation.  It is called once when the write is initiated, and again for each writable event detected by the engine.  Note that this method
     * may be called in the user's thread <i>or</i> in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
     */
    private void write() {

        try {
            // write what data we can, and set the mark...
            bytesWritten.set( channel.write( writeBuffer ) );  // this can throw an IOException...
            writeBuffer.mark();
            LOGGER.finest( "Wrote " + bytesWritten.get() + " bytes to " + this );

            // if there are no bytes remaining, then we're done...
            if( !writeBuffer.hasRemaining() ) {

                // clear the write buffer for use by our caller, accumulate the bytes written, and post our completion...
                writeBuffer.clear();
                cumulativeBytesWritten.addAndGet( 0xFFFFFFFFL & bytesWritten.get() );
                postWriteCompletion( forge.ok() );
            }

            // otherwise, we need to express write interest...
            else {
                key.interestOpsOr( WRITE_INTEREST );
                engine.wakeSelector();  // this guarantees that the key change will be effective immediately...
            }
        }
        catch( Exception _e ) {
            postWriteCompletion( forge.notOk( "Problem writing to channel: " + _e.getMessage(), _e ) );
        }
    }


    /**
     * If a write operation is still in progress, marks it as complete (i.e., {@code writeInProgress} set to false) and posts the given {@link Outcome} to the
     * {@code onWriteCompleteHandler}.  If no write operation was in progress, this method does nothing.
     *
     * @param _outcome The {@link Outcome} to post.
     */
    private void postWriteCompletion( final Outcome<?> _outcome ) {

        // if there was no write in progress, just return...
        // this catches a race condition when a cancelWrite occurs just as a write is completing normally...
        // or when a write "completes" just after a cancelWrite...
        if( !writeInProgress.getAndSet( false ) ) return;

        // if we're sending a completion that's ok, clear the buffer, otherwise, reset position to the mark...
        if( writeBuffer != null ) {
            if(  _outcome.ok() )
                writeBuffer.clear();
            else
                writeBuffer.reset();
        }

        // otherwise, send the completion...
        engine.execute( () -> onWriteCompleteHandler.accept( _outcome ) );
    }


    /**
     * Cancels a read operation in progress.  If there is no read operation in progress, it has no effect.  Canceling a read operation does not affect future read operations.
     */
    public void cancelRead() {
        postReadCompletion( forgeByteBuffer.notOk( "Read canceled" ) );
    }


    /**
     * Cancels a write operation in progress.  If there is no write operation in progress, it has no effect.  Canceling a write operation does not affect future write operations.
     */
    public void cancelWrite() {
        postWriteCompletion( forge.notOk( "Write canceled" ) );
    }


    /**
     * Returns the number of bytes read by the most recent read operation, assuming that read has not been called again.
     *
     * @return The number of bytes read by the most recent read operation.
     */
    public int getBytesRead() {
        return bytesRead.get();
    }


    /**
     * Returns the number of bytes written by the most recent write operation, assuming that write has not been called again.
     *
     * @return The number of bytes written by the most recent write operation.
     */
    public int getBytesWritten() {
        return bytesWritten.get();
    }


    /**
     * Resets the counters for cumulative bytes read or written to zero.
     */
    public void clearAccumulators() {
        cumulativeBytesRead.set( 0 );
        cumulativeBytesWritten.set( 0 );
    }


    /**
     * Returns the total number of bytes read by this instance since its instantiation or since the most recent time {@link #clearAccumulators()} was called.
     *
     * @return The total number of bytes read by this instance.
     */
    public long getCumulativeBytesRead() {
        return cumulativeBytesRead.get();
    }


    /**
     * Returns the total number of bytes written by this instance since its instantiation or since the most recent time {@link #clearAccumulators()} was called.
     *
     * @return The total number of bytes written by this instance.
     */
    public long getCumulativeBytesWritten() {
        return cumulativeBytesWritten.get();
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
     * Close any active TCP connection associated with this pipe, and close the channel.  This instance is not usable once it has been closed.
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
     * An internal exception type that is used for code convenience and is never thrown outside this class or its subclasses).
     */
    protected static class TCPPipeException extends Exception {
        public TCPPipeException( final String message ) {
            super( message );
        }
    }


    @Override
    public boolean equals( final Object _o ) {

        if( this == _o ) return true;
        if( _o == null || getClass() != _o.getClass() ) return false;
        TCPPipe tcpPipe = (TCPPipe) _o;
        return channel.equals( tcpPipe.channel ) && engine.equals( tcpPipe.engine );
    }


    @Override
    public int hashCode() {
        return Objects.hash( channel, engine );
    }
}
