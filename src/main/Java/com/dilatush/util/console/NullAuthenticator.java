package com.dilatush.util.console;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides a simple authenticator that always authenticates.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class NullAuthenticator implements ConsoleAuthenticator {


    @Override
    public boolean authenticate( final InputStream _inputStream, final OutputStream _outputStream ) {
        return true;
    }
}
