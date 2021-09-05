package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.*;

public class DNSTCPChannel extends DNSChannel {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<DNSTCPChannel> createOutcome = new Outcome.Forge<>();

    public final SocketChannel tcpChannel;

    public final InetSocketAddress socketAddress;

    private final ByteBuffer prefix = ByteBuffer.allocate( 2 );
    private       ByteBuffer message;


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
        if( sendData.size() == 1 ) {
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
            if( prefix.hasRemaining() ) {
                tcpChannel.read( prefix );
                if( prefix.hasRemaining() ) {
                    return;
                }
            }
            if( message == null ) {
                prefix.flip();
                int messageLength = prefix.getShort() & 0xFFFF;
                message = ByteBuffer.allocate( messageLength );
            }
            tcpChannel.read( message );
            if( !message.hasRemaining() ) {
                message.flip();
                resolver.handleReceivedData( message, DNSTransport.TCP );
                message = null;
                prefix.clear();
            }

        } catch( IOException _e ) {
            _e.printStackTrace();
        }
    }
}
