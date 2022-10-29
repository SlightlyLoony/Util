package com.dilatush.util.networkingengine;

import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.Waiter;
import com.dilatush.util.ip.IPAddress;
import com.dilatush.util.ip.IPv4Address;
import com.dilatush.util.networkingengine.interfaces.OnErrorHandler;
import com.dilatush.util.networkingengine.interfaces.OnReceiveDatagramHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

// TODO: all the classes using channels need close mechanisms...
public class UDPClient extends UDPBase {

    private static final Outcome.Forge<InboundDatagram> forgeInboundDatagram = new Outcome.Forge<>();
    private static final Outcome.Forge<UDPClient>       forgeUDPClient       = new Outcome.Forge<>();

    private static final Logger LOGGER = getLogger();


    protected final AtomicBoolean receiveInProgress;

    protected OnReceiveDatagramHandler onReceiveDatagramHandler;


    public static Outcome<UDPClient> getNewInstance( final NetworkingEngine _engine,
                                                     final IPAddress _bindToAddress, final int _bindToPort,
                                                     final IPAddress _remoteAddress, final int _remotePort,
                                                     final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler ) {

        try {
            // get a datagram channel...
            var protocolFamily = (_bindToAddress instanceof IPv4Address) ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;
            var channel = DatagramChannel.open( protocolFamily );
            channel.configureBlocking( false );

            // bind and connect our channel...
            channel.bind( new InetSocketAddress( _bindToAddress.toInetAddress(), _bindToPort ) );
            channel.connect( new InetSocketAddress( _remoteAddress.toInetAddress(), _remotePort ) );

            return forgeUDPClient.ok( new UDPClient( _engine, channel, _maxDatagramBytes, _onErrorHandler ) );
        }
        catch( final Exception _e ) {
            return forgeUDPClient.notOk( "Problem instantiating UDPClient: " + _e.getMessage(), _e );
        }

    }


    protected UDPClient( final NetworkingEngine _engine, final DatagramChannel _channel,
                         final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler ) throws IOException {
        super( _engine, _channel, _maxDatagramBytes, _onErrorHandler );

        // some setup...
        receiveInProgress = new AtomicBoolean( false );
    }


    public void receive( final OnReceiveDatagramHandler _onReceiveDatagramHandler ) throws NetworkingEngineException {

        // if we don't get a handler, we really have no alternative to an exception...
        if( isNull( _onReceiveDatagramHandler ) ) throw new NetworkingEngineException( "_onReceiveDatagramHandler is null" );

        // if we have a receive operation in progress, stop here and tell the caller...
        if( receiveInProgress.getAndSet( true ) ) throw new NetworkingEngineException( "Receive operation already in progress" );

        // save our handler...
        onReceiveDatagramHandler = _onReceiveDatagramHandler;

        receiveImpl();

    }


    public Outcome<InboundDatagram> receive() throws NetworkingEngineException {
        var waiter = new Waiter<Outcome<InboundDatagram>>();
        receive( waiter::complete );
        return waiter.waitForCompletion();
    }


    /**
     *
     */
    @Override
    /* package-private */ void onReadable() {
        receiveImpl();
    }


    private void receiveImpl() {

        try {
            // get a read buffer with one extra byte, so we can tell if the datagram was truncated...
            var readBuffer = ByteBuffer.allocate( maxDatagramBytes + 1 );

            // read our datagram, returning the socket address of the sender...
            var socket = (InetSocketAddress) channel.receive( readBuffer );

            // if we get a non-null for the socket address, we got a datagram...
            if( socket != null ) {

                // handle the case of the datagram being truncated...
                var truncated = ( readBuffer.limit() == readBuffer.capacity() );
                if( truncated ) readBuffer.limit( readBuffer.limit() - 1 );   // getting rid of the extra truncation-detection byte...

                // make our datagram...
                readBuffer.flip();
                var datagram = new InboundDatagram( readBuffer, socket, truncated );

                // post our ok completion...
                postReceiveOutcome( forgeInboundDatagram.ok( datagram ) );
            }
        }
        catch( Exception _e ) {
            LOGGER.log( Level.FINE, "Problem in onReadable: " + General.toString( _e ), _e );
            postReceiveOutcome( forgeInboundDatagram.notOk( "Problem in onReadable: " + General.toString( _e ), _e ) );
        }
    }


    private void postReceiveOutcome( final Outcome<InboundDatagram> _outcome ) {

        // if there was no read in progress, just return...
        if( !receiveInProgress.getAndSet( false ) ) return;

        // otherwise, send the completion...
        engine.execute( () -> onReceiveDatagramHandler.handle( _outcome ) );
    }
}
