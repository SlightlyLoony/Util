package com.dilatush.util.console;

import com.dilatush.util.Base64;
import com.dilatush.util.Crypto;
import com.dilatush.util.Sockets;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Key;
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

            // get our key...
            Key key = getKey();

            // get our decrypting input stream and turn it into a buffered reader...
            CipherInputStream cis = Crypto.getSocketInputStream_AES_128_CTR( socket, key );
            InputStreamReader isr = new InputStreamReader( cis, StandardCharsets.UTF_8 );
            BufferedReader br = new BufferedReader( isr, 1000 );

            // read the name of the desired console...
            String consoleName = br.readLine();
            // TODO: check whether we have the console, and invoke it...

            // get our encrypting output stream...
            CipherOutputStream cos = Crypto.getSocketOutputStream_AES_128_CTR( socket, key );
            OutputStreamWriter osw = new OutputStreamWriter( cos, StandardCharsets.UTF_8 );
            BufferedWriter bw = new BufferedWriter( osw, 1000 );

            // send our ok...
            bw.write( "OK\n" );
            bw.flush();

            rawIS.hashCode();

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
            LOGGER.log( Level.WARNING, "Console problem: " + _e.getMessage(), _e );
        }
        finally {
            Sockets.close( socket );
        }

        // the console provider has finished (or we've had a problem), so decrement our client count and we're done...
        server.clients.decrementAndGet();
    }


    private Key getKey() {
        byte[] keyBytes = Base64.decodeBytes( server.config.key );
        return new SecretKeySpec( keyBytes, "AES" );
    }


    private byte[] getBanner() {
        return ("Loony Console Server," + ConsoleServer.VERSION + "," + server.config.name + "\n").getBytes( StandardCharsets.UTF_8 );
    }
}
