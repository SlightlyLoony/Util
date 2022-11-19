package com.dilatush.util.feed;

import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;

import java.io.Closeable;
import java.nio.ByteBuffer;


/**
 * Implemented by input feeds that read data from a source.
 */
public interface InFeed extends Closeable {


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read data from this feed.  The data read is
     * copied into the given read buffer.  The {@code _handler} is called when the read operation completes, whether that operation completed normally, was
     * terminated because of an error, or was canceled.</p>
     * <p>This method has no default; it <i>must</i> be implemented.</p>
     * <p>Data is read from the feed into the read buffer starting at the buffer's current position (which may be non-zero, in which case the data between the start of the buffer
     * is considered previously read, but unprocessed feed data).  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()}
     * is called), so the buffer's position upon completion will always be zero, and its limit will indicate the end of the data (including any previously read, but unprocessed
     * data).</p>
     * <p>On success, the number of bytes actually read will be between the given minimum number of bytes and the remaining capacity of the read buffer when this method was
     * called.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _readBuffer The read buffer to read feed data into.  While the read operation is in  progress (i.e, before the {@code _handler} is called), the read
     *                    buffer must not be manipulated other than by this instance - hands off the read buffer!
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  This must be at least 1, and no greater than the
     *                  remaining capacity of the read buffer when this method is called.
     * @param _handler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                 canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the feed data read (preceded
     *                 by any previously read, but not processed data).  If not ok, then there is an explanatory message and possibly the exception that caused the
     *                 problem.
     */
    void read( final ByteBuffer _readBuffer, final int _minBytes, final OnReadComplete _handler );


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read data from this feed.  The data read is
     * copied into the given read buffer.  The {@code _handler} is called when the read operation completes, whether that operation completed normally, was
     * terminated because of an error, or was canceled.</p>
     * <p>This method has a default implementation, which <i>may</i> be overridden, but <i>must</i> retain the default behavior.</p>
     * <p>Data is read from the feed into the read buffer starting at the buffer's current position (which may be non-zero, in which case the data between the start of the buffer
     * is considered previously read, but unprocessed feed data).  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()}
     * is called), so the buffer's position upon completion will always be zero, and its limit will indicate the end of the data (including any previously read, but unprocessed
     * data).</p>
     * <p>On success, the number of bytes actually read will be between 1 and the remaining capacity of the read buffer when this method was called.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _readBuffer The read buffer to read feed data into.  While the read operation is in  progress (i.e, before the {@code _handler} is called), the read
     *                    buffer must not be manipulated other than by this instance - hands off the read buffer!
     * @param _handler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                 canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the feed data read (preceded
     *                 by any previously read, but not processed data).  If not ok, then there is an explanatory message and possibly the exception that caused the
     *                 problem.
     */
    default void read( final ByteBuffer _readBuffer, final OnReadComplete _handler ) {
        read( _readBuffer, 1, _handler );
    }


    /**
     * <p>Initiates a synchronous (blocking) operation to read data from this feed.  The data read is
     * copied into the given read buffer.  This method will return when the operation is complete, whether it completed normally, with an error, or was canceled</p>
     * <p>This method has a default implementation, which <i>may</i> be overridden, but <i>must</i> retain the default behavior.</p>
     * <p>Data is read from the feed into the read buffer starting at the buffer's current position (which may be non-zero, in which case the data between the start of the buffer
     * is considered previously read, but unprocessed feed data).  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()}
     * is called), so the buffer's position upon completion will always be zero, and its limit will indicate the end of the data (including any previously read, but unprocessed
     * data).</p>
     * <p>On success, the number of bytes actually read will be between the given minimum number of bytes and the remaining capacity of the read buffer when this method was
     * called.</p>
     *
     * @param _readBuffer The read buffer to read feed data into.  While the read operation is in  progress (i.e, before the {@code _handler} is called), the read
     *                    buffer must not be manipulated other than by this instance - hands off the read buffer!
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  This must be at least 1, and no greater than the
     *                  remaining capacity of the read buffer when this method is called.
     * @return The outcome of this operation.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the feed data read (preceded
     *         by any previously read, but not processed data).  If not ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    default Outcome<ByteBuffer> read( final ByteBuffer _readBuffer, final int _minBytes ) {
        Waiter<Outcome<ByteBuffer>> waiter = new Waiter<>();
        read( _readBuffer, _minBytes, waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * <p>Initiates a synchronous (blocking) operation to read data from this feed.  The data read is
     * copied into the given read buffer.  This method will return when the operation is complete, whether it completed normally, with an error, or was canceled</p>
     * <p>This method has a default implementation, which <i>may</i> be overridden, but <i>must</i> retain the default behavior.</p>
     * <p>Data is read from the feed into the read buffer starting at the buffer's current position (which may be non-zero, in which case the data between the start of the buffer
     * is considered previously read, but unprocessed feed data).  When the read operation is complete, the buffer is flipped (i.e., {@link ByteBuffer#flip()}
     * is called), so the buffer's position upon completion will always be zero, and its limit will indicate the end of the data (including any previously read, but unprocessed
     * data).</p>
     * <p>On success, the number of bytes actually read will be between 1 and the remaining capacity of the read buffer when this method was called.</p>
     *
     * @param _readBuffer The read buffer to read feed data into.  While the read operation is in  progress (i.e, before the {@code _handler} is called), the read
     *                    buffer must not be manipulated other than by this instance - hands off the read buffer!
     * @return The outcome of this operation.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the feed data read (preceded
     *         by any previously read, but not processed data).  If not ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    default Outcome<ByteBuffer> read( final ByteBuffer _readBuffer ) {
        return read( _readBuffer, 1 );
    }


    /**
     * Closes this input feed and releases any system resources associated with the feed.
     */
    void close();
}
