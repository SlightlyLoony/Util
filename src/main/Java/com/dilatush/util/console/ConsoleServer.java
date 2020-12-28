package com.dilatush.util.console;

import com.dilatush.util.AConfig;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements an embeddable TCP console server, designed to let embedded or daemon applications provide a simple command line interface.  A simple
 * authentication mechanism is provided, and it is extensible.  Similarly, a simple encryption mechanism is provided, and it is also extensible.  A
 * reference implementation of the other side, the Console Client, is provided in the author's
 * <a href="https://github.com/SlightlyLoony/ConsoleClient" target="_blank">ConsoleClient</a> github project.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ConsoleServer {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    final static private int  RECEIVE_BUFFER_SIZE = 1024;
    final static private long IO_PROBLEM_WAIT     = 10000;   // ten seconds...

    // configurable values...
    private final int                  maxClients;      // the maximum number of clients allowed simultaneously...
    private final int                  port;            // the port to listen for client connections on...
    private final InetAddress          bindTo;          // the IP address of the network interface to listen on; default is all interfaces...
    private final ConsoleAuthenticator authenticator;   // the authenticator that will be used to authenticate console client connections...
    private final ConsoleProvider      provider;        // the console provider that listens to commands and sends responses...
    private final ConsoleEncryptor     encryptor;       // the encryptor that will be used to encrypt client connections...

    private ServerSocket         socket;          // the server socket...
    private ServerThread         serverThread;    // the console server listener thread...

    private AtomicInteger clients;  // count of connected clients...


    public ConsoleServer( final Config _config ) {

        maxClients = _config.maxClients;
        port          = _config.port;
        bindTo        = _config.bindTo;
        authenticator = _config.authenticator;
        provider      = _config.provider;
        encryptor     = _config.encryptor;
        clients       = new AtomicInteger( 0 );
    }


    public void start() {

        // if we're already running, log a warning and leave...
        if( socket != null ) {
            LOGGER.warning( "Tried to start ConsoleServer when it was already running" );
            return;
        }

        // start up the listener thread...
        serverThread = new ServerThread();
    }


    private class ServerThread extends Thread {

        private ServerThread() {
            setName( "Console server listener" );
            setDaemon( true );
            start();
        }


        @Override
        public void run() {

            // we loop here forever, though normally the loop is only executed once - the looping happens if there's an I/O error of some kind...
            while( !interrupted() ) {

                try {
                    // establish the listening socket...
                    try {
                        SocketAddress socketAddress = new InetSocketAddress( bindTo, port );
                        socket = new ServerSocket();
                        socket.setReceiveBufferSize( RECEIVE_BUFFER_SIZE );
                        socket.setReuseAddress( true );
                        socket.setSoTimeout( 0 );                  // zero means no timeout; we'll wait forever for this...
                        socket.bind( socketAddress, maxClients );
                    }
                    catch( IOException _e ) {
                        LOGGER.log( Level.WARNING, "Failed to open socket for ConsoleServer", _e );
                        try {
                            //noinspection BusyWait
                            sleep( IO_PROBLEM_WAIT );
                        }
                        catch( InterruptedException _interruptedException ) {
                            // ignore this; our loop will exit anyway...
                        }
                        continue;
                    }

                    // now we loop forever, waiting for console client connections...
                    while( !interrupted() ) {

                        // wait for a client to connect...
                        Socket clientSocket = socket.accept();

                        // bump our count up...
                        clients.incrementAndGet();

                        // if we've exceeded our count, kill it...
                        if( clients.get() > maxClients ) {
                            try {
                                clientSocket.close();
                            }
                            catch( IOException _e ) {
                                // we ignore this; it may not be caused by the server socket at all...
                            }
                            clients.decrementAndGet();
                            continue;  // keep on waiting...
                        }

                        // we're ok with this connection, so make our console client connection and let it go to town...
                        new ConsoleClientConnection( clientSocket, authenticator, encryptor, provider, clients );
                    }
                }
                catch( IOException _e ) {
                    LOGGER.log( Level.WARNING, "Problem while listening for console clients", _e );
                    try {
                        //noinspection BusyWait
                        sleep( IO_PROBLEM_WAIT );
                    }
                    catch( InterruptedException _interruptedException ) {
                        // ignore this; our loop will exit anyway...
                    }
                }
            }
        }
    }


    public static class Config extends AConfig {

        public int                  maxClients;      // the maximum number of clients allowed simultaneously...
        public int                  port;            // the port to listen for client connections on...
        public InetAddress          bindTo;          // the IP address of the network interface to listen on; default is all interfaces...
        public ConsoleAuthenticator authenticator;   // the authenticator that will be used to authenticate console client connections...
        public ConsoleProvider      provider;        // the console provider that listens to commands and sends responses...
        public ConsoleEncryptor     encryptor;       // the encryptor that will be used to encrypt client connections...


        /**
         * Implemented by subclasses to verify that their fields are valid.  When possible, this should be accomplished by a series of invocations of
         * {@link #validate(Validator, String)}, one or more times for each field in the configuration.
         */
        @Override
        protected void verify() {

        }
    }
}
