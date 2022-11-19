package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static com.dilatush.util.General.isNull;

/**
 * A {@link ByteBufferInFeed} that contains an internal {@link ByteBuffer} whose remaining bytes that may be read from the feed.
 */
public class ByteBufferInFeed implements InFeed {

    private static final Outcome.Forge<ByteBuffer> forgeByteBuffer = new Outcome.Forge<>();

    private final ByteBuffer buffer;


    /**
     * Create a new instance of {@link ByteBufferInFeed} that can supply the remaining bytes in the given {@link ByteBuffer}.
     *
     * @param _buffer The {@link ByteBuffer} whose remaining bytes may be read by this feed.
     */
    public ByteBufferInFeed( final ByteBuffer _buffer ) {

        // sanity checks...
        if( isNull( _buffer ) ) throw new IllegalArgumentException( "_buffer is null" );
        if( !_buffer.hasRemaining() ) throw new IllegalArgumentException( "_buffer is empty" );

        buffer = _buffer;
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read data from this feed.  The data read is
     * copied into the given read buffer.  The {@code _handler} is called when the read operation completes, whether that operation completed normally, was
     * terminated because of an error, or was canceled.</p>
     * <p>Data is read from the feed into the read buffer starting at the buffer's current position (which may be non-zero, in which case the data between the start of the buffer
     * is considered previously read, but unprocessed feed data).  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()}
     * is called), so the buffer's position upon completion will always be zero, and its limit will indicate the end of the data (including any previously read, but unprocessed
     * data).</p>
     * <p>On success, the number of bytes actually read will be between the given minimum number of bytes and the remaining capacity of the read buffer when this method was
     * called.</p>
     * <p>The handler will be called in the same thread that called this method.</p>
     *
     * @param _readBuffer The read buffer to read feed data into.  While the read operation is in  progress (i.e, before the {@code _handler} is called), the read
     *                    buffer must not be manipulated other than by this instance - hands off the read buffer!
     * @param _minBytes   The minimum number of bytes that must be read for this read operation to be considered complete.  This must be at least 1, and no greater than the
     *                    remaining capacity of the read buffer when this method is called.
     * @param _handler    This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                    canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the feed data read (preceded
     *                    by any previously read, but not processed data).  If not ok, then there is an explanatory message and possibly the exception that caused the
     *                    problem.
     */
    @Override
    public void read( final ByteBuffer _readBuffer, final int _minBytes, final OnReadComplete _handler ) {

        if( isNull( _readBuffer, _handler ) ) throw new IllegalArgumentException( "_readBuffer or _handler is null" );

        if( buffer.remaining() == 0 )
            _handler.handle( forgeByteBuffer.notOk( "No more data", new BufferUnderflowException() ) );
        else if( _minBytes < 1 )
            _handler.handle( forgeByteBuffer.notOk( "_minBytes must be > 0", new IllegalArgumentException() ) );
        else if( _minBytes > _readBuffer.remaining() )
            _handler.handle( forgeByteBuffer.notOk( "_minBytes must be <= bytes remaining in the read buffer", new BufferUnderflowException() ) );
        else if( _minBytes > buffer.remaining() )
            _handler.handle( forgeByteBuffer.notOk( "_minBytes must be <= bytes remaining in the source buffer", new BufferUnderflowException() ) );
        else {
            byte[] b = new byte[ Math.min( buffer.remaining(), _readBuffer.limit() - _readBuffer.position() ) ];
            buffer.get( b );
            _readBuffer.put( b );
            _readBuffer.flip();
            _handler.handle( forgeByteBuffer.ok( _readBuffer ) );
        }
    }


    /**
     * Closes this input feed and releases any system resources associated with the feed.
     */
    @Override
    public void close() {
        buffer.position( 0 );
        buffer.limit( 0 );
    }
}