package com.dilatush.util.console;

import java.util.HashMap;
import java.util.List;
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
            config.providers.put( "test", "com.dilatush.util.console.TestConsoleServer$TestConsole" );
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


    public static class TestConsole extends CommandProcessorConsoleProvider {


        public TestConsole() {
            super(
                    "Test console to see if any of this command line processor stuff actually, like, works."
            );
        }


        /**
         * Returns the name of this console provider; it must be implemented by concrete subclasses.
         *
         * @return the name of this console provider
         */
        @Override
        protected String getName() {
            return "test";
        }


        /**
         * Called when the provider is being started; must be implemented by concrete subclasses to do whatever initialization they may need.
         * Conventionally this initialization includes sending a signon banner to the console client.
         */
        @Override
        protected void init() {
            writeLine( "Hello from the test console provider..." );
            addCommandProcessor( new TurkeyProcessor() );
            addCommandProcessor( new AddProcessor() );
            finish();
        }


        private class TurkeyProcessor extends CommandProcessor {

            public TurkeyProcessor() {
                super( "turkey", "gobble all but the last word of the line", "turkey <stuff>\ndo unlikely things" );
            }


            @Override
            protected void onCommandLine( final String _line, final List<String> _words ) {
                writeLine( "Gobble, gobble: " + _words.get( _words.size() - 1 ) );
            }
        }


        private class AddProcessor extends CommandProcessor {

            public AddProcessor() {
                super( "add", "add the arguments", "add <number>+\nAdd all the given numbers together" );
            }


            @Override
            protected void onCommandLine( final String _line, final List<String> _words ) {

                // add all the given numbers together...
                double sum = 0;
                for( String word : _words ) {
                    sum += Double.parseDouble( word );
                }
                writeLine( "sum: " + sum );
            }
        }
    }
}
