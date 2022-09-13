package com.dilatush.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.BigIntegers.*;
import static com.dilatush.util.CompositeModuloMath.CompositeIntegerModulus;
import static com.dilatush.util.CompositeModuloMath.pow;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;
import static java.math.BigInteger.ONE;

/**
 * Static container class for functions related to RSA public key encryption.  The design of these functions relies heavily on the RSA chapters of <i>Cryptography Engineering</i>.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class RSA {

    private static final Outcome.Forge<RSAKeyPair>    forgeRSAKeyPair    = new Outcome.Forge<>();
    private static final Outcome.Forge<RSAPublicKey>  forgeRSAPublicKey  = new Outcome.Forge<>();
    private static final Outcome.Forge<RSAPrivateKey> forgeRSAPrivateKey = new Outcome.Forge<>();
    private static final Outcome.Forge<byte[]>        forgeBytes         = new Outcome.Forge<>();


    /**
     * An RSA public key.
     *
     * @param n The modulus for this key.
     * @param eEncrypting The encrypting exponent for this key.
     * @param eSigning The signature verification exponent for this key.
     */
    public record RSAPublicKey( BigInteger n, BigInteger eEncrypting, BigInteger eSigning ) {

        public RSAPublicKey {

            // sanity checks...
            if( isNull( n, eEncrypting, eSigning ) ) throw new IllegalArgumentException( "n, eEncrypting, or eSigning is null" );
            if( n.compareTo( ONE ) < 0 )             throw new IllegalArgumentException( "n is less than one" );
            if( eEncrypting.compareTo( ONE ) < 0 )   throw new IllegalArgumentException( "eEncrypting is less than one" );
            if( eSigning.compareTo( ONE ) < 0 )      throw new IllegalArgumentException( "eSigning is less than one" );
        }

        /**
         * Returns a string that represents the value of this key, formatted as follows:
         * <ul>
         *     <li>The string "n:".</li>
         *     <li>The base64 value of {@code n}.</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "eE:".</li>
         *     <li>The base64 value of {@code eEncrypting}</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "eS:".</li>
         *     <li>The base64 value of {@code eSigning}.</li>
         *     <li>A terminating semicolon.</li>
         * </ul>
         * For example, if n = 1, eEncrypting = 2, and eSigning = 3, the string would be {@code n:1;eE:2;eS:3;}.
         *
         * @return A string representing the value of this key.
         */
        public String toString() {
            return    "n:"   + Base64Fast.encode( n )          + ";"
                    + "eE:" + Base64Fast.encode( eEncrypting ) + ";"
                    + "eS:" + Base64Fast.encode( eSigning )    + ";";
        }


        // pattern that parses the string representation of a public RSA key...
        private static final Pattern PARSE_PUBLIC_KEY = Pattern.compile( "n:([a-zA-Z0-9+/]+);eE:([a-zA-Z0-9+/]+);eS:([a-zA-Z0-9+/]+);" );


        /**
         * Parses a string that represents the value of a public RSA key, which must be formatted as follows:
         * <ul>
         *     <li>The string "n:".</li>
         *     <li>The base64 value of {@code n}.</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "eE:".</li>
         *     <li>The base64 value of {@code eEncrypting}</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "eS:".</li>
         *     <li>The base64 value of {@code eSigning}.</li>
         *     <li>A terminating semicolon.</li>
         * </ul>
         * For example, if n = 1, eEncrypting = 2, and eSigning = 3, the string would be {@code n:1;eE:2;eS:3;}.
         *
         * @return A string representing the value of this key.
         */
        public static Outcome<RSAPublicKey> fromString( final String _string ) {

            // sanity checks...
            if( isEmpty( _string ) ) throw new IllegalArgumentException( "_string is null or zero length" );

            // fail if we can't parse this string into fields for our three numerical values...
            Matcher matcher = PARSE_PUBLIC_KEY.matcher( _string );
            if( !matcher.matches() ) return forgeRSAPublicKey.notOk( "Not a public RSA key: " + _string );

            // it parsed ok, so get the values for our three fields...
            var n           = Base64Fast.decodeBigInteger( matcher.group( 1 ) );
            var eEncrypting = Base64Fast.decodeBigInteger( matcher.group( 2 ) );
            var eSigning    = Base64Fast.decodeBigInteger( matcher.group( 3 ) );

            // construct the public key and skedaddle...
            return forgeRSAPublicKey.ok( new RSAPublicKey( n, eEncrypting, eSigning ) );
        }


        @Override
        public boolean equals( final Object _o ) {

            if( this == _o ) return true;
            if( _o == null || getClass() != _o.getClass() ) return false;
            RSAPublicKey that = (RSAPublicKey) _o;
            return n.equals( that.n ) && eEncrypting.equals( that.eEncrypting ) && eSigning.equals( that.eSigning );
        }


        @Override
        public int hashCode() {

            return Objects.hash( n, eEncrypting, eSigning );
        }
    }


    /**
     * An RSA private key.
     *
     * @param m The composite modulus for this key.
     * @param t The totient for this key.
     * @param dEncrypting The decryption exponent for this key.
     * @param dSigning The signing exponent for this key.
     */
    public record RSAPrivateKey( CompositeIntegerModulus m, BigInteger t, BigInteger dEncrypting, BigInteger dSigning ) {


        public RSAPrivateKey {

            // sanity checks...
            if( isNull( m, t, dEncrypting, dSigning ) ) throw new IllegalArgumentException( "m, t, dEncrypting, or dSigning is null" );
            if( t.compareTo( ONE ) < 0 ) throw new IllegalArgumentException( "t is less than one" );
            if( dEncrypting.compareTo( ONE ) < 0 ) throw new IllegalArgumentException( "dEncrypting is less than one" );
            if( dSigning.compareTo( ONE ) < 0 ) throw new IllegalArgumentException( "dSigning is less than one" );
        }


        /**
         * Returns a string that represents the value of this key, formatted as follows:
         * <ul>
         *     <li>The string "p:".</li>
         *     <li>The base64 value of {@code p}.</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "q:".</li>
         *     <li>The base64 value of {@code q}.</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "dE:".</li>
         *     <li>The base64 value of {@code eEncrypting}</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "dS:".</li>
         *     <li>The base64 value of {@code eSigning}.</li>
         *     <li>A terminating semicolon.</li>
         * </ul>
         * For example, if n = 391, p = 17, q = 23, t = 88, dEncrypting = 2, and dSigning = 3, the string would be {@code p:17;q:11;dE:2;dS3;}.  Note that the string returned
         * does not contain all the values contained in this key, but rather the minimum set of values required to be able to <i>compute</i> all the values.
         *
         * @return A string representing the value of this key.
         */
        public String toString() {

            return "p:" + Base64Fast.encode( m.p() ) + ";"
                    + "q:" + Base64Fast.encode( m.q() ) + ";"
                    + "dE:" + Base64Fast.encode( dEncrypting ) + ";"
                    + "dS:" + Base64Fast.encode( dSigning ) + ";";
        }


        // pattern that parses the string representation of a public RSA key...
        private static final Pattern PARSE_PRIVATE_KEY = Pattern.compile( "p:([a-zA-Z0-9+/]+);q:([a-zA-Z0-9+/]+);dE:([a-zA-Z0-9+/]+);dS:([a-zA-Z0-9+/]+);" );


        /**
         * Parses a string that represents the value of a private RSA key, which must be formatted as follows:
         * <ul>
         *     <li>The string "p:".</li>
         *     <li>The base64 value of {@code p}.</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "q:".</li>
         *     <li>The base64 value of {@code q}.</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "dE:".</li>
         *     <li>The base64 value of {@code eEncrypting}</li>
         *     <li>A terminating semicolon (";").</li>
         *     <li>The string "dS:".</li>
         *     <li>The base64 value of {@code eSigning}.</li>
         *     <li>A terminating semicolon.</li>
         * </ul>
         * For example, if p = 1, q = 2 dEncrypting = 3, and dSigning = 4, the string would be {@code p:1;q:2;dE:3;dS:4;}.
         *
         * @return A string representing the value of this key.
         */
        public static Outcome<RSAPrivateKey> fromString( final String _string ) {

            // sanity checks...
            if( isEmpty( _string ) ) throw new IllegalArgumentException( "_string is null or zero length" );

            // fail if we can't parse this string into fields for our three numerical values...
            Matcher matcher = PARSE_PRIVATE_KEY.matcher( _string );
            if( !matcher.matches() ) return forgeRSAPrivateKey.notOk( "Not a private RSA key: " + _string );

            // it parsed ok, so get the values for our four fields...
            var p  = Base64Fast.decodeBigInteger( matcher.group( 1 ) );
            var q  = Base64Fast.decodeBigInteger( matcher.group( 2 ) );
            var dE = Base64Fast.decodeBigInteger( matcher.group( 3 ) );
            var dS = Base64Fast.decodeBigInteger( matcher.group( 4 ) );

            // compute remaining values from those parsed...
            var n = p.multiply( q );
            var m = new CompositeIntegerModulus( n, p, q );
            var t = lcm( p.subtract( ONE ), q.subtract( ONE ) );

            // construct the public key and skedaddle...
            return forgeRSAPrivateKey.ok( new RSAPrivateKey( m, t, dE, dS ) );
        }


        @Override
        public boolean equals( final Object _o ) {

            if( this == _o ) return true;
            if( _o == null || getClass() != _o.getClass() ) return false;
            RSAPrivateKey that = (RSAPrivateKey) _o;
            return m.equals( that.m ) && t.equals( that.t ) && dEncrypting.equals( that.dEncrypting ) && dSigning.equals( that.dSigning );
        }


        @Override
        public int hashCode() {

            return Objects.hash( m, t, dEncrypting, dSigning );
        }
    }


    /**
     * A complementary pair of RSA keys, one public, one private.
     *
     * @param publicKey The public key in this RSA key pair.
     * @param privateKey The private key in this RSA key pair.
     */
    public record RSAKeyPair( RSAPublicKey publicKey, RSAPrivateKey privateKey ) {}


    /*----------------------------------------------------------------------------------------------------------------------------------------------------------------------------*
     * The four methods below implement the pure RSA algorithm - first, encrypt with the public key and decrypt with the private key, then encrypt with the private key and       *
     * decrypt with the public key.  The plain text input to encrypt methods is numeric (a BigInteger), and the resulting cipher text is also numeric.  The cipher text input to  *
     * the decrypt methods is numeric (again, a BigInteger). and the resulting plain text is also numeric.  All the other encryption and decryption methods use these methods.    *
     *----------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/


    /**
     * Use the given RSA public key to encrypt the given plain text, using the encrypting exponent.  Note that the plain text is an integer in the range [0..n), where "n" is the
     * RSA modulus.
     *
     * @param _key The RSA public key.
     * @param _plainText The plain text to be encrypted.
     * @return The encrypted plain text (i.e., the ciphertext), which is also in the range [0..n), where "n" is the RSA modulus.
     */
    public static BigInteger encrypt( final RSAPublicKey _key, final BigInteger _plainText ) {

        // sanity checks...
        if( isNull( _key, _plainText ) ) throw new IllegalArgumentException( "_key or _plainText is null" );
        if( _key.n().compareTo( _plainText ) < 0 ) throw new IllegalArgumentException( "_plainText is not less than the modulus of the key" );

        // perform the encryption...
        return _plainText.modPow( _key.eEncrypting(), _key.n() );
    }


    /**
     * Use the given RSA private key to decrypt the given ciphertext.  Assuming the ciphertext was encrypted using the RSA public key that is complementary to this private key,
     * and that the exponent used was the encrypting exponent, the result of the decryption will be the original plaintext.  Note that the ciphertext must be an integer in the
     * range [0..n), where "n" is the RSA modulus.  The resulting plaintext will also be an integer in the same range.
     *
     * @param _key  The RSA private key.
     * @param _cipherText The encrypted text to be decrypted.
     * @return The decrypted ciphertext (i.e., the plaintext), which is also in the range [0..n), where "n" is the RSA modulus.
     */
    public static BigInteger decrypt( final RSAPrivateKey _key, final BigInteger _cipherText ) {

        // sanity checks...
        if( isNull( _key, _cipherText ) ) throw new IllegalArgumentException( "_key or _cipherText is null" );
        if( _key.m().n().compareTo( _cipherText ) < 0 ) throw new IllegalArgumentException( "_cipherText is not less than the modulus of the key" );

        // perform the decryption...
        return pow( _cipherText, _key.dEncrypting(), _key.m() );
    }


    /**
     * Use the given RSA private key to encrypt the given plain text, using the signing exponent.  Note that the plain text is an integer in the range [0..n), where "n" is the
     * RSA modulus.  Conventionally this operation is used to sign a message.
     *
     * @param _key The RSA private key.
     * @param _plainText The plain text to be encrypted.
     * @return The encrypted plain text (i.e., the ciphertext), which is also in the range [0..n), where "n" is the RSA modulus.
     */
    public static BigInteger encrypt( final RSAPrivateKey _key, final BigInteger _plainText ) {

        // sanity checks...
        if( isNull( _key, _plainText ) ) throw new IllegalArgumentException( "_key or _plainText is null" );
        if( _key.m.n().compareTo( _plainText ) < 0 ) throw new IllegalArgumentException( "_plainText is not less than the modulus of the key" );

        // perform the encryption...
        return pow( _plainText, _key.dSigning, _key.m() );
    }


    /**
     * Use the given RSA public key to decrypt the given ciphertext.  Assuming the ciphertext was encrypted using the RSA public key that is complementary to this private key,
     * and that the exponent used was the signing exponent, the result of the decryption will be the original plaintext.  Note that the ciphertext must be an integer in the
     * range [0..n), where "n" is the RSA modulus.  The resulting plaintext will also be an integer in the same range.
     *
     * @param _key  The RSA private key.
     * @param _cipherText The encrypted text to be decrypted.
     * @return The decrypted ciphertext (i.e., the plaintext), which is also in the range [0..n), where "n" is the RSA modulus.
     */
    public static BigInteger decrypt( final RSAPublicKey _key, final BigInteger _cipherText ) {

        // sanity checks...
        if( isNull( _key, _cipherText ) ) throw new IllegalArgumentException( "_key or _cipherText is null" );
        if( _key.n.compareTo( _cipherText ) < 0 ) throw new IllegalArgumentException( "_cipherText is not less than the modulus of the key" );

        // perform the decryption...
        return _cipherText.modPow( _key.eSigning(), _key.n() );
    }


    /*----------------------------------------------------------------------------------------------------------------------------------------------------------------------------*
     * The four methods below implement the RSA algorithm for plain texts and cipher texts are arrays of bytes.  All four methods convert the byte arrays to and from numeric     *
     * form (as BigInteger instances) and then use the preceding four pure RSA methods.                                                                                           *
     *----------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/


    /**
     * Use the given RSA public key to encrypt the given plain text, using the encrypting exponent.  Note that the plain text must resolve to an integer in the range [0..n),
     * where "n" is the RSA modulus.
     *
     * @param _key The RSA public key.
     * @param _plainText The plain text to be encrypted.
     * @return The encrypted plain text (i.e., the ciphertext), which resolves to an integer in the range [0..n), where "n" is the RSA modulus.
     */
    public static byte[] encrypt( final RSAPublicKey _key, final byte[] _plainText ) {

        // sanity checks...
        if( isNull( _key, _plainText ) ) throw new IllegalArgumentException( "_key or _plainText is null" );

        // do the encryption...
        return encrypt( _key, new BigInteger( _plainText ) ).toByteArray();
    }


    /**
     * Use the given RSA private key to decrypt the given ciphertext.  Assuming the ciphertext was encrypted using the RSA public key that is complementary to this private key,
     * and that the exponent used was the encrypting exponent, the result of the decryption will be the original plaintext.  Note that the ciphertext resolve to an integer in the
     * range [0..n), where "n" is the RSA modulus.  The resulting plaintext will also resolve to an integer in the same range.
     *
     * @param _key  The RSA private key.
     * @param _cipherText The encrypted text to be decrypted.
     * @return The decrypted ciphertext (i.e., the plaintext), which resolves to an integer in the range [0..n), where "n" is the RSA modulus.
     */
    public static byte[] decrypt( final RSAPrivateKey _key, final byte[] _cipherText ) {

        // sanity checks...
        if( isNull( _key, _cipherText ) ) throw new IllegalArgumentException( "_key or _cipherText is null" );

        // do the decryption...
        return decrypt( _key, new BigInteger( _cipherText ) ).toByteArray();
    }


    /**
     * Use the given RSA private key to encrypt the given plain text, using the signing exponent.  Note that the plain text must resolve to an integer in the range [0..n),
     * where "n" is the RSA modulus.  Conventionally this operation is used to sign a message.
     *
     * @param _key The RSA private key.
     * @param _plainText The plain text to be encrypted.
     * @return The encrypted plain text (i.e., the ciphertext), which resolves to an integer in the range [0..n), where "n" is the RSA modulus.
     */
    public static byte[] encrypt( final RSAPrivateKey _key, final byte[] _plainText ) {

        // sanity checks...
        if( isNull( _key, _plainText ) ) throw new IllegalArgumentException( "_key or _plainText is null" );

        // do the encryption...
        return encrypt( _key, new BigInteger( _plainText ) ).toByteArray();
    }


    /**
     * Use the given RSA public key to decrypt the given ciphertext.  Assuming the ciphertext was encrypted using the RSA public key that is complementary to this private key,
     * and that the exponent used was the signing exponent, the result of the decryption will be the original plaintext.  Note that the ciphertext must resolve to an integer in the
     * range [0..n), where "n" is the RSA modulus.  The resulting plaintext will also resolve to an integer in the same range.
     *
     * @param _key  The RSA private key.
     * @param _cipherText The encrypted text to be decrypted.
     * @return The decrypted ciphertext (i.e., the plaintext), which resolves to an integer in the range [0..n), where "n" is the RSA modulus.
     */
    public static byte[] decrypt( final RSAPublicKey _key, final byte[] _cipherText ) {

        // sanity checks...
        if( isNull( _key, _cipherText ) ) throw new IllegalArgumentException( "_key or _cipherText is null" );

        // do the decryption...
        return decrypt( _key, new BigInteger( _cipherText ) ).toByteArray();
    }


    /*----------------------------------------------------------------------------------------------------------------------------------------------------------------------------*
     *                                                                                          *
     *----------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/


    private static final int MAX_PRIME_ATTEMPTS_PER_BIT = 100;
    private static final int PRIME_CERTAINTY = 100;


    /**
     * Returns a randomly selected integer of the given bit length that is almost certainly a prime number (uncertainty is 2^-100) and is suitable for use as a p or q value.
     * A prime is suitable if it is not equal to one when taken modulo either the public encryption exponent (eE) or the public signing exponent (eS), and if at least one bit
     * in the most significant 87.5% of the given bit length is set to 1.  The random integer returned is in the range [2^(n/8)..2^n), where n is the bit length.
     *
     * @param _random The source of randomness to use.
     * @param _bitLength The bit length of the desired random number.
     * @param _eE The encryption exponent.
     * @param _eS The signing exponent.
     * @return The suitable random number.
     */
    private static BigInteger generateRSAPrime( final SecureRandom _random, final int _bitLength, final BigInteger _eE, final BigInteger _eS ) {

        // figure out how many attempts we're willing to make before giving up...
        var attempts = MAX_PRIME_ATTEMPTS_PER_BIT * _bitLength;

        // iterate until we find a suitable prime, or we give up...
        while( attempts-- > 0 ) {

            // pick a random number of the desired size...
            var trial = new BigInteger( _bitLength, _random );

            // make sure that the number is not too small...
            if( trial.bitLength() < (_bitLength >> 3) ) continue;

            // make sure that modulo either public key, the number does not equal 1...
            if( trial.mod( _eE ).compareTo( ONE ) == 0 ) continue;
            if( trial.mod( _eS ).compareTo( ONE ) == 0 ) continue;

            // if our number is prime, to a certainty of 1-1/2^100, then we have a winner - return it...
            if( trial.isProbablePrime( PRIME_CERTAINTY ) )
                return trial;
        }

        // if we get here, we couldn't find a suitable prime with our maximum number of attempts...
        throw new IllegalStateException( "couldn't find a suitable RSA prime within " + (MAX_PRIME_ATTEMPTS_PER_BIT * _bitLength) + " attempts" );
    }


    /**
     * Returns a randomly selected integer, uniformly distributed over the range [0..n), where n is the RSA modulus.
     *
     * @param _random A source of randomness.
     * @param _modulus The RSA modulus (n).
     * @return A randomly selected integer, uniformly distributed over the range [0..n).
     */
    private static BigInteger getRandomPlainText( final SecureRandom _random, final BigInteger _modulus ) {

        // sanity checks...
        if( isNull( _random, _modulus ) ) throw new IllegalArgumentException( "_random or _modulus is null" );
        if( _modulus.bitLength() < SHORTEST_REASONABLE_MODULUS_BIT_LENGTH ) throw new IllegalArgumentException( "_modulus is unreasonably small (" + _modulus + ")" );

        // loop until we get a random number within our range...
        BigInteger result;
        do {
            result = new BigInteger( _modulus.bitLength(), _random );
        } while( result.compareTo( _modulus ) >= 0 );

        // we're done...
        return result;
    }


    /**
     * Return a random number in the range [0..n), where "n" is the RSA modulus.  The returned number is suitable for use as a random plain text for the given RSA private key.
     *
     * @param _random The source of randomness.
     * @param _key The RSA private key (used only to provide the RSA modulus).
     * @return The random plain text.
     */
    public static BigInteger getRandomPlainText( final SecureRandom _random, final RSAPrivateKey _key ) {

        // sanity check...
        if( isNull( _random, _key ) ) throw new IllegalArgumentException( "_random or _key is null" );

        // extract the modulus from the key; use it to compute the random result...
        return getRandomPlainText( _random, _key.m().n() );
    }


    /**
     * Return a random number in the range [0..n), where "n" is the RSA modulus.  The returned number is suitable for use as a random plain text for the given RSA private key.
     *
     * @param _random The source of randomness.
     * @param _key The RSA public key (used only to provide the RSA modulus).
     * @return The random plain text.
     */
    public static BigInteger getRandomPlainText( final SecureRandom _random, final RSAPublicKey _key ) {

        // sanity check...
        if( isNull( _random, _key ) ) throw new IllegalArgumentException( "_key is null" );

        // extract the modulus from the key; use it to compute the random result...
        return getRandomPlainText( _random, _key.n() );
    }


    private static final int MAX_KEY_GENERATION_ATTEMPTS = 100;

    /**
     * Generates a complementary pair of random RSA keys (a matching private key and public key) with the given modulus length in bits ({@code _modulusBitLength}, which must be in
     * the range [{@code _bitLengthLoLimit}..{@code _bitLengthHiLimit}]), the given public encrypting exponent ({@code _eEncrypting}), and the given public signing exponent
     * ({@code _eSigning}).  The source of randomness for the key generation is {@code _random}.  This method will make at most 100 attempts to find suitable keys.
     *
     * @param _random The source of randomness for the key generation process.
     * @param _modulusBitLength The desired bit length for the RSA modulus.
     * @param _ePubEncrypting The public encryption exponent.
     * @param _ePubSigning The public signing exponent.
     * @param _bitLengthLoLimit The lower limit for the allowable modulus bit length.
     * @param _bitLengthHiLimit The upper limit for the allowable modulus bit length.
     * @return The outcome of the RSA key generation process, including (if successful) the RSA public/private key pair.
     */
    public static Outcome<RSAKeyPair> generateKeys( final SecureRandom _random,  final int _modulusBitLength,
                                                    final int _ePubEncrypting,   final int _ePubSigning,
                                                    final int _bitLengthLoLimit, final int _bitLengthHiLimit ) {

        // check that we got a source of randomness...
        if( isNull( _random )) throw new IllegalArgumentException( "_random was null" );

        // check for reasonable modulus bit length limits...
        if( (_bitLengthHiLimit < _bitLengthLoLimit) || (_bitLengthLoLimit < 1000) || (_bitLengthHiLimit > 20000) )
            return forgeRSAKeyPair.notOk( "Modulus bit length limits are not reasonable: " + _bitLengthLoLimit + "/" + _bitLengthHiLimit );

        // check for a reasonable modulus bit length...
        if( _modulusBitLength < _bitLengthLoLimit ) return forgeRSAKeyPair.notOk( _modulusBitLength + " is an unreasonably short bit length for a modulus" );
        if( _modulusBitLength > _bitLengthHiLimit ) return forgeRSAKeyPair.notOk( _modulusBitLength + " is an unreasonably long bit length for a modulus"  );

        // check for reasonable public exponents...
        if( (_ePubEncrypting < 3) || (_ePubEncrypting > 99999) ) return forgeRSAKeyPair.notOk( _ePubEncrypting + " _ePubEncrypting is not in [3..99999]" );
        if( (_ePubSigning    < 3) || (_ePubSigning    > 99999) ) return forgeRSAKeyPair.notOk( _ePubSigning    + " _ePubSigning is not in [3..99999]"    );
        if( (_ePubEncrypting & 1) == 0 ) return forgeRSAKeyPair.notOk( "_ePubEncrypting is not odd" );
        if( (_ePubSigning    & 1) == 0 ) return forgeRSAKeyPair.notOk( "_ePubSigning is not odd" );
        if( _ePubEncrypting == _ePubSigning ) return forgeRSAKeyPair.notOk( "_ePubEncrypting cannot be the same value as _ePubSigning" );

        // make sure that the two public exponents have no common factors...
        var ePubEncrypting = BigInteger.valueOf( _ePubEncrypting );
        var ePubSigning    = BigInteger.valueOf( _ePubSigning    );
        if( extendedGCD( ePubEncrypting, ePubSigning ).gcd().compareTo( ONE) != 0 )
            return forgeRSAKeyPair.notOk( _ePubEncrypting + " and " + _ePubSigning + " have one or more common factors" );

        // if we get here, then we're ready to attempt key generation - but we'll only try MAX_KEY_GENERATION_ATTEMPTS times before we throw up our hands and give up...
        var attempts = 0;
        while( attempts++ < MAX_KEY_GENERATION_ATTEMPTS ) {

            // generate a trial p and q with half our desired modulus bit length...
            var p = generateRSAPrime( _random, _modulusBitLength >> 1, ePubEncrypting, ePubSigning );
            var q = generateRSAPrime( _random, _modulusBitLength >> 1, ePubEncrypting, ePubSigning );

            // if p == q, try again...
            if( p.compareTo( q ) == 0 )
                continue;

            // compute n = p * q...
            var n = p.multiply( q );

            // make sure n has the right bit length...
            if( n.bitLength() != _modulusBitLength)
                continue;

            // compute t as the least common multiple (p − 1, q − 1)...
            var t = lcm( p.subtract( ONE ), q.subtract( ONE ) );

            // compute the private encrypting exponent, and verify that it shares no factors with t...
            var egcd = extendedGCD( ePubEncrypting, t );
            if( egcd.gcd().compareTo( ONE ) != 0 )   // if the gcd is anything other than 1, we've got a nasty shared factor, so try again...
                continue;
            var ePriEncrypting = egcd.x().mod( t );

            // compute the private signing exponent, and verify that it shares no factors with t...
            egcd = extendedGCD( ePubSigning, t );
            if( egcd.gcd().compareTo( ONE ) != 0 )   // if the gcd is anything other than 1, we've got a nasty shared factor, so try again...
                continue;
            var ePriSigning = egcd.x().mod( t );

            // if we get here, then we have all the information we need to make a usable pair of keys - so construct our result and skedaddle...
            System.out.println( "Attempts: " + attempts );
            var pubKey = new RSAPublicKey( n, ePubEncrypting, ePubSigning );
            var priKey = new RSAPrivateKey( new CompositeIntegerModulus( n, p, q ), t, ePriEncrypting, ePriSigning );
            return forgeRSAKeyPair.ok( new RSAKeyPair( pubKey, priKey ) );
        }

        // if we get here, we couldn't generate keys within a reasonable number of attempts...
        return forgeRSAKeyPair.notOk( "could not generate RSA keys after " + attempts + " attempts" );
    }


    private static final int SHORTEST_REASONABLE_MODULUS_BIT_LENGTH = 2000;
    private static final int LONGEST_REASONABLE_MODULUS_BIT_LENGTH  = 10000;

    /**
     * Generates a complementary pair of random RSA keys (a matching private key and public key) with the given modulus length in bits ({@code _modulusBitLength}, which must be in
     * the range [2000..10000]), the given public encrypting exponent ({@code _eEncrypting}), and the given public signing exponent ({@code _eSigning}).  The source of randomness
     * for the key generation is {@code _random}.  This method will make at most 100 attempts to find suitable keys.
     *
     * @param _random The source of randomness for the key generation process.
     * @param _modulusBitLength The desired bit length for the RSA modulus.
     * @param _ePubEncrypting The public encryption exponent.
     * @param _ePubSigning The public signing exponent.
     * @return The outcome of the RSA key generation process, including (if successful) the RSA public/private key pair.
     */
    public static Outcome<RSAKeyPair> generateKeys( final SecureRandom _random, final int _modulusBitLength, final int _ePubEncrypting, final int _ePubSigning ) {
        return generateKeys(
                _random, _modulusBitLength,
                _ePubEncrypting, _ePubSigning,
                SHORTEST_REASONABLE_MODULUS_BIT_LENGTH, LONGEST_REASONABLE_MODULUS_BIT_LENGTH );
    }


    private static final int DEFAULT_PUBLIC_ENCRYPTING_EXPONENT    = 5;
    private static final int DEFAULT_PUBLIC_SIGNING_EXPONENT       = 3;

    /**
     * Generates a complementary pair of random RSA keys (a matching private key and public key) with the given modulus length in bits ({@code _modulusBitLength}, which must be in
     * the range [2000..10000]), the given public encrypting exponent ({@code 5}), and the public signing exponent ({@code 3}).  The source of randomness for the key generation is
     * {@code _random}.  This method will make at most 100 attempts to find suitable keys.
     *
     * @param _random The source of randomness for the key generation process.
     * @param _modulusBitLength The desired bit length for the RSA modulus.
     * @return The outcome of the RSA key generation process, including (if successful) the RSA public/private key pair.
     */
    public static Outcome<RSAKeyPair> generateKeys( final SecureRandom _random, final int _modulusBitLength ) {
        return generateKeys(
                _random, _modulusBitLength,
                DEFAULT_PUBLIC_ENCRYPTING_EXPONENT, DEFAULT_PUBLIC_SIGNING_EXPONENT,
                SHORTEST_REASONABLE_MODULUS_BIT_LENGTH, LONGEST_REASONABLE_MODULUS_BIT_LENGTH );
    }


    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";          // the name of the default hash algorithm for use in RSA...
    private static final int    MAX_MASK_BYTES = Integer.MAX_VALUE;  // maximum allowable bytes in mask...


    /**
     * Mask generator (as defined by RFC 3447, section B.2) that returns a cryptographic hash of the given bytes, with the given length determining the size of the resulting
     * hash (in bytes).  The algorithm used is MGF1 (as defined in RFC 3447, section B.2.1).
     *
     * @param _bytes The bytes to compute a hash of.
     * @param _length The desired length (in bytes) of the hash results.
     * @param _hasher The {@link MessageDigest} to use within this method.
     * @return Ok with the hash results, or not ok with an explanatory message.
     */
    public static Outcome<byte[]> mask( final byte[] _bytes, final int _length, final MessageDigest _hasher ) {

        // sanity checks...
        //noinspection RedundantCast
        if( isNull( (Object) _bytes ) || (_bytes.length == 0) )
            return forgeBytes.notOk( "_bytes is null or empty" );
        if( (_length < 0) )
            return forgeBytes.notOk( "_length is negative: " + _length );

        // some setup...
        var result = new byte[0];
        var counter = 0;

        // iterate until our result is long enough...
        while( result.length < _length ) {

            // get the bytes we need to hash this time around...
            ByteBuffer bb = ByteBuffer.allocate( _bytes.length + 4 );
            bb.order( ByteOrder.BIG_ENDIAN );
            bb.put( _bytes );
            bb.putInt( counter );

            // concatenate the hash of these bytes with what we've already got...
            _hasher.reset();
            result = Bytes.concatenate( result, _hasher.digest( bb.array() ) );

            // update our counter; this ensures that each iteration is different...
            counter++;
        }

        // return the requested number of bytes...
        return forgeBytes.ok( Arrays.copyOf( result, _length ) );
    }


    /**
     * Mask generator (as defined by RFC 3447, section B.2) that returns a cryptographic hash of the given bytes, with the given length determining the size of the resulting
     * hash (in bytes).  The algorithm used is MGF1 (as defined in RFC 3447, section B.2.1).  The hash algorithm used is SHA-256.
     *
     * @param _bytes The bytes to compute a hash of.
     * @param _length The desired length (in bytes) of the hash results.
     * @return Ok with the hash results, or not ok with an explanatory message.
     */
    public static Outcome<byte[]> mask( final byte[] _bytes, final int _length ) {

        // get the default message digest...
        MessageDigest hasher;
        try {
            hasher = MessageDigest.getInstance( DEFAULT_HASH_ALGORITHM );
        }
        catch( NoSuchAlgorithmException _e ) {
            return forgeBytes.notOk( "Hash algorithm does not exist: " + DEFAULT_HASH_ALGORITHM );
        }

        return mask( _bytes, _length, hasher );
    }


    /*

    From RFC 3447:

    2. EME-OAEP encoding (see Figure 1 below):

      a. If the label L is not provided, let L be the empty string. Let
         lHash = Hash(L), an octet string of length hLen (see the note
         below).

      b. Generate an octet string PS consisting of k - mLen - 2hLen - 2
         zero octets.  The length of PS may be zero.

      c. Concatenate lHash, PS, a single octet with hexadecimal value
         0x01, and the message M to form a data block DB of length k -
         hLen - 1 octets as

            DB = lHash || PS || 0x01 || M.

      d. Generate a random octet string seed of length hLen.

      e. Let dbMask = MGF(seed, k - hLen - 1).

      f. Let maskedDB = DB \xor dbMask.

      g. Let seedMask = MGF(maskedDB, hLen).

      h. Let maskedSeed = seed \xor seedMask.

      i. Concatenate a single octet with hexadecimal value 0x00,
         maskedSeed, and maskedDB to form an encoded message EM of
         length k octets as

            EM = 0x00 || maskedSeed || maskedDB.


                             +----------+---------+-------+
                        DB = |  lHash   |    PS   |   M   |
                             +----------+---------+-------+
                                            |
                  +----------+              V
                  |   seed   |--> MGF ---> xor
                  +----------+              |
                        |                   |
               +--+     V                   |
               |00|    xor <----- MGF <-----|
               +--+     |                   |
                 |      |                   |
                 V      V                   V
               +--+----------+----------------------------+
         EM =  |00|maskedSeed|          maskedDB          |
               +--+----------+----------------------------+

     */

    /**
     * Pad the given message so that its length (in bytes) is the same as the length (in bytes) of the RSA modulus in a public or private RSA key.  Implements a padding scheme
     * that allows values smaller than the RSA modulus to be securely encrypted.  The algorithm used is called RSAES-OAEP in RFC 3447; see that document for implementation
     * details.  The mask generation function used is MGF1 (also defined in RFC 3447).
     *
     * @param _rsaModulus The modulus ("n") of the RSA key that will be used to encrypt the result of this method.
     * @param _message The message that will be padded in this method, then encrypted.  The message length (in bytes) must be less than or equal to k - 66, where k is the
     *                 number of bytes in the RSA key's modulus.
     * @param _label  The string label for this padding.  Note that the label will be encoded as UTF-8 (which, for characters in the ASCII character set, is identical to ASCII).
     *                Note that the hash of the label is used to construct the returned padded value, but not the label itself.
     * @param _random The source of randomness for the padding operation.
     * @param _hasher The {@link MessageDigest} to use in the padding operation.
     * @return The outcome of the padding operation.  If ok, the info is a byte array containing the padded message.  If not ok, the outcome contains an explanatory message.
     */
    public static Outcome<byte[]> pad( final BigInteger _rsaModulus, final byte[] _message, final String _label, final SecureRandom _random, final MessageDigest _hasher ) {

        // see the relevant RFC 3447 text in comments above...

        // sanity checks...
        if( isNull( _rsaModulus, _message, _random, _hasher ) ) return forgeBytes.notOk( "_rsaModulus, _message, _random, or _hasher is null" );

        // get our string into a byte array with a zero terminator...
        var label = (isNull( _label ) ? "" : _label) + "\0";
        var labelBytes = label.getBytes( StandardCharsets.UTF_8 );

        // some prep...
        var hashLen = _hasher.getDigestLength();
        var k = (_rsaModulus.bitLength() >>> 3) + ((_rsaModulus.bitLength() & 7) == 0 ? 0 : 1);  // get number of bytes required to hold the modulus...
        var psLen = k - _message.length - (hashLen << 1) - 2;
        if( psLen < 0 ) return forgeBytes.notOk( "_message is too long" );

        // the actual OAEP padding algorithm...

        // hash the label bytes (which may be a single 0 byte)...
        _hasher.reset();
        var lHash = _hasher.digest( labelBytes );

        // generate ps 0 bytes (length could be zero)...
        var ps = new byte[psLen];

        // generate data block db (length will be k - hashLen - 1)...
        var db = Bytes.concatenate( lHash, ps, new byte[] {1}, _message );

        // generate random seed...
        var seed = new byte[hashLen];
        _random.nextBytes( seed );

        // generate data block mask...
        var dbMask = mask( seed, db.length, _hasher );
        if( dbMask.notOk() ) return forgeBytes.notOk( "dbMask: " + dbMask.msg() );

        // generate masked data block...
        var maskedDb = Bytes.xor( db, 0, dbMask.info(), 0, db.length );

        // generate seed mask...
        var seedMask = mask( maskedDb, hashLen, _hasher );
        if( seedMask.notOk() ) return forgeBytes.notOk( "seedMask: " + seedMask.msg() );

        // generate masked seed...
        var maskedSeed = Bytes.xor( seed, 0, seedMask.info(), 0, seed.length );

        // generate encoded message (length is k)...
        var em = Bytes.concatenate( new byte[] {0}, maskedSeed, maskedDb );

        // return our result...
        return forgeBytes.ok( em );
    }


    /**
     * Pad the given message so that its length (in bytes) is the same as the length (in bytes) of the RSA modulus in a public or private RSA key.  Implements a padding scheme
     * that allows values smaller than the RSA modulus to be securely encrypted.  The algorithm used is called RSAES-OAEP in RFC 3447; see that document for implementation
     * details.  The mask generation function used is MGF1 (also defined in RFC 3447).  The hash function used is SHA-256.
     *
     * @param _rsaModulus The modulus ("n") of the RSA key that will be used to encrypt the result of this method.
     * @param _message The message that will be padded in this method, then encrypted.  The message length (in bytes) must be less than or equal to k - 66, where k is the
     *                 number of bytes in the RSA key's modulus.
     * @param _label  The string label for this padding.  Note that the label will be encoded as UTF-8 (which, for characters in the ASCII character set, is identical to ASCII).
     *                Note that the hash of the label is used to construct the returned padded value, but not the label itself.
     * @param _random The source of randomness for the padding operation.
     * @return The outcome of the padding operation.  If ok, the info is a byte array containing the padded message.  If not ok, the outcome contains an explanatory message.
     */
    public static Outcome<byte[]> pad( final BigInteger _rsaModulus, final byte[] _message, final String _label, final SecureRandom _random ) {

        // get the default message digest...
        MessageDigest hasher;
        try {
            hasher = MessageDigest.getInstance( DEFAULT_HASH_ALGORITHM );
        }
        catch( NoSuchAlgorithmException _e ) {
            return forgeBytes.notOk( "Hash algorithm does not exist: " + DEFAULT_HASH_ALGORITHM );
        }

        return pad( _rsaModulus, _message, _label, _random, hasher );
    }


    /**
     * Pad the given message so that its length (in bytes) is the same as the length (in bytes) of the RSA modulus in a public or private RSA key.  Implements a padding scheme
     * that allows values smaller than the RSA modulus to be securely encrypted.  The algorithm used is called RSAES-OAEP in RFC 3447; see that document for implementation
     * details.  The mask generation function used is MGF1 (also defined in RFC 3447).  The hash function used is SHA-256.  No label is used.
     *
     * @param _rsaModulus The modulus ("n") of the RSA key that will be used to encrypt the result of this method.
     * @param _message The message that will be padded in this method, then encrypted.  The message length (in bytes) must be less than or equal to k - 66, where k is the
     *                 number of bytes in the RSA key's modulus.
     * @param _random The source of randomness for the padding operation.
     * @return The outcome of the padding operation.  If ok, the info is a byte array containing the padded message.  If not ok, the outcome contains an explanatory message.
     */
    public static Outcome<byte[]> pad( final BigInteger _rsaModulus, final byte[] _message, final SecureRandom _random ) {

        return pad( _rsaModulus, _message, null, _random );
    }


    /*

    From RFC 3447:

    3. EME-OAEP decoding:

      a. If the label L is not provided, let L be the empty string. Let
         lHash = Hash(L), an octet string of length hLen (see the note
         in Section 7.1.1).

      b. Separate the encoded message EM into a single octet Y, an octet
         string maskedSeed of length hLen, and an octet string maskedDB
         of length k - hLen - 1 as

            EM = Y || maskedSeed || maskedDB.

      c. Let seedMask = MGF(maskedDB, hLen).

      d. Let seed = maskedSeed \xor seedMask.

      e. Let dbMask = MGF(seed, k - hLen - 1).

      f. Let DB = maskedDB \xor dbMask.

      g. Separate DB into an octet string lHash' of length hLen, a
         (possibly empty) padding string PS consisting of octets with
         hexadecimal value 0x00, and a message M as

            DB = lHash' || PS || 0x01 || M.

         If there is no octet with hexadecimal value 0x01 to separate PS
         from M, if lHash does not equal lHash', or if Y is nonzero,
         output "decryption error" and stop.  (See the note below.)

     */

    /**
     * Unpad the given message (which is presumed to have been padded with RSAES-OAEP, using the same hash algorithm and the MGF1 mask generation function), verifying the given
     * label and padded message format.  See RFC 3447 for details about both OAEP and MGF1.
     *
     * @param _message The padded message to be unpadded.
     * @param _label The string label to be verified.  Note that the label will be encoded as UTF-8 (which, for characters in the ASCII character set, is identical to ASCII).
     * @param _hasher The {@link MessageDigest} to use when unpadding (it must be the same one used when padding).
     * @return The outcome of the unpadding operation.  If ok, the info contains the unpadded original message.  If not ok, the outcome contains an explanatory message.
     */
    public static Outcome<byte[]> unpad( final byte[] _message, final String _label, final MessageDigest _hasher ) {

        // see the relevant RFC 3447 text in comments above...

        // sanity checks...
        if( isNull( _message, _hasher ) ) return forgeBytes.notOk( "_message or _hasher is null" );
        var hashLen = _hasher.getDigestLength();
        if( (_message.length - hashLen - 1) <= (hashLen + 1) ) return forgeBytes.notOk( "_message is impossibly short" );

        // get our string into a byte array with a zero terminator...
        var label = (isNull( _label ) ? "" : _label) + "\0";
        var labelBytes = label.getBytes( StandardCharsets.US_ASCII );

        // the actual OAEP unpadding algorithm...

        // split the given message into Y, maskedSeed, and maskedDB...
        var y = Bytes.copy( _message, 0, 1 );
        var maskedSeed = Bytes.copy( _message, 1, hashLen );
        var maskedDB = Bytes.copy( _message, hashLen + 1, _message.length - hashLen -1 );

        // get the seed mask...
        var seedMask = mask( maskedDB, hashLen, _hasher );
        if( seedMask.notOk() ) return forgeBytes.notOk( "seedMask: " + seedMask.msg() );

        // get the seed...
        var seed = Bytes.xor( maskedSeed, 0, seedMask.info(), 0, hashLen );

        // get the data block (DB) mask...
        var dbMask = mask( seed, maskedDB.length, _hasher );
        if( dbMask.notOk() ) return forgeBytes.notOk( "dbMask: " + dbMask.msg() );

        // get the data block (DB)...
        var db = Bytes.xor( maskedDB, 0, dbMask.info(), 0, maskedDB.length );

        // scan the data block to find the beginning of the original message...
        var msgIndex = 0;
        for( int i = hashLen; i < db.length; i++ ) {
            int b = db[i] & 0xff;
            if( b == 1 ) {
                msgIndex = i + 1;
                if( (db.length - msgIndex) == 0 ) return forgeBytes.notOk( "Unpadded message in data block is zero bytes long" );
                break;
            }
            else if( b != 0 ){
                return forgeBytes.notOk( "Data block in padded message is malformed" );
            }
        }
        if( msgIndex == 0 ) return forgeBytes.notOk( "Unpadded message in data block not found" );

        // split the data block into label hash and the original message...
        var lHash = Bytes.copy( db, 0, hashLen );
        var msg = Bytes.copy( db, msgIndex, db.length - msgIndex );

        // validity checks...
        if( y[0] != 0 ) return forgeBytes.notOk( "Malformed padded message: initial byte of message is not zero" );
        _hasher.reset();
        var lHashCheck = _hasher.digest( labelBytes );
        if( !Arrays.equals( lHash, lHashCheck ) ) return forgeBytes.notOk( "Label hash mismatch" );

        return forgeBytes.ok( msg );
    }


    /**
     * Unpad the given message (which is presumed to have been padded with RSAES-OAEP, using the same hash algorithm and the MGF1 mask generation function), verifying the given
     * label and padded message format.  See RFC 3447 for details about both OAEP and MGF1.  The hash algorithm used is SHA-256.
     *
     * @param _message The padded message to be unpadded.
     * @param _label The string label to be verified.  Note that the label will be encoded as UTF-8 (which, for characters in the ASCII character set, is identical to ASCII).
     * @return The outcome of the unpadding operation.  If ok, the info contains the unpadded original message.  If not ok, the outcome contains an explanatory message.
     */
    public static Outcome<byte[]> unpad( final byte[] _message, final String _label ) {

        // get the default message digest...
        MessageDigest hasher;
        try {
            hasher = MessageDigest.getInstance( DEFAULT_HASH_ALGORITHM );
        }
        catch( NoSuchAlgorithmException _e ) {
            return forgeBytes.notOk( "Hash algorithm does not exist: " + DEFAULT_HASH_ALGORITHM );
        }

        return unpad( _message, _label, hasher );
    }


    /**
     * Unpad the given message (which is presumed to have been padded with RSAES-OAEP, using the same hash algorithm and the MGF1 mask generation function), verifying the given
     * label and padded message format.  See RFC 3447 for details about both OAEP and MGF1.  The hash algorithm used is SHA-256.  The label is not used.
     *
     * @param _message The padded message to be unpadded.
     * @return The outcome of the unpadding operation.  If ok, the info contains the unpadded original message.  If not ok, the outcome contains an explanatory message.
     */
    public static Outcome<byte[]> unpad( final byte[] _message ) {

        return unpad( _message, null );
    }
}
