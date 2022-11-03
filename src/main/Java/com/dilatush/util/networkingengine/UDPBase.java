package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.Waiter;
import com.dilatush.util.networkingengine.interfaces.OnErrorHandler;
import com.dilatush.util.networkingengine.interfaces.OnSendCompleteHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Base class for {@link UDPServer}, {@link UDPClient}, and their subclasses.
 */
@SuppressWarnings( "unused" )
/* package-private */ abstract class UDPBase {

    protected static final Outcome.Forge<?>       forge          = new Outcome.Forge<>();

    private static final Logger LOGGER = getLogger();

    // constants related to SelectionKey interest operations...
    protected static final int NO_INTEREST    = 0;
    protected static final int READ_INTEREST  = SelectionKey.OP_READ;
    protected static final int WRITE_INTEREST = SelectionKey.OP_WRITE;


    protected final NetworkingEngine engine;
    protected final DatagramChannel  channel;
    protected final int              maxDatagramBytes;
    protected final OnErrorHandler   onErrorHandler;
    protected final AtomicBoolean    sendInProgress;
    protected final SelectionKey     key;

    protected OnSendCompleteHandler onSendCompleteHandler;
    protected OutboundDatagram      outboundDatagram;

    /**
     * <p>Creates a new instance of this class that will be associated with the given networking engine, and will use the given {@link DatagramChannel}, which must be bound to
     * the interface(s) and port that this instance will listen for datagrams on.  The new instance will be able to receive datagrams from any source, and these datagrams may be
     * any length from one byte to the given maximum number of datagram bytes.  If the received datagram is longer than the given maximum number of bytes, then the truncated flag
     * is set in the datagram.  The on receipt handler is called with each datagram received, including any truncated datagrams.  The on error handler is called if any errors
     * occur.  The server is started and ready to receive datagrams as soon as it is instantiated.</p>
     *<p>Note that this constructor should not be invoked directly.  Instead, get a new instance of this class through one of the factory methods in this class.</p>
     *
     * @param _engine The {@link NetworkingEngine} to associate the new instance with.
     * @param _channel The {@link DatagramChannel} for this instance to use.
     * @param _maxDatagramBytes The maximum number of bytes to receive in a datagram (bytes more than this are truncated).
     * @param _onErrorHandler The handler to call when an error occurs.
     * @throws IOException on any I/O error.
     */
    protected UDPBase( final NetworkingEngine _engine, final DatagramChannel _channel,
                       final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler ) throws IOException {

        // sanity checks...
        if( isNull( _engine, _channel ) )
            throw new IllegalArgumentException( "_engine, _channel, or _onReceiptHandler is null" );
        if( (_maxDatagramBytes < 1) || (_maxDatagramBytes > 65535) )
            throw new IllegalArgumentException( "_maxDatagramBytes is out of range (1..65535): " + _maxDatagramBytes );

        // some setup...
        sendInProgress   = new AtomicBoolean( false );

        // squirrel it all away...
        engine           = _engine;
        channel          = _channel;
        maxDatagramBytes = _maxDatagramBytes;
        onErrorHandler   = (_onErrorHandler == null) ? this::defaultOnErrorHandler : _onErrorHandler;
        key              = _engine.register( channel, NO_INTEREST, this );
    }


    /* package-private */ abstract void onReadable();


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to send a datagram from the UDP port represented by this instance, from the given {@link OutboundDatagram}.  The
     * {@code _onSendCompleteHandler} is called when the write operation completes, whether that operation completed normally or was terminated because of an error.
     * Note that the {@code _onSendCompleteHandler} will always be called in one of the threads from the associated {@link NetworkingEngine}'s {@link ScheduledExecutor}, never in
     * the thread that calls this method.</p>
     *
     * @param _datagram The {@link OutboundDatagram} to send.
     * @param _onSendCompleteHandler The handler to call upon the completion of sending the datagram.
     * @throws NetworkingEngineException if no on send complete handler is specified, or if a send operation is in progress.
     */
    public void send( final OutboundDatagram _datagram, final OnSendCompleteHandler _onSendCompleteHandler ) throws NetworkingEngineException {

        // if we didn't get a send complete handler, then we really don't have any choice but to throw an exception...
        if( isNull( _onSendCompleteHandler ) ) throw new NetworkingEngineException( "_onSendCompleteHandler is null" );

        // make sure we haven't already got a send operation in progress...
        if( sendInProgress.getAndSet( true ) ) throw new NetworkingEngineException( "Send operation already in progress" );

        // sanity checks...
        if( isNull( _datagram ) ) {
            postSendCompletion( forge.notOk( "_datagram is null" ) );
            return;
        }

        // squirrel away our initial state...
        outboundDatagram       = _datagram;
        onSendCompleteHandler  = _onSendCompleteHandler;

        // initiate the actual write process...
        send();
   }


    /**
     * <p>Attempts a synchronous (blocking) operation to send a datagram from the UDP port represented by this instance, from the given {@link OutboundDatagram}.</p>
     *
     * @param _datagram The {@link OutboundDatagram} to send.
     * @return The outcome of the attempt.  If ok, the datagram was successfully sent.  If not ok, then there is an explanatory message and possibly the exception that caused
     * the problem.
     * @throws NetworkingEngineException if a send operation is in progress.
     */
    public Outcome<?> send( final OutboundDatagram _datagram ) throws NetworkingEngineException {
        Waiter<Outcome<?>> waiter = new Waiter<>();
        send( _datagram, waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     * Called by the {@link NetworkingEngine} whenever write interest is indicated in the selection key, and the engine detects that the connection is writeable.  Note that this
     * method is <i>always</i> called in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
     */
    /* package-private */ void onWriteable() {

        // if there's no send in progress, we just log and ignore this call...
        if( !sendInProgress.get() ) {
            LOGGER.log( Level.WARNING, "UDPBase::onWriteable() called when no send was in progress; ignoring" );
            return;
        }

        // handle the writeable event...
        send();
    }


    /**
     * Implements the core of the send operation.  It is called once when the write is initiated, and again for each writable event detected by the engine.  Note that this method
     * may be called in the user's thread <i>or</i> in one of the threads from the {@link NetworkingEngine}'s {@link ScheduledExecutor}.
     */
    private void send() {

        try {

            // write out the datagram...
            var socketAddress = new InetSocketAddress( outboundDatagram.getIpAddress().toInetAddress(), outboundDatagram.getPort() );
            var bytes = channel.send( outboundDatagram.getData(), socketAddress );

            // if we wrote any bytes, then we're done...
            if( bytes > 0 ) {

                // clear the write buffer for use by our caller, accumulate the bytes written, and post our completion...
                postSendCompletion( forge.ok() );
                LOGGER.finest( "Sent " + bytes + " bytes to " + outboundDatagram.getIpAddress() + " port " + outboundDatagram.getPort() );
            }

            // otherwise, we need to express write interest...
            else {
                key.interestOpsOr( WRITE_INTEREST );
                engine.wakeSelector();  // this guarantees that the key change will be effective immediately...
            }
        }
        catch( Exception _e ) {
            postSendCompletion( forge.notOk( "Problem sending to channel: " + _e.getMessage(), _e ) );
        }
    }


    /**
     * If a write operation is still in progress, marks it as complete (i.e., {@code writeInProgress} set to false) and posts the given {@link Outcome} to the
     * {@code onWriteCompleteHandler}.  If no write operation was in progress, this method does nothing.
     *
     * @param _outcome The {@link Outcome} to post.
     */
    private void postSendCompletion( final Outcome<?> _outcome ) {

        // if there was a send in progress, send the completion...
       if( sendInProgress.getAndSet( false ) ) {
           engine.execute( () -> onSendCompleteHandler.handle( _outcome ) );
       }
    }


    public void close() {

        try {
            channel.close();
        }
        catch( IOException _e ) {
            // naught to do...
        }

        // wake up the selector to make sure the close has immediate effect...
        engine.wakeSelector();
    }


    /**
     * The default {@code onErrorHandler}, which just logs the error as a warning.
     *
     * @param _message A message explaining the error.
     * @param _e The (optional) exception that caused the error.
     */
    private void defaultOnErrorHandler( final String _message, final Exception _e ) {
        LOGGER.log( Level.WARNING, _message, _e );
    }
}
