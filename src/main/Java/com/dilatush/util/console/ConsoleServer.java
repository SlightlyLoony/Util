package com.dilatush.util.console;

import com.dilatush.util.AConfig;
import com.dilatush.util.Base64Fast;
import com.dilatush.util.Networking;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

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
    /*package-private*/ final Config        config;   // our configuration..
    /*package-private*/ final AtomicInteger clients;  // count of connected clients...

    private ServerSocket                    socket;          // the server socket...


    /**
     * Create a new instance of this class using the given configuration.
     *
     * @param _config The {@link Config} configuration for this console server.
     */
    public ConsoleServer( final Config _config ) {

        // fail fast if our configuration is missing...
        if( isNull( _config ) )
            throw new IllegalArgumentException( "Missing configuration" );

        // setup...
        config  = _config;
        clients = new AtomicInteger( 0 );

        // start up our server thread...
        new ServerThread();
    }


    /**
     * Implements a simple server socket listener, waiting for TCP connections that are (we hope!) from console clients.
     */
    private class ServerThread extends Thread {

        /**
         * Create a new instance of this class.
         */
        private ServerThread() {
            setName( "Console Listener" );
            setDaemon( true );
            start();
        }


        /**
         * Run the server socket listener.  The only exit is from an interrupt.
         */
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


    /**
     * The configuration class for {@link ConsoleServer}.  This class may be constructed directly, but is designed to be built through a JavaScript
     * configuration file (see {@link AConfig}.
     */
    public static class Config extends AConfig {

        /**
         * The maximum number of clients allowed to be connected simultaneously.
         */
        public int                 maxClients = 1;      // the maximum number of clients allowed simultaneously...

        /**
         * The name of this console server.
         */
        public String              name;            // the name of this console server...

        /**
         * The base64-encoded shared secret for this console server.  This must be a 16 byte (128 bit) value.
         */
        public String              key;             // the base64 encoded shared secret for this console server...

        /**
         * The TCP port to listen on for client connections.
         */
        public int                 port;            // the port to listen for client connections on...

        /**
         * The IP address of the network interface to listen on (i.e., to bind to).  If {@code null}, listens on all network interfaces.
         */
        public InetAddress         bindTo = null;          // the InetAddress of the network interface to listen on; default is all interfaces...

        /**
         * The map of console name to the fully qualified class name for the console provider of that name.
         */
        public Map<String, String> providers = new HashMap<>();       // map of console names to the fully qualified class names of the console provider of that name...


        /**
         * Verify the validity of this object.  Each error found adds an explanatory message to the given list of messages.
         *
         * @param _messages The list of messages explaining the errors found.
         */
        @Override
        public void verify( final List<String> _messages ) {
            validate( () -> maxClients > 0, _messages, "maxClients must be greater than zero" );
            validate( () -> !isEmpty( name ), _messages, "name must be present and not empty" );
            validate( this::validateKey, _messages, "key must be 22 characters of valid base64" );
            validate( () -> (port >= 1024) && (port <= 65535), _messages, "port must be in the range [1024..65535" );
            validateProviders( _messages );
        }


        /**
         * Verifies that the provider map exists, and that each value is the fully qualified class name of a class that extends
         * {@link ConsoleProvider} that has a public no-args constructor.
         *
         * @param _messages The list of error messages; if the provider map is invalid, this method adds error messages to it.
         */
        private void validateProviders( final List<String> _messages ) {

            // if we have no map, we're most definitely invalid...
            if( isNull( providers ) ) {
                validate( () -> false, _messages, "providers must be initialized" );
                return;
            }

            // iterate over all the configured providers...
            for( Map.Entry<String,String> provider : providers.entrySet() ) {

                // if the name is empty, problem...
                if( isEmpty( provider.getKey() ) ) {
                    validate( () -> false, _messages, "name of provider may not be the empty string" );
                }

                // if the configured class does not exist and is not the right type, problem...
                String klassName = null;
                try {
                    klassName = provider.getValue();
                    Class<?> klass = Class.forName( klassName );

                    // make sure it subclasses ConsoleProvider...
                    if( !ConsoleProvider.class.isAssignableFrom( klass ) )
                        validate( () -> false, _messages, "class " + klassName + " is not a console provider" );

                    // get the no-args constructor for the console provider class...
                    Constructor<?> ctor = klass.getConstructor();
                }
                    catch( ClassNotFoundException _e ) {
                        validate( () -> false, _messages, "class " + klassName + " does not exist" );
                }
                catch( NoSuchMethodException _e ) {
                    validate( () -> false, _messages, "class " + klassName + " does not have a public no-args constructor" );
                }
            }
        }


        /**
         * Return {@code true} if the key (a string) is valid base64 that decodes to 16 bytes.
         *
         * @return {@code true} if the key (a string) is valid base64 that decodes to 16 bytes
         */
        private boolean validateKey() {

            // if we have no string, we're definitely not valid...
            if( isEmpty( key ) )
                return false;

            // see if the string decodes to 16 bytes; if so, we're golden, otherwise, not so much...
            try {
                byte[] test = Base64Fast.decodeBytes( key );
                return test.length == 16;
            }

            // we'll get here if the string is not valid base64...
            catch( Exception _e ) {
                return false;
            }
        }
    }
}
