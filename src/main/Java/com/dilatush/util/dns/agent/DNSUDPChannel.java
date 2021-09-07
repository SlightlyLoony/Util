package com.dilatush.util.dns.agent;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class DNSUDPChannel extends DNSChannel {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    public final DatagramChannel udpChannel;


    protected DNSUDPChannel( final DNSServerAgent _agent, final DNSNIO _nio, final InetSocketAddress _serverAddress ) throws IOException {
        super( _agent, _nio );

        udpChannel = DatagramChannel.open();
        udpChannel.configureBlocking( false );
        udpChannel.bind( null );
        udpChannel.connect( _serverAddress );
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
                nio.register( this, udpChannel, OP_WRITE | OP_READ );
                return outcome.ok();
            }
            catch( ClosedChannelException _e ) {
                return outcome.notOk( "Problem registering write interest", _e );
            }
        }

        return outcome.ok();
    }

    @Override
    protected void register( final Selector _selector, final int _operations, final Object _attachment ) throws ClosedChannelException {
        udpChannel.register( _selector, _operations, _attachment );
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
                nio.register( this, udpChannel, OP_READ );
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
            agent.executor.submit( () -> agent.handleReceivedData( readData, DNSTransport.UDP ) );
        }

        catch( IOException _e ) {
            _e.printStackTrace();
        }
    }
}
