package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;

public class DNSUDPChannel extends DNSChannel {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<DNSUDPChannel> createOutcome = new Outcome.Forge<>();

    public final DatagramChannel udpChannel;


    private DNSUDPChannel( final DNSResolver _resolver, final DatagramChannel _channel ) {
        super( _resolver, _channel );

        udpChannel = _channel;
    }


    public static Outcome<DNSUDPChannel> create( final DNSResolver _resolver, final InetSocketAddress _serverAddress ) {

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


    // TODO: error handling needed...

    @Override
    public synchronized void write() {

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
                DNSResolver.runner.register( this, channel, OP_READ );
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
            resolver.handleReceivedData( readData );
        }

        catch( IOException _e ) {
            _e.printStackTrace();
        }
    }
}
