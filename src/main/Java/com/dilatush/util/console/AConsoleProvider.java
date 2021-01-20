package com.dilatush.util.console;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Sockets.close;

/**
 * An abstract base class for {@link ConsoleProvider} implementations.  It creates a thread to read the input stream, calling the
 * {@link #onLine(String)} method for each line of text received.  It also provides various methods for the concrete provider subclass to write
 * text to the output.  Network-related exceptions are handled here, so that concrete provider subclasses don't need to.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AConsoleProvider implements ConsoleProvider {

    private Socket socket;

    private BufferedReader reader;
    private BufferedWriter writer;


    @Override
    public void run( final Socket _socket, final BufferedReader _reader, final BufferedWriter _writer ) {

        socket = _socket;
        reader = _reader;
        writer = _writer;

        // initialize the provider...
        init();

        // read input from the network...
        try {
            while( !Thread.currentThread().isInterrupted() ) {
                String line = reader.readLine();
                if( isNull( line ) )
                    break;
                onLine( line );
            }
        }
        catch( IOException _e ) {

            // close the socket, which will provoke all sorts of hate and discontent...
            close( socket );
        }
    }


    protected void exit( final String _signoffMsg ) {
        writeLine( _signoffMsg );
        close( socket );
    }


    protected void writeLine( final String _line ) {

        try {
            writer.write( _line );
            writer.newLine();
            writer.flush();
        }
        catch( IOException _e ) {

            // close the socket, which will provoke all sorts of hate and discontent...
            close( socket );
        }
    }


    protected abstract String getName();

    protected abstract void onLine( final String _line );

    protected abstract void init();
}
