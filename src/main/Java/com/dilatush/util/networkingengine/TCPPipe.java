package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.feed.*;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Abstract base class for all TCP pipes, each of which provides communications on a single TCP connection; subclasses implement specific kinds of TCP pipes.
 */
@SuppressWarnings( "unused" )
public abstract class TCPPipe implements FullFeedSourceSink {

    private static final Logger                      LOGGER                                      = getLogger();

    protected static final Outcome.Forge<?>            forge              = new Outcome.Forge<>();
    protected static final Outcome.Forge<ByteBuffer>   forgeByteBuffer    = new Outcome.Forge<>();

    // constants related to SelectionKey interest operations...
    protected static final int NO_INTEREST    = 0;
    protected static final int READ_INTEREST  = SelectionKey.OP_READ;
    protected static final int WRITE_INTEREST = SelectionKey.OP_WRITE;

    protected final SocketChannel    channel;               // the channel for the TCP connection this instance abstracts...
    protected final NetworkingEngine engine;                // the networking engine whose Selector our channel's SelectionKey is registered with...
    protected final SelectionKey     key;                   // the SelectionKey for our channel...

    protected IPAddress            remoteIP;                // the IP address of the remote side of this connection...
    protected int                  remotePort;              // the TCP port of the remote side of this connection...
    private final AtomicBoolean    reading;                 // true when a read operation is in progress...
    private final AtomicBoolean    writing;                 // true when a write operation is in progress...

    private ByteBuffer      readBuffer;
    private ByteBuffer      writeBuffer;
    private int             minBytes;
    private OnReadComplete  onReadCompleteHandler;
    private OnWriteComplete onWriteCompleteHandler;


    /**
     * Creates a new instance of this abstract base class. Socket options SO_REUSEADDR and SO_KEEPALIVE are both set to {@code true}, and the channel's selection key (with no
     * interests) is registered with the {@link NetworkingEngine}'s selector.
     *
     * @param _engine The {@link NetworkingEngine} whose selector our channel's selection key should be registered with.
     * @param _channel The {@link SocketChannel} accepted by {@link TCPListener}.
     * @throws IOException if any I/O error occurs
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws java.lang.IllegalStateException if the channel is not already connected
     */
    protected TCPPipe( final NetworkingEngine _engine, final SocketChannel _channel ) throws IOException {

        // sanity checks...
        if( isNull( _engine, _channel ))
            throw new IllegalArgumentException( "_engine or _channel is null" );

        // some initialization...
        engine  = _engine;
        channel = _channel;

        // configure our channel...
        channel.configureBlocking( false );
        channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );  // reuse connections in TIME_WAIT (e.g., after close and reconnect)...
        channel.setOption( StandardSocketOptions.SO_KEEPALIVE, true );  // enable keep-alive packets on this connection (mainly to detect broken connections)...

        // save the remote IP and port, if our channel is connected...
        if( channel.isConnected() ) {
            remoteIP = IPAddress.fromInetAddress( ((InetSocketAddress) channel.getRemoteAddress()).getAddress() );
            remotePort = ((InetSocketAddress) channel.getRemoteAddress()).getPort();
        }

        // attempt to register a key for this instance with our engine, with (for now) no interest in any notifications...
        key = engine.register( channel, NO_INTEREST, this );

