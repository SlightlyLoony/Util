package com.dilatush.util.test;

import com.dilatush.util.AConfig;
import com.dilatush.util.console.ConsoleServer;

import java.util.HashMap;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestTest {

    private static Logger LOGGER;

    public static void main( final String[] _args ) throws InterruptedException {

        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );
        LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getSimpleName() );

        // get test configuration...
        AConfig.InitResult ir = AConfig.init( TestManager.Config.class, "TestTest.js" );
        if( !ir.valid )
            throw new IllegalStateException( "Configuration problem: " + ir.message );

        TestManager.configure( (TestManager.Config) ir.config );
        TestManager mgr = TestManager.getInstance();

        TestEnabler te1 = mgr.register( "te1" );
        TestEnabler te2 = mgr.register( "te2" );

        System.out.println( "Count: " + te2.getAsLong( "count" ) );


        try {
            ConsoleServer.Config config = new ConsoleServer.Config();
            config.port = 8217;
            config.bindTo = null;
            config.maxClients = 1;
            config.name = "test";
            config.key = "abcdefghijklmnopqrstuA";
            config.providers = new HashMap<>();
            config.providers.put( "test", "com.dilatush.util.test.TestConsoleProvider" );
            ConsoleServer server = new ConsoleServer( config );


            while( true ) {
                sleep( 1000 );
            }
        }
        catch( Exception _e ) {
            System.out.println( _e.getMessage() );
            _e.printStackTrace();
        }


        while( true ) {
            sleep( 500 );

            if( te2.isEnabled() ) {
                System.out.println( "te2 enabled" );
            } else {
                System.out.println( "te2 disabled" );
            }

            if( te1.isEnabled() )
                System.out.println( "te1 enabled " );
        }
    }
}
