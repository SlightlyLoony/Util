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

    private BufferedWriter writer;


    /**
     * Create a new instance of this class with the given socket, reader, and writer.  The reader is based on a decrypting stream, already
     * initialized.  The writer is based on an encrypting stream, also already initialized.
     *
     * @param _socket The socket for the network connection to the console client.
     * @param _reader The reader to read text from the console client.
     * @param _writer The writer to write text to the console client.
     */
    @Override
    public void run( final Socket _socket, final BufferedReader _reader, final BufferedWriter _writer ) {

        socket = _socket;
        writer = _writer;

        // initialize the provider...
        init();

        // read input from the network...
        try {
            while( !Thread.currentThread().isInterrupted() ) {
                String line = _reader.readLine();
                if( isNull( line ) )
                    break;
                onLine( line );
            }
        }
        catch( IOException _e ) {

            // close the socket, which will provoke all sorts of hate and discontent on both ends of the connection...
            close( socket );
        }
    }


    /**
     * Write the given signoff message to the console client, then close the connection to the client.  This should be called by concrete subclasses
     * to gracefully terminate the connection.
     *
     * @param _signoffMsg The signoff message to send to the console client before closing the connection.
     */
    protected void exit( final String _signoffMsg ) {
        writeLine( _signoffMsg );
        close( socket );
    }


    /**
     * Write the given string to the console client, followed by a newline, the entirety to be flushed for immediate transmission.
     *
     * @param _line The string to send to the console client.
     */
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


    /**
     * Returns the name of this console provider; it must be implemented by concrete subclasses.
     *
     * @return the name of this console provider
     */
    protected abstract String getName();


    /**
     * Called upon receipt of a line of text from the console client; must be implemented by concrete subclasses to process text from the console
     * client.
     *
     * @param _line The line of text received from the console client.
     */
    protected abstract void onLine( final String _line );


    /**
     * Called when the provider is being started; must be implemented by concrete subclasses to do whatever initialization they may need.
     * Conventionally this initialization includes sending a signon banner to the console client.
     */
    protected abstract void init();
}
