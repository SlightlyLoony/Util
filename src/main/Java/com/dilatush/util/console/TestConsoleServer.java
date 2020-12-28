package com.dilatush.util.console;

import static java.lang.Thread.sleep;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestConsoleServer {

    public static void main( final String[] _args ) {

        try {
            ConsoleServer.Config config = new ConsoleServer.Config();
            config.port = 8217;
            config.bindTo = null;
            config.authenticator = new NullAuthenticator();
            config.encryptor = new NullConsoleEncryptor();
            config.maxClients = 1;
            config.provider = new EchoConsoleProvider();
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
