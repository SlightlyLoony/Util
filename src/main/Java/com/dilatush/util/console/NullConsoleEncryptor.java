package com.dilatush.util.console;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides a simple console encryptor that does nothing at all.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class NullConsoleEncryptor implements ConsoleEncryptor {

    @Override
    public InputStream getDecryptedInputStream( final InputStream _encryptedInputStream ) {
        return _encryptedInputStream;
    }


    @Override
    public OutputStream getDecryptedOutputStream( final OutputStream _encryptedOutputStream ) {
        return _encryptedOutputStream;
    }
}
