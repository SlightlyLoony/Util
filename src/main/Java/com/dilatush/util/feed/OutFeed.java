package com.dilatush.util.feed;

import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;

import java.io.Closeable;
import java.nio.ByteBuffer;


/**
 * Implemented by output feeds that write data to a sink.
 */
public interface OutFeed extends Closeable {

    /**
     * <p>Initiates an asynchronous (non-blocking) operation to write data to this feed, from the given write buffer.  The
     * {@code _handler} is called when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>This method has no default; it <i>must</i> be provided by every {@link OutFeed} implementation.</p>
     * <p>The data remaining in the given write buffer (i.e., the bytes between the position and the limit) will be written to the feed.  Note that this is not
     * a guarantee in any way that the bytes that were successfully written actually reached their ultimate destination - only that they were successfully written to the feed.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _writeBuffer The write buffer to write to the feed from.  While the write operation is in  progress (i.e, before the {@code _handler} is called),
     *                     the write buffer must not be manipulated other than by this instance - hands off the write buffer!
     * @param _handler This handler is called with the outcome of the write operation, when the write operation completes, whether normally, terminated by an error, or canceled.
     *                 If the outcome is ok, then the operation completed normally.  If not ok, then there is an explanatory message and possibly the exception that caused the
     *                 problem.
     * @throws IllegalStateException if another write operation is in progress.
     * @throws IllegalArgumentException if no on write complete handler is specified.
     */
    void write( final ByteBuffer _writeBuffer, final OnWriteComplete _handler );


    /**
     * <p>Attempts a synchronous (blocking) operation to write data to this feed, from the given write buffer.  This method returns
     * when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>This method has a default implementation, which <i>may</i> be overridden, but <i>must</i> retain the default behavior.</p>
     * <p>The data remaining in the given write buffer (i.e., the bytes between the position and the limit) will be written to the feed.  Note that this is not
     * a guarantee in any way that the bytes that were successfully written actually reached their ultimate destination - only that they were successfully written to the feed.</p>
     *
     * @param _writeBuffer The write buffer to write to the feed from.  While the write operation is in  progress (i.e, before this method returns),
     *                     the write buffer must not be manipulated other than by this instance - hands off the write buffer!
     * @return The outcome of the attempt.  If ok, the data was successfully written.  If not ok, then there is an explanatory message and possibly the exception that caused
     * the problem.
     * @throws IllegalStateException if another write operation is already in progress.
     */
    default Outcome<?> write( final ByteBuffer _writeBuffer ) {
        Waiter<Outcome<?>> waiter = new Waiter<>();
        write( _writeBuffer, waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * Closes this output feed and releases any system resources associated with the feed.
     */
    void close();
}
