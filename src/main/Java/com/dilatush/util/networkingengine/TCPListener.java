package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

public class TCPListener {

    private static final Logger                      LOGGER = getLogger();

    private static final Outcome.Forge<TCPListener>  forgeTCPListener = new Outcome.Forge<>();

    private final IPAddress           ip;
    private final int                 port;
    private final ServerSocketChannel channel;
    private final ServerSocket        socket;
    private final SelectionKey        key;
    private final NetworkingEngine    engine;
    private final Consumer<TCPPipe>   onAccept;


    // _ip can be wildcard address
    /* package-private */ static Outcome<TCPListener> getInstance( final NetworkingEngine _engine, final IPAddress _ip, final int _port, final Consumer<TCPPipe> _onAccept ) {

        try {

            // sanity checks...
            if( isNull( _engine, _ip) )           return forgeTCPListener.notOk( "_engine or _ip is null" );
            if( (_port < 1) || (_port > 0xFFFF) ) return forgeTCPListener.notOk( "_port is out of range (1-65535): " + _port );

            // attempt to get our instance...
            return forgeTCPListener.ok( new TCPListener( _engine, _ip, _port, _onAccept ) );
        }
        catch( Exception _e ) {
            return forgeTCPListener.notOk( "Problem instantiating TCPListener: " + _e.getMessage(), _e );
        }
    }


    private TCPListener( final NetworkingEngine _engine, final IPAddress _ip, final int _port, final Consumer<TCPPipe> _onAccept ) throws IOException {

        ip = _ip;
        port = _port;
        engine = _engine;
        onAccept = _onAccept;

        channel = ServerSocketChannel.open();
        channel.setOption( StandardSocketOptions.SO_REUSEADDR, true );

        socket = channel.socket();
        socket.bind( new InetSocketAddress( ip.toInetAddress(), port ) );
        channel.configureBlocking( false );

        // register our channel with the selector, with ourselves as the attachment...
        key = engine.register( channel, channel.validOps(), this );
    }


    public ServerSocket getSocket() {
        return socket;
    }


    /* package-private */ void onAcceptable() {

        try {
            var getPipeOutcome = getPipe();
            if( getPipeOutcome.ok() ) {
                var pipe = getPipeOutcome.info();
                pipe.register( key.selector() );
                if( onAccept != null ) onAccept.accept( pipe );
                LOGGER.finest( "Accepted TCP connection from " + pipe );
            }
            else
                LOGGER.log( Level.WARNING, "Problem getting TCPPipe: " + getPipeOutcome.msg(), getPipeOutcome.cause() );
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem accepting inbound TCP connection: " + _e.getMessage(), _e );
        }
    }


    protected Outcome<TCPPipe> getPipe() throws IOException {
        return TCPPipe.getTCPPipe( engine, channel.accept() );
    }


    public String toString() {
        return "TCPListener (" + ip.toString() + ", port " + port + ")";
    }
}
