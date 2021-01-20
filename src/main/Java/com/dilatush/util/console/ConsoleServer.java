package com.dilatush.util.console;

import com.dilatush.util.AConfig;
import com.dilatush.util.Networking;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements an embeddable TCP console server, designed to let embedded or daemon applications provide a simple command line interface.
 * See the <a href="package-summary.html">package documentation</a> for information about the protocol used.  A
 * reference implementation of the other side, the Console Client, is provided in the author's
 * <a href="https://github.com/SlightlyLoony/ConsoleClient" target="_blank">ConsoleClient</a> github project.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ConsoleServer {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    final static public String VERSION = "1.0";

    final static private int  RECEIVE_BUFFER_SIZE = 1024;
    final static private long IO_PROBLEM_WAIT     = 10000;   // ten seconds...

    // our configuration...
    /*package-private*/ final Config        config;
    /*package-private*/ final AtomicInteger clients;  // count of connected clients...

    private ServerSocket         socket;          // the server socket...



    public ConsoleServer( final Config _config ) {

        config = _config;
        clients       = new AtomicInteger( 0 );

        // add our echo provider...
        config.providers.put( "echo", "com.dilatush.util.console.EchoConsoleProvider" );
    }


    public void start() {

        // if we're already running, log a warning and leave...
        if( socket != null ) {
            LOGGER.warning( "Tried to start ConsoleServer when it was already running" );
            return;
        }

        // start up the listener thread...
        new ServerThread();
    }


    private class ServerThread extends Thread {

        private ServerThread() {
            setName( "Console Listener" );
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
                        SocketAddress socketAddress = new InetSocketAddress( config.bindTo, config.port );
                        socket = new ServerSocket();
                        socket.setReceiveBufferSize( RECEIVE_BUFFER_SIZE );
                        socket.setReuseAddress( true );
                        socket.setSoTimeout( 0 );                  // zero means no timeout; we'll wait forever for this...
                        socket.bind( socketAddress, config.maxClients );
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
                        if( clients.get() > config.maxClients ) {
                            try {
                                clientSocket.close();
                            }
                            catch( IOException _e ) {
                                // we ignore this; it may not be caused by the server socket at all...
                            }
                            clients.decrementAndGet();
                            continue;  // keep on waiting...
                        }

                        LOGGER.fine( "Console client connected from " + Networking.toString( clientSocket.getRemoteSocketAddress() ) );

                        // we're ok with this connection, so make our console client connection and let it go to town...
                        new ConsoleClientConnection( clientSocket, ConsoleServer.this );
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

        public int                 maxClients;      // the maximum number of clients allowed simultaneously...
        public String              name;            // the name of this console server...
        public String              key;             // the base64 encoded shared secret for this console server...
        public int                 port;            // the port to listen for client connections on...
        public InetAddress         bindTo;          // the IP address of the network interface to listen on; default is all interfaces...
        public Map<String, String> providers;       // map of console names to the fully qualified class names of the console provider of that name...


        /**
         * Verify the validity of this object.  Each error found adds an explanatory message to the given list of messages.
         *
         * @param _messages The list of messages explaining the errors found.
         */
        @Override
        public void verify( final List<String> _messages ) {

        }
    }
}
