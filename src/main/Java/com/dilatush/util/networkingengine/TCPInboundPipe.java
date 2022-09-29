package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Instances of this class (or its subclasses) are used to communicate over the TCP protocol.  Instances may be created by a listener ({@link TCPListener} to respond to inbound
 * TCP connections, or they may be created by application code to initiate an outbound TCP connection.
 */
public class TCPInboundPipe extends TCPPipe {


    private static final Outcome.Forge<TCPInboundPipe>      forgeTCPInboundPipe = new Outcome.Forge<>();



    /* package-private */ static Outcome<TCPInboundPipe> getTCPInboundPipe( final NetworkingEngine _engine, final SocketChannel _channel ) {

        try {

            // make sure the channel we've been given is actually there, and is connected...
            if( (_channel != null) && !_channel.isConnected() ) return forgeTCPInboundPipe.notOk( "_channel is not connected" );

            var pipe = new TCPInboundPipe( _engine, _channel );
            return forgeTCPInboundPipe.ok( pipe );
        }
        catch( Exception _e ) {
            return forgeTCPInboundPipe.notOk( "Problem creating or configuring TCP inbound pipe: " + _e.getMessage(), _e );
        }
    }



    protected TCPInboundPipe( final NetworkingEngine _engine, final SocketChannel _channel ) throws IOException {
        super( _engine, _channel );
    }


    public String toString() {
        return "TCPInboundPipe: " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();
    }
}
