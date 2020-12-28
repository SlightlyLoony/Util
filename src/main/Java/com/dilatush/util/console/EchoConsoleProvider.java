package com.dilatush.util.console;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * A simple console provider (for testing purposes) whose output simply echoes its input.  It stops running when it detects the word "quit" at
 * the beginning of a line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EchoConsoleProvider implements ConsoleProvider {

    private InputStream  is;
    private OutputStream os;


    @Override
    public void setStreams( final InputStream _inputStream, final OutputStream _outputStream ) {
        is = _inputStream;
        os = _outputStream;
    }


    @Override
    public void run() throws IOException {

        BufferedReader reader = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) );
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( os, StandardCharsets.UTF_8 ) );

        IOException exception = null;

        try {

            boolean done = false;
            while( !done ) {

                String line = reader.readLine();
                done = line.startsWith( "quit" );
                writer.write( line );
                writer.newLine();
                writer.flush();
            }
        }
        catch( IOException _e ) {
            exception = _e;
        }

        reader.close();
        writer.close();

        if( exception != null ) {
            throw exception;
        }
    }
}
