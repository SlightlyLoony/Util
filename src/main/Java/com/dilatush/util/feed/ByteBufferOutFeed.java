package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static com.dilatush.util.General.isNull;


/**
 * A {@link ByteBufferOutFeed} that writes bytes to an internal {@link ByteBuffer}.
 */
public class ByteBufferOutFeed implements OutFeed {

    private static final Outcome.Forge<?> forge = new Outcome.Forge<>();

    private ByteBuffer buffer;

    public ByteBufferOutFeed( final int _capacity ) {

        // sanity checks...
        if( _capacity < 0 ) throw new IllegalArgumentException( "_capacity is less than zero: " + _capacity );

        buffer = ByteBuffer.allocate( _capacity );
    }


    /**
     * Return the internal buffer, flipped and ready to get bytes from.  Allocate a new buffer with the same capacity.
     *
     * @return the internal buffer, flipped and ready to get bytes from.
     */
    public ByteBuffer getBuffer() {
        var result = buffer.flip();
        buffer = ByteBuffer.allocate( result.capacity() );
        return result;
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to write data to this feed, from the given write buffer.  The
     * {@code _handler} is called when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>The data remaining in the given write buffer (i.e., the bytes between the position and the limit) will be written to the feed.  When the write operation completes
     * normally, the write buffer will be cleared.  Otherwise, the buffer's position is set to the first byte that was <i>not</i> successfully written.  Note that this is not
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
    public void write( final ByteBuffer _writeBuffer, final OnWriteComplete _handler ) {

        // sanity checks...
        if( isNull( _handler ) ) throw new IllegalArgumentException( "_handler is null" );
        if( isNull( _writeBuffer ) || !_writeBuffer.hasRemaining() )
            _handler.handle( forge.notOk( "_writeBuffer is null or empty", new BufferUnderflowException() ) );
        else if( _writeBuffer.remaining() > buffer.remaining() )
            _handler.handle( forge.notOk( "internal buffer has no room to accept bytes in _writeBuffer", new BufferOverflowException() ) );
        else {
            buffer.put( _writeBuffer );
            _handler.handle( forge.ok() );
        }
    }


    /**
     * Closes this output feed and releases any system resources associated with the feed.
     */
    @Override
    public void close() {
        buffer.clear();
        buffer.limit( 0 );
    }
}
