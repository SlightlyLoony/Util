package com.dilatush.util.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Instances of this class are created and started (in their own thread) when a client TCP connection has been successfully accepted.  This class
 * implements the protocol described in the <a href="package-summary.html">package documentation</a>.  Once the connection is fully established, the
 * connection data streams are handed off to the selected console provider.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ConsoleClientConnection extends Thread {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final Socket               socket;         // the socket for the connection to our client...
    private final ConsoleServer        server;         // the server that provided this connection...


    public ConsoleClientConnection( final Socket _socket, final ConsoleServer _server ) {

        socket        = _socket;
        server        = _server;

        setName( "Console client connection " + ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress().toString() );
        setDaemon( true );
        start();
    }


    @Override
    public void run() {


        try {

            // get our data streams...
            InputStream  rawIS = socket.getInputStream();
            OutputStream rawOS = socket.getOutputStream();

            // send our banner to the client...
            rawOS.write( getBanner() );
            rawOS.flush();

//            // hand off our data streams to the console provider...
//            provider.setStreams( socket.getInputStream(), socket.getOutputStream() );
//
//            // now run our console provider...
//            try {
//                provider.run();
//            }
//            catch( IOException _e ) {
//                LOGGER.log( Level.WARNING, "Problem when running console client", _e );
//            }
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem getting input or output stream from connected socket for console client", _e );
        }

        // the console provider has finished (or we've had a problem), so close our connection and we're done...
        try {
            socket.close();
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem when closing connected socket for console client", _e );
        }
        server.clients.decrementAndGet();
    }


    private byte[] getBanner() {
        return ("Console Server," + ConsoleServer.VERSION + "," + server.config.name + "\n").getBytes( StandardCharsets.UTF_8 );
    }
}
