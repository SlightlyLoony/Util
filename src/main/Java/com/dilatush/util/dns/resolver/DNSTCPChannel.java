package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;

public class DNSTCPChannel extends DNSChannel {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<DNSTCPChannel> createOutcome = new Outcome.Forge<>();

    public final SocketChannel tcpChannel;


    private DNSTCPChannel( final DNSResolver _resolver, final SocketChannel _channel ) {
        super( _resolver, _channel );

        tcpChannel = _channel;
    }


    public static Outcome<DNSTCPChannel> create( final DNSResolver _resolver, final InetSocketAddress _serverAddress ) {

        try {
            SocketChannel tcp = SocketChannel.open();
            tcp.configureBlocking( false );
            tcp.bind( null );
            return createOutcome.ok( new DNSTCPChannel( _resolver, tcp ));
        }

        catch( IOException _e ) {
            return createOutcome.notOk( "Program creating SocketChannel", _e );
        }
    }


    @Override
    public synchronized void write() {

        ByteBuffer buffer = sendData.pollLast();

        if( buffer != null ) {
            try {
                tcpChannel.write( buffer );
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
        ByteBuffer readData = ByteBuffer.allocate( 512 );
        try {
            tcpChannel.read( readData );
        } catch( IOException _e ) {
            _e.printStackTrace();
        }
    }
}
