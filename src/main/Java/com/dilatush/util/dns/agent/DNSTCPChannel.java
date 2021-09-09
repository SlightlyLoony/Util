package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static java.nio.channels.SelectionKey.*;

public class DNSTCPChannel extends DNSChannel {

    private static final Logger                       LOGGER        = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );
    private static final long                         LINGER_MILLIS = 3000;   // allow TCP connection to linger for 3 seconds after last data sent or received...

    public        SocketChannel        tcpChannel;
    private final ByteBuffer           prefix = ByteBuffer.allocate( 2 );
    private       ByteBuffer           message;
    private       DNSTCPLingerTimeout  timeout;


    protected DNSTCPChannel( final DNSServerAgent _agent, final DNSNIO _nio, final ExecutorService _executor, final InetSocketAddress _serverAddress ) {
        super( _agent, _nio, _executor, _serverAddress );
    }


    @Override
    protected Outcome<?> send( final DNSMessage _msg ) {

        if( isNull( _msg) )
            throw new IllegalArgumentException( "Required message argument is missing" );

        Outcome<ByteBuffer> emo = _msg.encode();
        if( emo.notOk() )
            return outcome.notOk( "Could not encode message: " + emo.msg(), emo.cause() );
        ByteBuffer encodedMsg = emo.info();

        // prepend the TCP length field...
        ByteBuffer data = ByteBuffer.allocate( 2 + encodedMsg.limit() );
        data.putShort( (short) encodedMsg.limit() );
        if( encodedMsg.position() != 0 )
            encodedMsg.flip();
        data.put( encodedMsg );
        data.flip();

        boolean wasAdded = sendData.offerFirst( data );
        if( !wasAdded )
            return outcome.notOk( "Send data queue full" );

        if( tcpChannel == null ) {
            try {
                tcpChannel = SocketChannel.open();
                tcpChannel.configureBlocking( false );
                tcpChannel.bind( null );
            }
            catch( IOException _e ) {
                return outcome.notOk( "Could not open TCP channel: " + _e.getMessage(), _e );
            }
        }

        // if we just added the first data, connect and set write interest on...
        if( !(tcpChannel.isConnected() || tcpChannel.isConnectionPending()) ) {
            try {
                tcpChannel.connect( serverAddress );
                nio.register( this, tcpChannel, OP_WRITE | OP_READ | OP_CONNECT );
                return outcome.ok();
            }
            catch( IOException _e ) {
                return outcome.notOk( "Could not send message via TCP: " + _e.getMessage(), _e );
            }
        }

        linger();

        return outcome.ok();
    }

    @Override
    protected void register( final Selector _selector, final int _operations, final Object _attachment ) throws ClosedChannelException {
        tcpChannel.register( _selector, _operations, _attachment );
    }


    @Override
    public void write() {

        ByteBuffer buffer = sendData.peekLast();

        while( (buffer != null) ) {

            if( !buffer.hasRemaining() ) {
                sendData.pollLast();
                buffer = sendData.peekLast();
                continue;
            }

            try {
                int bytesWritten = tcpChannel.write( buffer );
                if( bytesWritten == 0 )
                    break;
                linger();

            } catch( IOException _e ) {
                _e.printStackTrace();
            }
        }

        if( sendData.isEmpty() ) {
            try {
                nio.register( this, tcpChannel, OP_READ );
            } catch( ClosedChannelException _e ) {
                _e.printStackTrace();
            }
        }
    }


    @Override
    protected void read() {

        try {
            if( prefix.hasRemaining() ) {
                tcpChannel.read( prefix );
                if( prefix.hasRemaining() ) {
                    return;
                }
                LOGGER.finest( "Read TCP prefix: " + (prefix.getShort( 0 ) & 0xFFFF) );
                linger();
            }
            if( message == null ) {
                prefix.flip();
                int messageLength = prefix.getShort() & 0xFFFF;
                message = ByteBuffer.allocate( messageLength );
                LOGGER.finest( "Made TCP message buffer: " + (prefix.getShort( 0 ) & 0xFFFF) );
            }
            if( tcpChannel.read( message ) != 0 ) {
                linger();
                if( !message.hasRemaining() ) {
                    message.flip();
                    LOGGER.finest( "Got message: " + message.limit() );
                    ByteBuffer msg = message;
                    executor.submit( () -> agent.handleReceivedData( msg, DNSTransport.TCP ) );
                    message = null;
                    prefix.clear();
                }
            }

        } catch( IOException _e ) {
            _e.printStackTrace();
        }
    }


    private void linger() {

        LOGGER.log( Level.FINEST, "Setting TCP linger timeout" );
        if( timeout != null )
            timeout.cancel();
        timeout = new DNSTCPLingerTimeout( LINGER_MILLIS, this::handleLingerTimeout );
        nio.addTimeout( timeout );
    }


    private void handleLingerTimeout() {

        if( (timeout == null) || timeout.isCancelled() )
            return;

        LOGGER.log( Level.FINEST, "TCP linger timeout" );
        timeout = null;
        try {
            tcpChannel.close();
        } catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem when closing TCP channel after lingering", _e );
        }
    }


    @Override
    protected void close() {


        try {
            if( tcpChannel != null )
                tcpChannel.close();
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Exception when closing TCP channel", _e );
        }
    }
}
