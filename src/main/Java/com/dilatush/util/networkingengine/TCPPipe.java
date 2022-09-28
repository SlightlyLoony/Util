package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 *
 */
@SuppressWarnings( "unused" )
public abstract class TCPPipe {

    private static final Logger                      LOGGER                                      = getLogger();

    protected static final Outcome.Forge<?>            forge              = new Outcome.Forge<>();
    protected static final Outcome.Forge<ByteBuffer>   forgeByteBuffer    = new Outcome.Forge<>();

    protected static final int                   NO_INTEREST    = 0;
    protected static final int                   READ_INTEREST  = SelectionKey.OP_READ;
    protected static final int                   WRITE_INTEREST = SelectionKey.OP_WRITE;

    protected final SocketChannel                 channel;
    protected final NetworkingEngine              engine;
    protected final Consumer<Outcome<ByteBuffer>> onReadReadyHandler;
    protected final Runnable                      onWriteReadyHandler;
    protected final BiConsumer<String,Exception>  onErrorHandler;
    protected final SelectionKey                  key;
    protected final AtomicBoolean                 readInProgress;
    protected final AtomicBoolean                 writeInProgress;

    private ByteBuffer readBuffer;
    private int minBytes;


    /**
     * Creates a new instance of this superclass, configured for inbound connections that have already been completed by the {@link TCPListener}.
     *
     * @param _engine
     * @param _config
     * @throws IOException
     */
    protected TCPPipe( final NetworkingEngine _engine, final TCPPipeInboundConfig _config ) throws IOException {

        // sanity checks...
        if( isNull( _engine, _config, _config.channel(), _config.onErrorHandler(), _config.onReadReadyHandler(), _config.onWriteReadyHandler() ))
            throw new IllegalArgumentException( "_config, _engine, _config.channel, _config.onErrorHandler, _config.onReadReadyHandler, or _config.onWriteReadyHandler is null" );
        //noinspection resource
        if( !_config.channel().isConnected() )
            throw new IllegalStateException( "_config.channel is not connected" );

        // some initialization...
        engine                    = _engine;
        channel                   = _config.channel();
        onReadReadyHandler        = _config.onReadReadyHandler();
        onWriteReadyHandler       = _config.onWriteReadyHandler();
        onErrorHandler            = _config.onErrorHandler();

        // configure our channel...
        channel.configureBlocking( false );
        channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );  // reuse connections in TIME_WAIT (e.g., after close and reconnect)...
        channel.setOption( StandardSocketOptions.SO_KEEPALIVE, true );  // enable keep-alive packets on this connection (mainly to detect broken connections)...

        // attempt to register a key for this instance with our engine, with (for now) no interest in any notifications...
        key                       = engine.register( channel, NO_INTEREST, this );

        // create and initialize our operation in progress flags...
        readInProgress  = new AtomicBoolean( false );
        writeInProgress = new AtomicBoolean( false );
    }


    /**
     * Initiates a read operation into the given read buffer, which is cleared (via {@link ByteBuffer#clear()}) before reading.
     *
     * @param _readBuffer
     * @param _minBytes
     */
    public void read( final ByteBuffer _readBuffer, final int _minBytes ) {

        try {

            // sanity checks...
            if( isNull( _readBuffer ) ) throw new TCPPipeException( "_readBuffer is null" );
            if( (_minBytes < 1) || (_minBytes > _readBuffer.capacity())) throw new TCPPipeException( "_minBytes out of range: " + _minBytes );

            // make sure we haven't already got a read operation in progress...
            if( readInProgress.getAndSet( true ) ) throw new TCPPipeException( "Read operation already in progress" );

            // clear the read buffer, so we know it's ready to read into, and squirrel it away...
            _readBuffer.clear();
            readBuffer = _readBuffer;
        }
        catch( TCPPipeException _e ) {
            postReadCompletion( forgeByteBuffer.notOk( _e.getMessage() ) );
        }

        read();
    }


    public void read( final ByteBuffer _readBuffer ) {
        read( _readBuffer, 1 );
    }


    // called by NetworkingEngine when the channel is readable...
    /* package-private */ void onReadable() {

        // if there's no read in progress, we just log and ignore this call...
        if( !readInProgress.get() ) {
            LOGGER.log( Level.WARNING, "TCPPipe::onReadable() called when no read was in progress; ignoring" );
            return;
        }

        read();
    }


    private void read() {
        try {
            // read what data we can...
            var bytesRead = channel.read( readBuffer );

            // if the total bytes read is at least the minimum number of bytes, then we're done...
            if( readBuffer.position() >= minBytes ) {
                postReadCompletion( forgeByteBuffer.ok( readBuffer ) );
            }

            // otherwise, we need to express interest again...
            else {
                key.interestOpsOr( READ_INTEREST );
                engine.wakeSelector();
            }
        }
        catch( IOException _e ) {
            postReadCompletion( forgeByteBuffer.notOk( "Problem reading from channel: " + _e.getMessage(), _e ) );
        }
    }


    private void postReadCompletion( final Outcome<ByteBuffer> _outcome ) {
        readInProgress.set( false );
        engine.execute( () -> onReadReadyHandler.accept( _outcome ) );
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


    public void close() {

    }


    protected static class TCPPipeException extends Exception {
        public TCPPipeException( final String message ) {
            super( message );
        }
    }
}
