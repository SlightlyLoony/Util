package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dilatush.util.General.isNull;


/**
 * A {@link BufferedOutFeedSink} that adds bytes to an internal buffer.  Several methods allow emptying bytes from the buffer to make more room for bytes from the feed.
 */
public class BufferedOutFeedSink implements OutFeedSink {

    private static final Outcome.Forge<?> forge = new Outcome.Forge<>();

    private final ByteBuffer    buffer;
    private final AtomicBoolean writing;

    private OnWriteComplete onWriteCompleteHandler;
    private ByteBuffer      writeBuffer;
    private boolean         writeMode;  // true if the buffer is in write mode...
    private boolean         closed;


    /**
     * Create a new instance of {@link BufferedOutFeedSink} with an internal buffer that can hold the given number of bytes, which must be in the range [1..65536].
     *
     * @param _size the number of bytes in the internal buffer.
     */
    public BufferedOutFeedSink( final int _size ) {

        // sanity checks...
        if( (_size < 1) || (_size > 65536) ) throw new IllegalArgumentException( "_size is out of range [1..65536]: " + _size );

        buffer    = ByteBuffer.allocate( _size );
        writing   = new AtomicBoolean( false );
        writeMode = true;
        closed    = false;
    }


    /**
     * Drain any bytes read from the feed into a new byte array, which is returned.  If there are no bytes to drain, returns a {@code null}.
     *
     * @return a byte array containing the bytes drained, or a {@code null} if there were no bytes to drain.
     */
    public synchronized byte[] drainToByteArray() {

        // if we don't have any bytes to drain, leave with a null...
        if( !buffer.hasRemaining() ) return null;

        // put the buffer in read mode if it's not already...
        if( writeMode ) {
            buffer.flip();
            writeMode = false;
        }

        // get our bytes and leave...
        var b = new byte[buffer.remaining()];
        buffer.get( b );
        return b;
    }


    /**
     * Drain any bytes read from the feed into a new {@link ByteBuffer}, which is returned.  If there are no bytes to drain, returns a {@code null}.
     *
     * @return a {@link ByteBuffer} containing the bytes drained, or a {@code null} if there were no bytes to drain.
     */
    public synchronized ByteBuffer drainToByteBuffer() {
        var bytes = drainToByteArray();
        if( bytes == null ) return null;
        return ByteBuffer.allocate( bytes.length ).put( bytes ).flip();
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
        }
    }


    private void write() {

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
     * Closes this output feed and releases any system resources associated with the feed.
     */
    @Override
    public void close() {
        closed = true;
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
