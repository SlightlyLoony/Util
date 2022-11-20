package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;

public class PipedFeed implements InFeed, OutFeed {



    /**
     * <p>Initiates an asynchronous (non-blocking) operation to write data to this feed, from the given write buffer.  The
     * {@code _handler} is called when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>This method has no default; it <i>must</i> be implemented.</p>
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
     * @throws IllegalStateException    if another write operation is in progress.
     * @throws IllegalArgumentException if no on write complete handler is specified.
     */
    @Override
    public void write( final ByteBuffer _writeBuffer, final OnWriteComplete _handler ) {

    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read between the given {@code _minBytes} and {@code _maxBytes} bytes from this feed.  The data is read into a new
     * {@link ByteBuffer}, which is the info in the {@link Outcome} if the outcome was ok.  The {@code _handler} is called when the read operation completes, whether that
     * operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>This method has no default implementation; it <i>must</i> be provided by every {@link InFeed} implementation.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  The value must be in the range [1..{@code _maxBytes}].
     * @param _maxBytes The maximum number of bytes that may be read in this read operation.  The value must be in the range [{@code _minBytes}..65536].
     * @param _handler  This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                  canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this feed.  If not ok,
     *                  then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalArgumentException if the {@code _handler} is {@code null}.
     * @throws IllegalStateException    if a read operation is already in progress.
     */
    @Override
    public void read( final int _minBytes, final int _maxBytes, final OnReadComplete _handler ) {

    }


    /**
     * Closes this piped feed and releases any system resources associated with the feed.
     */
    @Override
    public void close() {

    }
}
