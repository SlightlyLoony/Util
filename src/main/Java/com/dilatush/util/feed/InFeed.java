package com.dilatush.util.feed;

import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;

import java.io.Closeable;
import java.nio.ByteBuffer;


/**
 * Implemented by input feeds that read data from a source.
 */
public interface InFeed extends Closeable {

    /** The maximum number of bytes in a single read operation. */
    int MAX_READ_BYTES = 65536;

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
     * @param _handler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                 canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this feed.  If not ok,
     *                 then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalArgumentException if the {@code _handler} is {@code null}.
     * @throws IllegalStateException if a read operation is already in progress.
     */
    void read( final int _minBytes, final int _maxBytes, final OnReadComplete _handler );


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read between 1 and the given {@code _maxBytes} bytes from this feed.  The data is read into a new
     * {@link ByteBuffer}, which is the info in the {@link Outcome} if the outcome was ok.  The {@code _handler} is called when the read operation completes, whether that
     * operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>This method has a default implementation, which <i>may</i> be overridden, but <i>must</i> retain the default behavior.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _maxBytes The maximum number of bytes that may be read in this read operation.  The value must be in the range [1..65536].
     * @param _handler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                 canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this feed.  If not ok,
     *                 then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalArgumentException if the {@code _handler} is {@code null}.
     * @throws IllegalStateException if a read operation is already in progress.
     */
    default void read( final int _maxBytes, final OnReadComplete _handler ) {
        read( 1, _maxBytes, _handler );
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read between the given {@code _minBytes} and {@code _maxBytes} bytes from this feed.  The data is read into a new
     * {@link ByteBuffer}, which is the info in the {@link Outcome} if the outcome was ok.  This method will return when the operation is complete, whether it completed normally,
     * with an error, or was canceled</p>
     * <p>This method has a default implementation, which <i>may</i> be overridden, but <i>must</i> retain the default behavior.</p>
     *
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  The value must be in the range [1..{@code _maxBytes}].
     * @param _maxBytes The maximum number of bytes that may be read in this read operation.  The value must be in the range [{@code _minBytes}..65536].
     * @return The outcome of this operation.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this
     * feed.  If not ok, then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalStateException if a read operation is already in progress.
     */
    default Outcome<ByteBuffer> read( final int _minBytes, final int _maxBytes ) {
        Waiter<Outcome<ByteBuffer>> waiter = new Waiter<>();
        read( _minBytes, _maxBytes, waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read between 1 and the given {@code _maxBytes} bytes from this feed.  The data is read into a new
     * {@link ByteBuffer}, which is the info in the {@link Outcome} if the outcome was ok.  This method will return when the operation is complete, whether it completed normally,
     * with an error, or was canceled</p>
     * <p>This method has a default implementation, which <i>may</i> be overridden, but <i>must</i> retain the default behavior.</p>
     *
     * @param _maxBytes The maximum number of bytes that may be read in this read operation.  The value must be in the range [{@code _minBytes}..65536].
     * @return The outcome of this operation.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this
     * feed.  If not ok, then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalStateException if a read operation is already in progress.
     */
    default Outcome<ByteBuffer> read( final int _maxBytes ) {
        return read( 1, _maxBytes );
    }


    /**
     * Return {@code true} if a read operation is already in progress.  The default implementation always returns {@code false}.
     *
     * @return {@code true} if a read operation is already in progress.
     */
    default boolean isReading() {
        return false;
    }


    /**
     * Closes this input feed and releases any system resources associated with the feed.
     */
    void close();
}
