package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;
import static java.util.logging.Level.FINEST;

/**
 * A piped feed (the combination of an {@link OutFeed} and an {@link InFeed}) that makes bytes written to the {@link OutFeed} available to be read from the {@link InFeed}.  The
 * bytes written are stored in an internal buffer until they are read.  The size of the internal buffer is set on instantiation.
 */
public class BufferedPipedFeed implements Feed {

    private static final Logger LOGGER          = getLogger();

    private static final Outcome.Forge<ByteBuffer> forgeByteBuffer = new Outcome.Forge<>();
    private static final Outcome.Forge<?>          forge           = new Outcome.Forge<>();

    private final ByteBuffer    buffer;
    private final AtomicBoolean reading;
    private final AtomicBoolean writing;

    private OnReadComplete  onReadCompleteHandler;
    private ByteBuffer      readBuffer;
    private OnWriteComplete onWriteCompleteHandler;
    private ByteBuffer      writeBuffer;
    private int             minBytes;
    private boolean         writeMode;  // true if the buffer is in write mode...
    private boolean         closed;


    /**
     * Create a new instance of {@link BufferedInFeed} with an internal buffer that can hold the given number of bytes, which must be in the range [1..65536].
     *
     * @param _size the number of bytes in the internal buffer.
     */
    public BufferedPipedFeed( final int _size ) {

        // sanity checks...
        if( (_size < 1) || (_size > 65536) ) throw new IllegalArgumentException( "_size is out of range [1..65536]: " + _size );

        buffer    = ByteBuffer.allocate( _size );
        reading   = new AtomicBoolean( false );
        writing   = new AtomicBoolean( false );
        writeMode = true;
        closed    = false;
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read between the given {@code _minBytes} and {@code _maxBytes} bytes from this feed.  The data is read into a new
     * {@link ByteBuffer}, which is the info in the {@link Outcome} if the outcome was ok.  The {@code _handler} is called when the read operation completes, whether that
     * operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  The value must be in the range [1..{@code _maxBytes}].
     * @param _maxBytes The maximum number of bytes that may be read in this read operation.  The value must be in the range [{@code _minBytes}..65536].
     * @param _handler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                 canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this feed.  If not ok,
     *                 then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalArgumentException if the {@code _handler} is {@code null}.
     * @throws IllegalStateException if a read operation is already in progress.
     */
    @Override
    @SuppressWarnings( "DuplicatedCode" )
    public synchronized void read( final int _minBytes, final int _maxBytes, final OnReadComplete _handler ) {

        // if we didn't get a read complete handler, then we really don't have any choice but to throw an exception...
        if( isNull( _handler ) ) throw new IllegalArgumentException( "_handler is null" );

        // if we already have a read operation in progress, throw an exception...
        if( reading.getAndSet( true ) )
            throw new IllegalStateException( "Read operation already in progress" );

        // squirrel away the handler, as we may need it asynchronously...
        onReadCompleteHandler = _handler;

        // sanity checks...
        if( closed )
            postReadCompletion( forgeByteBuffer.notOk( "BufferedInFeed is closed" ) );
        else if( _minBytes < 1 )
            postReadCompletion( forgeByteBuffer.notOk( "_minBytes is " + _minBytes + ", but must be >= 1", new IllegalArgumentException() ) );
        else if( _minBytes > _maxBytes )
            postReadCompletion( forgeByteBuffer.notOk(
                    "_minBytes is " + _minBytes + " and _maxBytes is " + _maxBytes + ", but _minBytes must be <= _maxBytes", new IllegalArgumentException() ) );
        else if( _maxBytes > InFeed.MAX_READ_BYTES )
            postReadCompletion( forgeByteBuffer.notOk( "_maxBytes is " + _maxBytes + ", but must be <= 65,536", new IllegalArgumentException() ) );

            // if things look sane, then it's time to read some bytes...
        else {

            // set up for this read...
            readBuffer = ByteBuffer.allocate( _maxBytes );
            minBytes = _minBytes;

            // initiate the actual read process...
            LOGGER.finest( "Initiating the actual read process" );
            read();

            // handle any pending writes...
            write();
        }
    }


    /**
     * The actual read operation.
     */
    private void read() {

        LOGGER.log( FINEST, "read(): " + buffer );

        // if there's no read in progress, just leave...
        if( !reading.get() ) return;

        // put the buffer in read mode if it's not already...
        if( writeMode ) {
            buffer.flip();
            writeMode = false;
        }

        // complete the read...
        var n = Math.min( readBuffer.remaining(), buffer.remaining() );  // calculate how many bytes we're going to pull out of our internal buffer and send to the feed...
        readBuffer.put( buffer.slice( buffer.position(), n ) );               // copy a slice of our internal buffer into the read buffer...
        buffer.position( buffer.position() + n );                             // update the position on our internal buffer, as the .slice() above prevents that...

        // if we haven't read the minimum number of bytes yet, just leave...
        if( readBuffer.position() < minBytes ) return;

        // otherwise, get the read buffer ready and post the completion...
        readBuffer.flip();
        postReadCompletion( forgeByteBuffer.ok( readBuffer ) );
    }


    /**
     * If a read operation is still in progress, marks it as complete (i.e., {@code readInProgress} set to false) and posts the given {@link Outcome} to the
     * {@code onReadCompleteHandler}.  If no read operation was in progress, this method does nothing.
     *
     * @param _outcome The {@link Outcome} to post.
     */
    private void postReadCompletion( final Outcome<ByteBuffer> _outcome ) {

        // if there was no read in progress, just return...
        if( !reading.getAndSet( false ) ) return;

        // otherwise, send the completion...
        onReadCompleteHandler.handle( _outcome );
    }


    /**
     * Closes this input feed and releases any system resources associated with the feed.
     */
    @Override
    public void close() {
        closed = true;
    }


    /**
     * Return {@code true} if a read operation is already in progress.
     *
     * @return {@code true} if a read operation is already in progress.
     */
    @Override
    public boolean isReading() {
        return reading.get();
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to write data to this feed, from the given write buffer.  The
     * {@code _handler} is called when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>The data remaining in the given write buffer (i.e., the bytes between the position and the limit) will be written to the feed.  Note that this is not
     * a guarantee in any way that the bytes that were successfully written actually reached their ultimate destination - only that they were successfully written to the feed.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _writeBuffer The write buffer to write to the feed from.  While the write operation is in  progress (i.e, before the {@code _handler} is called),
     *                     the write buffer must not be manipulated other than by this instance - hands off the write buffer!
     * @param _handler     This handler is called with the outcome of the write operation, when the write operation completes, whether normally, terminated by an error, or canceled.
     *                     If the outcome is ok, then the operation completed normally.  If not ok, then there is an explanatory message and possibly the exception that caused the
     *                     problem.
     * @throws IllegalStateException if another write operation is in progress.
     * @throws IllegalArgumentException if no on write complete handler is specified.
     */
    @Override
    public synchronized void write( final ByteBuffer _writeBuffer, final OnWriteComplete _handler ) {

        // if we didn't get a read complete handler, then we really don't have any choice but to throw an exception...
        if( isNull( _handler ) ) throw new IllegalArgumentException( "_handler is null" );

        // if we already have a write operation in progress, throw an exception...
        if( writing.getAndSet( true ) )
            throw new IllegalStateException( "Write operation already in progress" );

        // squirrel away the handler, as we may need it asynchronously...
        onWriteCompleteHandler = _handler;

        // sanity checks...
        if( closed )
            postWriteCompletion( forge.notOk( "BufferOutFeedSink is closed" ) );
        else if( isNull( _writeBuffer ) || !_writeBuffer.hasRemaining() )
            postWriteCompletion( forge.notOk( "_writeBuffer is null or empty", new IllegalArgumentException() ) );
        else {
            writeBuffer = _writeBuffer;
            write();

            // handle any pending reads...
            read();
        }
    }


    private void write() {

        LOGGER.log( FINEST, "write(): " + buffer );

        // put the buffer in write mode if it's not already there...
        if( !writeMode ) {
            buffer.compact();
            writeMode = true;
        }

        // write all the bytes we can...
        var n = Math.min( writeBuffer.remaining(), buffer.remaining() );  // calculate how many bytes to pull out of the write buffer and send to our internal buffer...
        if( n > 0 ) {
            buffer.put( writeBuffer.slice( writeBuffer.position(), n ) );      // copy a slice of the write buffer into our internal buffer...
            writeBuffer.position( writeBuffer.position() + n );                // update the position on the write buffer, as the .slice() above prevents that...
        }

        // if we still have bytes to write, just leave...
        if( writeBuffer.hasRemaining() ) return;

        // otherwise, all the bytes have been written, and it's time to complete...
        postWriteCompletion( forge.ok() );
    }


    /**
     * If a write operation is still in progress, marks it as complete (i.e., {@code writing} set to false) and posts the given {@link Outcome} to the
     * {@code onWriteCompleteHandler}.  If no write operation was in progress, this method does nothing.
     *
     * @param _outcome The {@link Outcome} to post.
     */
    private void postWriteCompletion( final Outcome<?> _outcome ) {

        // if there was no write in progress, just return...
        if( !writing.getAndSet( false ) ) return;

        // otherwise, send the completion...
        onWriteCompleteHandler.handle( _outcome );
    }


    /**
     * Return {@code true} if a write operation is already in progress.
     *
     * @return {@code true} if a write operation is already in progress.
     */
    @Override
    public boolean isWriting() {
        return writing.get();
    }
}
