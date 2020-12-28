package com.dilatush.util.console;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Instances of this class are created and started (in their own thread) when a client connection has been successfully accepted, first authenticating
 * the connection, then providing encryption and decryption of the data streams in both directions.  Once the connection has been authenticated and
 * encryption established, then the data streams (in the clear) in both directions are made available to a console provider.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ConsoleClientConnection extends Thread {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final Socket               socket;         // the socket for the connection to our client...
    private final ConsoleAuthenticator authenticator;  // the authenticator to use for this connection...
    private final ConsoleEncryptor     encryptor;      // the encryptor to use for this connection...
    private final ConsoleProvider      provider;       // the provider to use for this connection...
    private final AtomicInteger        clientCount;    // the number of clients currently connected...


    public ConsoleClientConnection( final Socket _socket, final ConsoleAuthenticator _authenticator,
                                    final ConsoleEncryptor _encryptor, final ConsoleProvider _provider, final AtomicInteger _clientCount ) {

        socket        = _socket;
        authenticator = _authenticator;
        encryptor     = _encryptor;
        provider      = _provider;
        clientCount   = _clientCount;

        setName( "Console client connection " + ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress().toString() );
        setDaemon( true );
        start();
    }


    @Override
    public void run() {

        // authenticate...

        // set up encryption...

        try {

            // hand off our data streams to the console provider...
            provider.setStreams( socket.getInputStream(), socket.getOutputStream() );

            // now run our console provider...
            try {
                provider.run();
            }
            catch( IOException _e ) {
                LOGGER.log( Level.WARNING, "Problem when running console client", _e );
            }
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
        clientCount.decrementAndGet();
    }
}
