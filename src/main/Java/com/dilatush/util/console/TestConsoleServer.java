package com.dilatush.util.console;

import java.util.HashMap;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestConsoleServer {

    private static Logger LOGGER;

    public static void main( final String[] _args ) {

        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );
        LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getSimpleName() );


        try {
            ConsoleServer.Config config = new ConsoleServer.Config();
            config.port = 8217;
            config.bindTo = null;
            config.maxClients = 1;
            config.name = "test";
            config.key = "abcdefghijklmnopqrstuA";
            config.providers = new HashMap<>();
            ConsoleServer server = new ConsoleServer( config );
            server.start();


            while( true ) {
                sleep( 1000 );
            }
        }
        catch( Exception _e ) {
            System.out.println( _e.getMessage() );
            _e.printStackTrace();
        }
    }
}
