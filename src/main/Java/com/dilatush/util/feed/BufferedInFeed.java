package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * An {@link InFeed} that empties available bytes from an internal buffer.  Several methods allow adding bytes to the internal buffer to make them available to the feed.
 */
public class BufferedInFeed implements InFeed {

    private static final Logger                    LOGGER          = getLogger();

    private static final Outcome.Forge<ByteBuffer> forgeByteBuffer = new Outcome.Forge<>();

    private final ByteBuffer    buffer;
    private final AtomicBoolean reading;

    private OnReadComplete  onReadCompleteHandler;
    private ByteBuffer      readBuffer;
    private int             minBytes;
    private boolean         writeMode;  // true if the buffer is in write mode...
    private boolean         closed;


    /**
     * Create a new instance of {@link BufferedInFeed} with an internal buffer that can hold the given number of bytes, which must be in the range [1..65536].
     *
     * @param _size the number of bytes in the internal buffer.
     */
    public BufferedInFeed( final int _size ) {

        // sanity checks...
        if( (_size < 1) || (_size > 65536) ) throw new IllegalArgumentException( "_size is out of range [1..65536]: " + _size );

        buffer    = ByteBuffer.allocate( _size );
        reading   = new AtomicBoolean( false );
        writeMode = true;
        closed    = false;
    }


    /**
     * Attempt to append the given bytes to the internal buffer, to make them available for reading from this feed.
     *
     * @param _bytes the bytes to append.
     * @return {@code true} if the bytes were successfully appended, {@code false} otherwise.  If the bytes were not successfully added, the internal buffer is not modified.
     */
    public synchronized boolean append( final byte[] _bytes ) {

        // sanity check...
        if( (_bytes == null) || (_bytes.length == 0) ) throw new IllegalArgumentException( "_bytes is null or empty" );

        // put the buffer in write mode if it's not already there...
        if( !writeMode ) {
            buffer.compact();
            writeMode = true;
        }

        // if we don't have room for the given bytes, return false to indicate that we didn't write them...
        if( buffer.remaining() < _bytes.length ) return false;

        // we have room, so write the bytes we just got...
        buffer.put( _bytes );

        // if we have enough bytes to satisfy the minimum bytes of a read in progress, do it...
        if( reading.get() && (buffer.position() >= minBytes) )
            read();

        // then return true to indicate that we DID write the bytes...
        return true;
    }


    /**
     * Attempt to append the remaining bytes in the given {@link ByteBuffer} to the internal buffer, to make them available for reading from this feed.
     *
     * @param _buffer the buffer whose remaining bytes will be appended.
     * @return {@code true} if the bytes were successfully appended, {@code false} otherwise.  If the bytes were not successfully added, the internal buffer is not modified.
     */
    public synchronized boolean append( final ByteBuffer _buffer ) {

        // sanity check...
        if( isNull( _buffer ) || (_buffer.remaining() == 0) ) throw new IllegalArgumentException( "_buffer is null or empty" );

        var bytes = new byte[_buffer.remaining()];
        _buffer.get( bytes );
        return append( bytes );
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
        }
    }


    /**
     * The actual read operation.
     */
    private void read() {

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
}