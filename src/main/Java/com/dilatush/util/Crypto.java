package com.dilatush.util;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.*;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Sockets.readBytes;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Static container class for cryptographic (and related) methods.  Note that this class <i>does</i> maintain a bit of state, in static fields.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Crypto {

    public static final long   SECURE_RANDOM_REFRESH_INTERVAL    = 24 * 60 * 60 * 1000;  // interval between reseeding our SecureRandom instance...
    public static final String SECURE_RANDOM_FALLBACK_ALGORITHM  = "SHA1PRNG";
    public static final String SECURE_RANDOM_FALLBACK_PROVIDER   = "SUN";


    private static SecureRandom random;   // our one and only SecureRandom instance...
    private static long nextRefresh;      // the system time the next SecureRandom refresh is due...


    /**
     * Returns an instance of {@link SecureRandom} that is intended as the only instance within a given process.  The instance return is refreshed
     * (by recreating and reseeding) periodically (see {@link #SECURE_RANDOM_REFRESH_INTERVAL}).
     *
     * @return the {@link SecureRandom} instance
     */
    public static SecureRandom getSecureRandom() {

        // if we've already constructed and it's not time for a refresh, just give it up...
        if( !isNull( random ) && (System.currentTimeMillis() <= nextRefresh) )
            return random;

        // otherwise, we need to get an instance...
        // first we try to get a strong instance via the strong instance getter...
        try {
            random = SecureRandom.getInstanceStrong();
        }
        catch( Exception _e ) {

            try {
                // if we get here, then we're stuck with an environment that doesn't have a strong instance, so we try for a known good one...
                random = SecureRandom.getInstance( SECURE_RANDOM_FALLBACK_ALGORITHM, SECURE_RANDOM_FALLBACK_PROVIDER );
            }
            catch( Exception _f ) {

                // if we get here, then we can't even get a known good algorithm, so we're stuck with whatever the default is...
                // we don't know how it was seeded, so we'll do it our ownself...
                Runtime runtime = Runtime.getRuntime();
                ByteBuffer buffer = ByteBuffer.allocate( 64 );
                buffer.putLong( System.nanoTime()                 );  // 8 bytes
                buffer.putLong( System.currentTimeMillis()        );  // 8 bytes
                buffer.putInt(  System.getenv().hashCode()        );  // 4 bytes
                buffer.putInt(  System.getProperties().hashCode() );  // 4 bytes
                buffer.putLong( runtime.freeMemory()              );  // 8 bytes
                buffer.putLong( runtime.maxMemory()               );  // 8 bytes
                buffer.putLong( runtime.totalMemory()             );  // 8 bytes
                buffer.putInt(  runtime.availableProcessors()     );  // 4 bytes
                buffer.putInt(  runtime.hashCode()                );  // 4 bytes
                buffer.putLong( System.nanoTime()                 );  // 8 bytes
                random = new SecureRandom( buffer.array() );
            }
        }

        // so we've got an instance here, seeded as best we know how...
        // we grab some bytes, even if we don't know why, as recommended by several online sources...
        int numBytes = random.nextInt( 100 ) + 10;  // get an integer in [10..109]...
        byte[] bytes = new byte[numBytes];
        random.nextBytes( bytes );

        // record when we did all this...
        nextRefresh = System.currentTimeMillis() + SECURE_RANDOM_REFRESH_INTERVAL;

        // now give it up to the caller...
        return random;
    }


    /**
     * Returns an instance of {@link CipherOutputStream} that will encrypt any bytes written to it using the given {@link Key}, sending them to the
     * output stream of the given {@link Socket} instance, which must be connected.  The encryption is done with the AES cipher using a 128-bit key.
     * This method generates a random 128 bit initialization vector (IV), and writes it to the socket's output stream before any encrypted bytes are
     * sent; the other side of the connection must receive and use this IV (along with a matching key) in order to decrypt the succeeding encrypted
     * bytes.  This is precisely what {@link #getSocketInputStream_AES_128_CTR(Socket, Key)} does.
     *
     * @param _socket The connected socket to make an encrypted output stream for.
     * @param _key The valid AES 128 bit key to use.
     * @return the created and initialized {@link CipherOutputStream} instance
     * @throws IOException on any I/O problem
     */
    public static CipherOutputStream getSocketOutputStream_AES_128_CTR( final Socket _socket, final Key _key ) throws IOException {

        // fail fast if our arguments are bad...
        if( isNull( _socket, _key ) )
            throw new IllegalArgumentException( "Socket or key is missing" );

        // make sure we've got the right kind of key...
        if( !isKey_AES_128( _key ) )
            throw new IllegalArgumentException( "Wrong type of key: " + _key.getAlgorithm() );

        // make sure our socket connected and the output stream not shutdown...
        if( !_socket.isConnected() || _socket.isOutputShutdown() )
            throw new IOException( "Socket is not connected or output is shutdown" );

        // generate our IV...
        IvParameterSpec iv = makeRandomIV( 16 );

        // get our cipher...
        Cipher cipher;
        try {
            cipher = Cipher.getInstance( "AES/CTR/NoPadding" );
            cipher.init( Cipher.ENCRYPT_MODE, _key, iv );
        }
        catch( NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException _e ) {

            // if we have any of these problems, something is seriously wrong...
            throw new IllegalStateException( "Unexpected cipher problem: " + _e.getMessage(), _e );
        }

        // send the IV down the wire...
        OutputStream os = _socket.getOutputStream();
        os.write( iv.getIV() );

        // wrap the socket's output stream and our cipher in a CipherOutputStream and return it...
        return new CipherOutputStream( os, cipher );
    }


    /**
     * Returns an instance of {@link CipherInputStream} that will decrypt any bytes read from it using the given {@link Key}, getting the encrypted
     * bytes from the input stream of the given {@link Socket} instance, which must be connected.  The encryption is done with the AES cipher using a
     * 128-bit key. This method reads the 128 bit initialization vector (IV) from the socket's input stream before any bytes are decrypted; the other
     * side of the connection must send and use this IV (along with a matching key) in order to encrypt the bytes the cipher input stream returned
     * by this method will decrypt.  This is precisely what {@link #getSocketOutputStream_AES_128_CTR(Socket, Key)} does.
     *
     * @param _socket The connected socket to make an encrypted input stream for.
     * @param _key The valid AES 128 bit key to use.
     * @return the created and initialized {@link CipherInputStream} instance
     * @throws IOException on any I/O problem
     */
    public static CipherInputStream getSocketInputStream_AES_128_CTR( final Socket _socket, final Key _key ) throws IOException {

        // fail fast if our arguments are bad...
        if( isNull( _socket, _key ) )
            throw new IllegalArgumentException( "Socket or key is missing" );

        // make sure we've got the right kind of key...
        if( !isKey_AES_128( _key ) )
            throw new IllegalArgumentException( "Wrong type of key: " + _key.getAlgorithm() );

        // make sure our socket connected and the output stream not shutdown...
        if( !_socket.isConnected() || _socket.isInputShutdown() )
            throw new IOException( "Socket is not connected or input is shutdown" );

        // read the IV from the input stream...
        IvParameterSpec iv = new IvParameterSpec( readBytes( _socket, 16 ) );

        // get our cipher...
        Cipher cipher;
        try {
            cipher = Cipher.getInstance( "AES/CTR/NoPadding" );
            cipher.init( Cipher.DECRYPT_MODE, _key, iv );
        }
        catch( NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException _e ) {

            // if we have any of these problems, something is seriously wrong...
            throw new IllegalStateException( "Unexpected cipher problem: " + _e.getMessage(), _e );
        }

        // wrap the socket's input stream and our cipher in a CipherInputStream and return it...
        return new CipherInputStream( _socket.getInputStream(), cipher );
    }


    /**
     * Returns an initialization vector (IV) as an instance of {@link IvParameterSpec} with the given size (the number of bytes).
     *
     * @param _size The number of bytes in the initialization vector.
     * @return the generated initialization vector, an instance of {@link IvParameterSpec}
     */
    public static IvParameterSpec makeRandomIV( final int _size ) {

        // get the required number of random bytes...
        byte[] bytes = new byte[_size];
        getSecureRandom().nextBytes( bytes );

        // and turn it into an IV parameter spec...
        return new IvParameterSpec( bytes );
    }


    /**
     * Returns {@code true} if the given key is a 128 bit AES key.
     *
     * @param _key The {@link Key} instance to check.
     * @return {@code true} if the given key is a 128 bit AES key
     */
    public static boolean isKey_AES_128( final Key _key ) {
        return (_key != null) && ("AES".equals( _key.getAlgorithm())) && (_key.getEncoded().length == 16);
    }


    /**
     * Creates and returns a 128 bit AES key using the given key bytes as the key material.  Exactly 16 bytes (128 bits) must be provided.
     *
     * @param _keyBytes The 16 bytes (128 bits) of key material.
     * @return the 128 bit AES {@link Key}
     */
    public static Key getKey_AES_128( final byte[] _keyBytes ) {

        // fail fast if our argument is obviously bogus...
        if( (_keyBytes == null) || (_keyBytes.length != 16) )
            throw new IllegalArgumentException( "Invalid AES key bytes (null or not 16 bytes long)" );

        return new SecretKeySpec( _keyBytes, "AES" );
    }


    /**
     * Creates and returns a 128 bit AES key using the given base64-encoded key bytes as the key material.  Exactly 22 characters (which decode to
     * 16 bytes (128 bits)) must be provided.
     *
     * @param _base64KeyBytes The 22 characters of base64-encoded key material.
     * @return  the 128 bit AES {@link Key}
     */
    @SuppressWarnings( "unused" )
    public static Key getKey_AES_128( final String _base64KeyBytes ) {

        // fail fast if our argument is obviously bogus...
        if( isEmpty( _base64KeyBytes ) || (_base64KeyBytes.length() != 22) )
            throw new IllegalArgumentException( "Invalid AES key base64 string: " + _base64KeyBytes );

        return getKey_AES_128( Base64Fast.decodeBytes( _base64KeyBytes ) );
    }
}
