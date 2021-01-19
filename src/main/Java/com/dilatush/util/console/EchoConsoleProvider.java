package com.dilatush.util.console;

/**
 * A simple console provider (for testing purposes) whose output simply echoes its input.  It stops running when it detects the word "quit" at
 * the beginning of a line.  This provider is available to all console server implementations.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EchoConsoleProvider extends AConsoleProvider {


    @Override
    protected String getName() {
        return "echo";
    }


    @Override
    protected void onLine( final String _line ) {
        writeLine( _line );
    }


    @Override
    protected void init() {
        writeLine( "ECHO console provider, at your service!" );
    }
}
