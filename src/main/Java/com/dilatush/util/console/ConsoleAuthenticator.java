package com.dilatush.util.console;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implemented by classes providing authentication for a fresh console client connection.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ConsoleAuthenticator {

    boolean authenticate( final InputStream _inputStream, final OutputStream _outputStream );
}