        // create and initialize our operation in progress and cancel flags...
        reading = new AtomicBoolean( false );
        writing = new AtomicBoolean( false );
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read between the given {@code _minBytes} and {@code _maxBytes} bytes from this feed, which gets its data from a
     * TCP network connection.  The data is read into a new {@link ByteBuffer}, which is the info in the {@link Outcome} if the outcome was ok.  The {@code _handler} is called
     * when the read operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>The {@code _handler} will <i>always</i> be called from a different thread than the call to this method was made in.</p>
     *
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  The value must be in the range [1..{@code _maxBytes}].
     * @param _maxBytes The maximum number of bytes that may be read in this read operation.  The value must be in the range [{@code _minBytes}..65536].
     * @param _handler This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                 canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this feed.  If not ok,
     *                 then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalArgumentException if the {@code _handler} is {@code null}.
     * @throws IllegalStateException if a read operation is already in progress.
     */
    public void read( final int _minBytes, final int _maxBytes, final OnReadComplete _handler ) {

        // if we didn't get a read complete handler, then we really don't have any choice but to throw an exception...
        if( isNull( _handler ) ) throw new IllegalArgumentException( "_handler is null" );

        // if we already have a read operation in progress, throw an exception...
        if( reading.getAndSet( true ) )
            throw new IllegalStateException( "Read operation already in progress" );

        // squirrel away the handler, as we may need it asynchronously...
        onReadCompleteHandler = _handler;

        // sanity checks...
        if( _minBytes < 1 )
            postReadCompletion( forgeByteBuffer.notOk( "_minBytes is " + _minBytes + ", but must be >= 1" ) );
        else if( _minBytes > _maxBytes )
            postReadCompletion( forgeByteBuffer.notOk( "_minBytes is " + _minBytes + " and _maxBytes is " + _maxBytes + ", but _minBytes must be <= _maxBytes" ) );
        else if( _maxBytes > InFeed.MAX_READ_BYTES )
            postReadCompletion( forgeByteBuffer.notOk( "_maxBytes is " + _maxBytes + ", but must be <= 65,536" ) );

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
     * Called by the {@link NetworkingEngine} whenever read interest is indicated in the selection key, and the engine detects that the connection is readable.  Note that this
     * method is <i>always</i> called in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
     */
    /* package-private */ void onReadable() {

        // if there's no read in progress, we just log and ignore this call...
        if( !reading.get() ) {
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

            // if there's no read in progress, just leave...
            if( !reading.get() ) return;

            // read what data we can...
            LOGGER.finest( "Reading TCP bytes" );
            var bc = channel.read( readBuffer );  // this can throw an IOException...
            LOGGER.finest( "Read " + bc + " bytes from " + this );

            // if the total bytes read is at least the minimum number of bytes, then we're done...
            if( readBuffer.position() >= minBytes ) {

                LOGGER.finest( "Reading buffered TCP bytes" );

                // set up the read buffer for use by our caller and post our completion...
                readBuffer.flip();
                postReadCompletion( forgeByteBuffer.ok( readBuffer ) );
            }

            // otherwise, we need to express read interest...
            else {
                LOGGER.finest( "Expressing read interest for TCP bytes" );
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
        if( !reading.getAndSet( false ) ) return;

        // otherwise, send the completion...
        engine.execute( () -> onReadCompleteHandler.handle( _outcome ) );
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to write network data to the TCP connection represented by this instance, from the given write buffer.  The
     * {@code _onWriteCompleteHandler} is called when the write operation completes, whether that operation completed normally, was terminated because of an error, or was canceled.
     * Note that the {@code _onWriteCompleteHandler} will always be called in one of the threads from the associated {@link NetworkingEngine}'s {@link ScheduledExecutor}, never in
     * the thread that calls this method.</p>
     * <p>The data remaining in the given write buffer (i.e., the bytes between the position and the limit) will be written to the network.  Note that this is not
     * a guarantee in any way that the bytes that were successfully written actually reached the destination - only that they were successfully written to the local TCP/IP
     * queue.</p>
     *
     * @param _writeBuffer The write buffer to write network data from.  While the write operation is in  progress (i.e, before the {@code _onWriteCompleteHandler} is called),
     *                     the write buffer must not be manipulated other than by this instance - hands off the write buffer!
     * @param _onWriteCompleteHandler This handler is called with the outcome of the write operation, when the write operation completes, whether normally, terminated by an error,
     *                                or canceled.  If the outcome is ok, then the operation completed normally.  If not ok, then there is an explanatory message and possibly the
     *                                exception that caused the problem.
     * @throws IllegalStateException if another write operation is already in progress.
     * @throws IllegalArgumentException if no on write complete handler is specified.
     */
    public void write( final ByteBuffer _writeBuffer, final OnWriteComplete _onWriteCompleteHandler ) {

        // set our write buffer to null, so in the postWriteCompletion we can tell if the mark has been set...
        writeBuffer = null;

        // if we didn't get a write complete handler, then we really don't have any choice but to throw an exception...
        if( isNull( _onWriteCompleteHandler ) ) throw new IllegalArgumentException( "_onWriteCompleteHandler is null" );

        // make sure we haven't already got a write operation in progress...
        if( writing.getAndSet( true ) ) throw new IllegalStateException( "Write operation already in progress" );

        // in the code below, the TCPPipeException exists only to make the code easier to read and understand...
        try {

            // sanity checks...
            if( isNull( _writeBuffer ) || (_writeBuffer.remaining() == 0) ) throw new TCPPipeException( "_writeBuffer is null or has no data to write" );

            // mark the current position, so that we may return the buffer to its initial state if the write does not complete normally...
            _writeBuffer.mark();

            // squirrel away our write state...
            writeBuffer            = _writeBuffer;
            onWriteCompleteHandler = _onWriteCompleteHandler;

            // initiate the actual write process...
            write();
        }
        catch( TCPPipeException _e ) {
            postWriteCompletion( forge.notOk( _e.getMessage() ) );
        }
    }


    /**
     * Called by the {@link NetworkingEngine} whenever write interest is indicated in the selection key, and the engine detects that the connection is writeable.  Note that this
     * method is <i>always</i> called in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
     */
    /* package-private */ void onWriteable() {

        // if there's no write in progress, we just log and ignore this call...
        if( !writing.get() ) {
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

            // write what data we can...
            var bytesWritten = channel.write( writeBuffer );  // this can throw an IOException...
            LOGGER.finest( "Wrote " + bytesWritten + " bytes to " + this );

            // if there are no bytes remaining, then we're done...
            if( !writeBuffer.hasRemaining() ) {

                // post our completion...
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
        if( !writing.getAndSet( false ) ) return;

        // if we're sending a completion that's ok, clear the buffer, otherwise, reset position to the mark...
        if( writeBuffer != null ) {
            if(  _outcome.ok() )
                writeBuffer.clear();
            else
                writeBuffer.reset();
        }

        // otherwise, send the completion...
        engine.execute( () -> onWriteCompleteHandler.handle( _outcome ) );
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


    public IPAddress getRemoteIP() {
        return remoteIP;
    }


    public int getRemotePort() {
        return remotePort;
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
     * Return {@code true} if a write operation is already in progress.
     *
     * @return {@code true} if a write operation is already in progress.
     */
    @Override
    public boolean isWriting() {

        return writing.get();
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
