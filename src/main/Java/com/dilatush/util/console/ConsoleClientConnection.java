package com.dilatush.util.console;

import com.dilatush.util.Base64;
import com.dilatush.util.Crypto;
import com.dilatush.util.Networking;
import com.dilatush.util.Sockets;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.Strings.isEmpty;

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


    /**
     * Create a new instance of this class with the given {@link Socket} and {@link ConsoleServer}.
     *
     * @param _socket The socket for the client TCP connection to the console server.
     * @param _server The {@link ConsoleServer} that created this instance.
     */
    public ConsoleClientConnection( final Socket _socket, final ConsoleServer _server ) {

        socket        = _socket;
        server        = _server;

        setName( "Console from: " + Networking.toString( socket.getRemoteSocketAddress() ) );
        setDaemon( true );
        start();
    }


    /**
     * Run this client connection, in its own thread.  This method handles the protocol to establish the encrypted TCP connection, then calls
     * whatever console provider the console client specified.  This method exits (which terminates the TCP connection) if there is any problem
     * establishing the connection, instantiating the provider, or when the provider has finished.
     */
    @Override
    public void run() {

        try {

            // get our data streams...
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
            String consoleClassName = server.config.providers.get( consoleName );

            // if we know about the provider, keep on truckin'...
            if( !isEmpty( consoleClassName) ) {

                // get our encrypting output stream...
                CipherOutputStream cos = Crypto.getSocketOutputStream_AES_128_CTR( socket, key );
                OutputStreamWriter osw = new OutputStreamWriter( cos, StandardCharsets.UTF_8 );
                BufferedWriter bw = new BufferedWriter( osw, 1000 );

                // send our ok...
                bw.write( "OK\n" );
                bw.flush();

                // instantiate our console provider...
                try {

                    // get the class object for our console provider...
                    Class<?> klass = Class.forName( consoleClassName );

                    // make sure it subclasses ConsoleProvider...
                    if( !ConsoleProvider.class.isAssignableFrom( klass ) )
                        throw new IOException( "Class is not a ConsoleProvider: " + consoleClassName );

                    // get the no-args constructor for the console provider class...
                    Constructor<?> ctor = klass.getConstructor();

                    // instantiate our provider...
                    ConsoleProvider provider = (ConsoleProvider) ctor.newInstance();
                    LOGGER.fine( "Instantiated console provider: " + consoleClassName );

                    // run the provider; we return when it's finished or there was a problem...
                    provider.run( socket, br, bw );

                }
                catch( ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException
                        | InvocationTargetException _e ) {
                    throw new IOException( "Problem instantiating console provider: " + _e.getMessage(), _e );
                }
            }
            else {
                LOGGER.fine( "Could not find provider by this name: " + consoleName );
            }
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Console problem: " + _e.getMessage(), _e );
        }
        finally {
            Sockets.close( socket );
        }

        // the console provider has finished (or we've had a problem), so decrement our client count and we're done...
        LOGGER.fine( "Shutting down console client connection" );
        server.clients.decrementAndGet();
    }


    /**
     * Return a {@link SecretKey} instance constructed from our configured key.
     *
     * @return the {@link SecretKey}
     */
    private Key getKey() {
        byte[] keyBytes = Base64.decodeBytes( server.config.key );
        return new SecretKeySpec( keyBytes, "AES" );
    }


    /**
     * Get the banner string.
     *
     * @return the banner string
     */
    private byte[] getBanner() {
        return ("Loony Console Server," + ConsoleServer.VERSION + "," + server.config.name + "\n").getBytes( StandardCharsets.UTF_8 );
    }
}
