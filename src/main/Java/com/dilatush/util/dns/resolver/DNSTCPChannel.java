package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.*;

public class DNSTCPChannel extends DNSChannel {

    private static final Logger                       LOGGER        = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );
    private static final Outcome.Forge<DNSTCPChannel> createOutcome = new Outcome.Forge<>();
    private static final long                         LINGER_MILLIS = 3000;   // allow TCP connection to linger for 3 seconds after last data sent or received...

    public  final SocketChannel        tcpChannel;
    public  final InetSocketAddress    socketAddress;

    private final ByteBuffer           prefix = ByteBuffer.allocate( 2 );
    private       ByteBuffer           message;
    private       DNSTCPLingerTimeout  timeout;


    private DNSTCPChannel( final DNSResolver _resolver, final SocketChannel _channel, final InetSocketAddress _socketAddress ) {
        super( _resolver, _channel );

        tcpChannel = _channel;
        socketAddress = _socketAddress;
    }


    public static Outcome<DNSTCPChannel> create( final DNSResolver _resolver, final InetSocketAddress _serverAddress ) {

        try {
            SocketChannel tcp = SocketChannel.open();
            tcp.configureBlocking( false );
            tcp.bind( null );
            return createOutcome.ok( new DNSTCPChannel( _resolver, tcp, _serverAddress ));
        }

        catch( IOException _e ) {
            return createOutcome.notOk( "Program creating SocketChannel", _e );
        }
    }


    @Override
    protected Outcome<?> send( final ByteBuffer _data ) {

        // prepend the TCP length field...
        ByteBuffer data = ByteBuffer.allocate( 2 + _data.limit() );
        data.putShort( (short) _data.limit() );
        if( _data.position() != 0 )
            _data.flip();
        data.put( _data );
        data.flip();

        boolean wasAdded = sendData.offerFirst( data );
        if( !wasAdded )
            return outcome.notOk( "Send data queue full" );

        // if we just added the first data, connect and set write interest on...
        if( !(tcpChannel.isConnected() || tcpChannel.isConnectionPending()) ) {
            try {
                tcpChannel.connect( socketAddress );
                DNSResolver.runner.register( this, channel, OP_WRITE | OP_READ | OP_CONNECT );
                return outcome.ok();
            }
            catch( ClosedChannelException _e ) {
                return outcome.notOk( "Problem registering write interest", _e );
            } catch( IOException _e ) {
                return outcome.notOk( "Could not connect: " + _e.getMessage(), _e );
            }
        }

        linger();

        return outcome.ok();
    }


    @Override
    public synchronized void write() {

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
                DNSResolver.runner.register( this, channel, OP_READ );
            } catch( ClosedChannelException _e ) {
                _e.printStackTrace();
            }
        }
    }


    @Override
    protected synchronized void read() {

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
                    resolver.handleReceivedData( message, DNSTransport.TCP );
                    message = null;
                    prefix.clear();
                }
            }

        } catch( IOException _e ) {
            _e.printStackTrace();
        }
    }


    private void linger() {

        if( "IO Runner".equals( Thread.currentThread().getName() ) )
            "".hashCode();

        LOGGER.log( Level.FINEST, "Setting TCP linger timeout" );
        if( timeout != null )
            timeout.cancel();
        timeout = new DNSTCPLingerTimeout( LINGER_MILLIS, this::handleLingerTimeout );
        DNSResolver.runner.addTimeout( timeout );
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
}
