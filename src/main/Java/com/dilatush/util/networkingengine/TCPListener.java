package com.dilatush.util.networkingengine;

import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;

public class TCPListener {

    private static final Logger                      LOGGER = getLogger();

    private final IPAddress           ip;
    private final int                 port;
    private final ServerSocketChannel channel;
    private final ServerSocket        socket;
    private final SelectionKey        key;
    private final NetworkingEngine    engine;


    public TCPListener( final NetworkingEngine _engine, final IPAddress _ip, final int _port ) throws IOException {

        ip = _ip;
        port = _port;
        engine = _engine;

        channel = ServerSocketChannel.open();
        channel.configureBlocking( false );
        channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );

        socket = channel.socket();
        socket.bind( new InetSocketAddress( ip.toInetAddress(), port ) );

        // register our channel with the selector, with ourselves as the attachment...
        key = engine.register( channel, channel.validOps(), this );
    }


    public ServerSocket getSocket() {
        return socket;
    }


    /* package-private */ void onAcceptable() {

        try {
            var getPipeOutcome = TCPPipe.getTCPPipe( engine, channel.accept() );
            if( getPipeOutcome.ok() ) {
                var pipe = getPipeOutcome.info();
                pipe.register( key.selector() );
                LOGGER.finest( "Accepted TCP connection from " + pipe );
            }
            else
                LOGGER.log( Level.WARNING, "Problem getting TCPPipe: " + getPipeOutcome.msg(), getPipeOutcome.cause() );
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem accepting inbound TCP connection: " + _e.getMessage(), _e );
        }
    }


    public String toString() {
        return "TCPListener (" + ip.toString() + ", port " + port + ")";
    }
}
