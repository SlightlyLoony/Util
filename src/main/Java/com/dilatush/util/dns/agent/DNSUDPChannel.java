package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class DNSUDPChannel extends DNSChannel {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private DatagramChannel udpChannel;


    public DNSUDPChannel( final DNSServerAgent _agent, final DNSNIO _nio, final ExecutorService _executor, final InetSocketAddress _serverAddress ) {
        super( _agent, _nio, _executor, _serverAddress );
    }


    @Override
    protected synchronized Outcome<?> send( final DNSMessage _msg ) {

        if( isNull( _msg) )
            throw new IllegalArgumentException( "Required message argument is missing" );

        Outcome<ByteBuffer> emo = _msg.encode();
        if( emo.notOk() )
            return outcome.notOk( "Could not encode message: " + emo.msg(), emo.cause() );

        boolean wasAdded = sendData.offerFirst( emo.info() );
        if( !wasAdded )
            return outcome.notOk( "Send data queue full" );

        // if we just added the first data, open the UDP socket, bind, connect, and set write interest on...
        if( sendData.size() == 1 ) {

            try {
                udpChannel = DatagramChannel.open();
                udpChannel.configureBlocking( false );
                udpChannel.bind( null );
                udpChannel.connect( serverAddress );
                nio.register( this, udpChannel, OP_WRITE | OP_READ );
                return outcome.ok();
            }
            catch( IOException _e ) {
                return outcome.notOk( "Could not send message via UDP", _e );
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
            executor.submit( () -> agent.handleReceivedData( readData, DNSTransport.UDP ) );
        }

        catch( IOException _e ) {
            _e.printStackTrace();
        }
    }


    @Override
    protected void close() {

        try {
            if( udpChannel != null )
                udpChannel.close();
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Exception when closing UDP channel", _e );
        }
    }
}
