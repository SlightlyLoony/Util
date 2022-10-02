package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Instances of this class (or its subclasses) allow network communications with the TCP protocol, for connections made through an instance of {@link TCPListener}.
 */
public class TCPInboundPipe extends TCPPipe {


    private static final Outcome.Forge<TCPInboundPipe>      forgeTCPInboundPipe = new Outcome.Forge<>();


    /**
     * Attempts to create a new instance of this class, associated with the given {@link NetworkingEngine} and using the given {@link SocketChannel}.  This method assumes that the
     * given channel is already connected.  Normally this method is called only by {@link TCPListener} upon acceptances of an inbound connection.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _channel The {@link SocketChannel} for the new instance to use.
     * @return The outcome of attempting to create a new instance of this class.  If ok, the info contains the new instance.  If not ok, there is an explanatory message and
     * possibly the exception that caused the problem.
     */
    /* package-private */ static Outcome<TCPInboundPipe> getTCPInboundPipe( final NetworkingEngine _engine, final SocketChannel _channel ) {

        try {

            // make sure the channel we've been given is actually there, and is connected...
            if( (_channel != null) && !_channel.isConnected() ) return forgeTCPInboundPipe.notOk( "_channel is not connected" );

            // construct our new instance...
            var pipe = new TCPInboundPipe( _engine, _channel );
            return forgeTCPInboundPipe.ok( pipe );
        }
        catch( Exception _e ) {
            return forgeTCPInboundPipe.notOk( "Problem creating or configuring TCP inbound pipe: " + _e.getMessage(), _e );
        }
    }


    /**
     * Creates a new instance of this class, associated with the given {@link NetworkingEngine} and using the given {@link SocketChannel}.  This method assumes that the
     * given channel is already connected.
     *
     * @param _engine The {@link NetworkingEngine} to associate with the new instance.
     * @param _channel The {@link SocketChannel} for the new instance to use.
     * @throws IOException on any I/O problem.
     */
    protected TCPInboundPipe( final NetworkingEngine _engine, final SocketChannel _channel ) throws IOException {
        super( _engine, _channel );
    }


    /**
     * Return a string representing this instance.
     *
     * @return A string representing this instance.
     */
    public String toString() {
        return "TCPInboundPipe: (" + channel.socket().getInetAddress().getHostAddress() + " port " + channel.socket().getPort() + ")";
    }
}
