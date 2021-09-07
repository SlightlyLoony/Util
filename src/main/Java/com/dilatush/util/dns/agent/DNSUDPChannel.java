package com.dilatush.util.dns.agent;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class DNSUDPChannel extends DNSChannel {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<DNSUDPChannel> createOutcome = new Outcome.Forge<>();

    public final DatagramChannel udpChannel;


    private DNSUDPChannel( final DNSServerAgent _resolver, final DatagramChannel _channel ) {
        super( _resolver, _channel );

        udpChannel = _channel;
    }


    public static Outcome<DNSUDPChannel> create( final DNSServerAgent _resolver, final InetSocketAddress _serverAddress ) {

        try {
            DatagramChannel udp = DatagramChannel.open();
            udp.configureBlocking( false );
            udp.bind( null );
            udp.connect( _serverAddress );
            return createOutcome.ok( new DNSUDPChannel( _resolver, udp ));
        }

        catch( IOException _e ) {
            return createOutcome.notOk( "Problem creating DatagramChannel", _e );
        }
    }


    @Override
    protected synchronized Outcome<?> send( final ByteBuffer _data ) {

        if( _data.position() != 0 )
            _data.flip();

        boolean wasAdded = sendData.offerFirst( _data );
        if( !wasAdded )
            return outcome.notOk( "Send data queue full" );

        // if we just added the first data, set write interest on...
        if( sendData.size() == 1 ) {
            try {
                DNSServerAgent.runner.register( this, channel, OP_WRITE | OP_READ );
                return outcome.ok();
            }
            catch( ClosedChannelException _e ) {
                return outcome.notOk( "Problem registering write interest", _e );
            }
        }

        return outcome.ok();
    }


    // TODO: error handling needed...

    @Override
    public void write() {

        ByteBuffer buffer = sendData.pollLast();

        if( buffer != null ) {
            try {
                udpChannel.write( buffer );
            } catch( IOException _e ) {
                _e.printStackTrace();
            }
        }

        if( sendData.isEmpty() ) {
            try {
                DNSServerAgent.runner.register( this, channel, OP_READ );
            } catch( ClosedChannelException _e ) {
                _e.printStackTrace();
            }
        }
    }


    @Override
    protected void read() {

        try {
            ByteBuffer readData = ByteBuffer.allocate( 512 );

            if( udpChannel.read( readData ) == 0 )
                return;

            readData.flip();
            DNSServerAgent.runner.executor.submit( () -> resolver.handleReceivedData( readData, DNSTransport.UDP ) );
        }

        catch( IOException _e ) {
            _e.printStackTrace();
        }
    }
}
