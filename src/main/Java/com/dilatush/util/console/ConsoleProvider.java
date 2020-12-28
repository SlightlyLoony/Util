package com.dilatush.util.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implemented by classes providing consoles.  In general, these classes read commands from an input stream, and provide response through an
 * output stream.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ConsoleProvider {

    void setStreams( final InputStream _inputStream, final OutputStream _outputStream );

    void run() throws IOException;
}
