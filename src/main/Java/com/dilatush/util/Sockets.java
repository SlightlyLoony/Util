package com.dilatush.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for utility functions related to {@link Socket}s.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Sockets {


    /**
     * Reads and returns the given number of bytes from the given socket.
     *
     * @param _socket The socket to read from.
     * @param _numBytes The number of bytes to read.
     * @return the bytes read
     * @throws IOException on any I/O problem...
     */
    public static byte[] readBytes( final Socket _socket, final int _numBytes ) throws IOException {

        // fail fast if our arguments are bad...
        if( isNull( _socket ) )
            throw new IllegalArgumentException( "Socket is missing" );
        if( _numBytes < 1 )
            throw new IllegalArgumentException( "Invalid number of bytes to read: " + _numBytes );

        InputStream is = _socket.getInputStream();
        byte[] result = new byte[ _numBytes ];
        int gotBytes = is.read( result, 0, _numBytes );  // this will block until all the bytes are read...
        if( gotBytes < 1 )
            throw new IOException( "Could not read the requested " + _numBytes + " bytes" );
        return result;
    }


    /**
     * Read and return bytes from the input stream of the given {@link Socket} until the given terminator byte is received. If the specified buffer
     * size is exceeded, this method will throw an {@link IOException}.  Note that the terminator byte is <i>not</i> returned in the result.
     *
     * @param _socket The socket to read bytes from.
     * @param _bufferSize The maximum number of bytes that can be read (not including the terminator) and returned.
     * @param _terminator The byte value that will terminate the reading.
     * @return the bytes read, not including the terminator
     * @throws IOException on any I/O problem or if buffer size is exceeded
     */
    public static byte[] readUntil( final Socket _socket, final int _bufferSize, final byte _terminator ) throws IOException {

        // fail fast if our arguments are bad...
        if( isNull( _socket ) )
            throw new IllegalArgumentException( "Socket is missing" );
        if( _bufferSize < 1 )
            throw new IllegalArgumentException( "Invalid buffer size: " + _bufferSize );

        InputStream is = _socket.getInputStream();
        byte[] buffer = new byte[ _bufferSize ];
        int i = 0;
        while( true ) {
            int read = is.read();
            if( read < 0 )
                throw new IOException( "Unexpected end of socket input stream" );
            if( (byte) read == _terminator )
                return Arrays.copyOfRange( buffer, 0, i);
            if( i >= buffer.length )
                throw new IOException( "Read buffer size exceeded" );
            buffer[i++] = (byte) read;
        }
    }


    /**
     * Reads and returns a line of text from the input stream of the given {@link Socket}.  The line is terminated by reading a newline ('\n') from
     * the input stream; the newline is not returned in the results.  The given buffer size determines the maximum number of bytes that can be read
     * before the line terminator is read.  Note that the number of bytes <i>may</i> not be the same as the number of characters, depending on the
     * character encoding used.  The given {@link Charset} determines the encoding that translates the bytes to characters.  This method will throw
     * an {@link IOException} if the buffer size is exceeded, or any problem occurs while reading bytes.
     *
     * @param _socket  The socket to read bytes from.
     * @param _bufferSize The maximum number of bytes that can be read (not including the terminator), translated, and returned.
     * @param _charset The character set to use when translating from bytes to characters.
     * @return the line of text read
     * @throws IOException on any I/O problem, or if the buffer size is exceeded
     */
    public static String readLine( final Socket _socket, final int _bufferSize, final Charset _charset ) throws IOException {
        return new String( readUntil( _socket, _bufferSize, (byte)'\n' ), _charset );
    }


    /**
     * Reads and returns a line of text from the input stream of the given {@link Socket}.  The line is terminated by reading a newline ('\n') from
     * the input stream; the newline is not returned in the results.  The given buffer size determines the maximum number of bytes that can be read
     * before the line terminator is read.  Note that the number of bytes <i>may</i> not be the same as the number of characters.  The UTF-8
     * {@link Charset} is used to determine the encoding that translates the bytes to characters.  This method will throw an {@link IOException} if
     * the buffer size is exceeded, or any problem occurs while reading bytes.  The {@code readLine( _socket, _bufferSize )} method
     * has exactly the same effect as {@code readLine( _socket, _bufferSize, StandardCharsets.UTF_8 )}.
     *
     * @param _socket  The socket to read bytes from.
     * @param _bufferSize The maximum number of bytes that can be read (not including the terminator), translated, and returned.
     * @return the line of text read
     * @throws IOException on any I/O problem, or if the buffer size is exceeded
     */
    public static String readLine( final Socket _socket, final int _bufferSize ) throws IOException {
        return readLine( _socket, _bufferSize, StandardCharsets.UTF_8 );
    }


    /**
     * Reads and returns a line of text from the input stream of the given {@link Socket}.  The line is terminated by reading a newline ('\n') from
     * the input stream; the newline is not returned in the results.  A maximum of 500 bytes that can be read
     * before the line terminator is read.  Note that the number of bytes <i>may</i> not be the same as the number of characters.  The UTF-8
     * {@link Charset} is used to determine the encoding that translates the bytes to characters.  This method will throw an {@link IOException} if
     * the buffer size is exceeded, or any problem occurs while reading bytes.  The {@code readLine( _socket )} method
     * has exactly the same effect as {@code readLine( _socket, 500, StandardCharsets.UTF_8 )}.
     *
     * @param _socket  The socket to read bytes from.
     * @return the line of text read
     * @throws IOException on any I/O problem, or if the buffer size is exceeded
     */
    public static String readLine( final Socket _socket ) throws IOException {
        return new String( readUntil( _socket, 500, (byte)'\n' ), StandardCharsets.UTF_8 );
    }


    /**
     * Closes the given socket and absorbs any exception.  If the given socket is {@code null} or is already closed, this method does nothing.
     *
     * @param _socket The socket to close.
     */
    public static void close( final Socket _socket ) {
        if( (_socket != null) && !_socket.isClosed() ) {
            try {
                _socket.close();
            }
            catch( IOException _f ) {
                /* naught to do */
            }
        }
    }
}
