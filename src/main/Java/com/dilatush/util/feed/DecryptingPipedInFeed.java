package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.dilatush.util.General.isNull;

/**
 * An {@link InFeed} that provides data read from another {@link InFeed} and decrypted.
 */
public class DecryptingPipedInFeed implements InFeed {


    private final InFeed inFeed;
    private final Cipher cipher;


    /**
     * Create a new instance of {@link DecryptingPipedInFeed} that uses the given {@link Cipher} to decrypt bytes read from the given {@link InFeed}.  The cipher must be fully
     * initialized, must use CFB8 or OFB8 mode, no padding, and be in decrypt mode.
     *
     * @param _inFeed the {@link InFeed} to read encrypted bytes from.
     * @param _cipher the fully initialized {@link Cipher}
     */
    // TODO: change this to actually get the Cipher instance, to ensure the correct initialization...
    public DecryptingPipedInFeed( final InFeed _inFeed, final Cipher _cipher ) {

        // sanity checks...
        if( isNull( _inFeed, _cipher ) ) throw new IllegalArgumentException( "_inFeed or _cipher is null" );
        var algo = _cipher.getAlgorithm();
        if( !algo.contains( "/CFB8/" ) && !algo.contains( "/OFB8/" ) ) throw new IllegalArgumentException( "Cipher must use CFB8 or OFB8 mode" );
        //if( _cipher.)

        inFeed = _inFeed;
        cipher = _cipher;
    }

    public static void main( String[] args ) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {

        var decipher = getDecryptCipher();
        var encipher = getEncryptCipher();

        var alg = decipher.getAlgorithm();
        var ap = decipher.getParameters();

        var ob = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 };

        var ct1 = new byte[10];
        var c1 = encipher.update( ob, 0, 10, ct1 );
        var db1 = new byte[10];
        var c2 = decipher.doFinal( ct1, 0, 10, db1 );

        var ct2 = new byte[5];
        var c3 = encipher.update( ob, 0, 5, ct2 );
        var db2 = new byte[5];
        var c4 = decipher.doFinal( ct1, 0, 5, db2 );

        decipher.hashCode();
    }


    private static Cipher getDecryptCipher() {

        byte[] keyBytes = new byte[32];
        var key = new SecretKeySpec( keyBytes, "AES" );

        var iv = new IvParameterSpec( new byte[16] );


        Cipher cipher;
        try {
            cipher = Cipher.getInstance( "AES/CFB8/NoPadding" );
            cipher.init( Cipher.DECRYPT_MODE, key, iv );
        }
        catch( NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException _e ) {

            // if we have any of these problems, something is seriously wrong...
            throw new IllegalStateException( "Unexpected cipher problem: " + _e.getMessage(), _e );
        }

        return cipher;
    }


    private static Cipher getEncryptCipher() {

        byte[] keyBytes = new byte[32];
        var key = new SecretKeySpec( keyBytes, "AES" );

        var iv = new IvParameterSpec( new byte[16] );


        Cipher cipher;
        try {
            cipher = Cipher.getInstance( "AES/CFB8/NoPadding" );
            cipher.init( Cipher.ENCRYPT_MODE, key, iv );
        }
        catch( NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException _e ) {

            // if we have any of these problems, something is seriously wrong...
            throw new IllegalStateException( "Unexpected cipher problem: " + _e.getMessage(), _e );
        }

        return cipher;
    }


    /**
     * <p>Initiates an asynchronous (non-blocking) operation to read between the given {@code _minBytes} and {@code _maxBytes} bytes from this feed.  The data is read into a new
     * {@link ByteBuffer}, which is the info in the {@link Outcome} if the outcome was ok.  The {@code _handler} is called when the read operation completes, whether that
     * operation completed normally, was terminated because of an error, or was canceled.</p>
     * <p>This method has no default implementation; it <i>must</i> be provided by every {@link InFeed} implementation.</p>
     * <p>Whether the handler is called in the same thread this method was called in, or in a separate thread (or even either, depending on conditions), is
     * implementation-dependent and should be documented for each implementation.</p>
     *
     * @param _minBytes The minimum number of bytes that must be read for this read operation to be considered complete.  The value must be in the range [1..{@code _maxBytes}].
     * @param _maxBytes The maximum number of bytes that may be read in this read operation.  The value must be in the range [{@code _minBytes}..65536].
     * @param _handler  This handler is called with the outcome of the read operation, when the read operation completes, whether normally, terminated by an error, or
     *                  canceled.  If the outcome is ok, then the operation completed normally and the info contains the read buffer with the bytes read from this feed.  If not ok,
     *                  then there is an explanatory message and possibly the exception that caused the problem.
     * @throws IllegalArgumentException if the {@code _handler} is {@code null}.
     * @throws IllegalStateException    if a read operation is already in progress.
     */
    @Override
    public void read( final int _minBytes, final int _maxBytes, final OnReadComplete _handler ) {

    }


    /**
     * Closes this input feed and releases any system resources associated with the feed.
     */
    @Override
    public void close() {

    }
}
