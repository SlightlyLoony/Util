package com.dilatush.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Contains methods to work with streams.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Streams {

    private static final int DEFAULT_INPUT_BYTE_BUFFER_SIZE = 1024;


    /**
     * Reads all the bytes in the specified input stream into a byte array.  The size of the byte array returned is equal to the number of bytes
     * actually read.
     *
     * @param _is the input stream to read bytes from
     * @return the byte array containing the bytes read from the input stream
     * @throws IOException on any I/O error
     */
    public static byte[] toByteArray( final InputStream _is ) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream( DEFAULT_INPUT_BYTE_BUFFER_SIZE );
        while( _is.available() > 0 ) {
            byte[] buff = new byte[_is.available()];
            int bytesRead = _is.read( buff );
            baos.write( buff, 0, bytesRead );
        }
        return baos.toByteArray();
    }


    /**
     * Reads all the bytes in the specified input stream into a {@link ByteBuffer}.
     *
     * @param _is the input stream to read bytes from
     * @return the ByteBuffer containing the bytes read from the input stream
     * @throws IOException on any I/O error
     */
    public static ByteBuffer toByteBuffer( final InputStream _is ) throws IOException {
        return ByteBuffer.wrap( toByteArray( _is ) );
    }



    /**
     * Reads all the bytes in the specified input stream into a string, decoding the bytes with the specified {@link Charset}.
     *
     * @param _is the input stream to read the bytes from
     * @param _charset the character set to decode the bytes with
     * @return the string containing the characters read from the input stream
     * @throws IOException on any I/O error
     */
    public static String toString( final InputStream _is, final Charset _charset ) throws IOException {
        return new String( toByteArray( _is ), _charset );
    }


    /**
     * Reads all the bytes in the specified input stream into a string, decoding the bytes as UTF-8.  This is exactly the same as invoking
     * {@link #toString(InputStream, Charset)} with {@link java.nio.charset.StandardCharsets#UTF_8}.
     *
     * @param _is the input stream to read the bytes from
     * @return the string containing the characters read from the input stream
     * @throws IOException on any I/O error
     */
    public static String toUTF8String( final InputStream _is ) throws IOException {
        return toString( _is, StandardCharsets.UTF_8 );
    }
}
