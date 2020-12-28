package com.dilatush.util.console;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implemented by classes providing encryption and decryption for console client connections.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ConsoleEncryptor {

    InputStream getDecryptedInputStream( final InputStream _encryptedInputStream );

    OutputStream getDecryptedOutputStream( final OutputStream _encryptedOutputStream );
}
