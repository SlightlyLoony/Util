package com.dilatush.util;

import java.math.BigInteger;
import java.security.SecureRandom;
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
public class RSA {

    private static final Outcome.Forge<RSAKeyPair>    forgeRSAKeyPair    = new Outcome.Forge<>();
    private static final Outcome.Forge<RSAPublicKey>  forgeRSAPublicKey  = new Outcome.Forge<>();
    private static final Outcome.Forge<RSAPrivateKey> forgeRSAPrivateKey = new Outcome.Forge<>();


    /**
     * An RSA public key.
     *
     * @param n The modulus for this key.
     * @param eEncrypting The encrypting exponent for this key.
     * @param eSigning The signature verification exponent for this key.
     */
    public record RSAPublicKey( BigInteger n, BigInteger eEncrypting, BigInteger eSigning ) {


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
            return    "n:"   + Base64Fast.encode( n )           + ";"
                    + ";eE:" + Base64Fast.encode( eEncrypting ) + ";"
                    + ";eS:" + Base64Fast.encode( eSigning )    + ";";
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

            return    "p:"  + Base64Fast.encode( m.p() )       + ";"
                    + "q:"  + Base64Fast.encode( m.q() )       + ";"
                    + "dE:" + Base64Fast.encode( dEncrypting ) + ";"
                    + "dS:" + Base64Fast.encode( dSigning )    + ";";
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
}


/**
     * A complementary pair of RSA keys, one public, one private.
     *
     * @param publicKey The public key in this RSA key pair.
     * @param privateKey The private key in this RSA key pair.
     */
    public record RSAKeyPair( RSAPublicKey publicKey, RSAPrivateKey privateKey ) {}


    /**
     * Use the given RSA public key to encrypt the given plain text.  Note that the plain text is an integer in the range [0..n), where "n" is the RSA modulus.
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
     * the result of the decryption will be the original plaintext.  Note that the ciphertext must be an integer in the range [0..n), where "n" is the RSA modulus.  The
     * resulting plaintext will also be an integer in the same range.
     *
     * @param _key  The RSA private key.
     * @param _cipherText The encrypted text to be decrypted.
     * @return The decrypted ciphertext (i.e., the plaintext), which is also in the range [0..n), where "n" is the RSA modulus.
     */
    public static BigInteger decrypt( final RSAPrivateKey _key, final BigInteger _cipherText ) {

        // sanity checks...
        if( isNull( _key, _cipherText ) ) throw new IllegalArgumentException( "_key or _cipherText is null" );
        if( _key.m().n().compareTo( _cipherText ) < 0 ) throw new IllegalArgumentException( "_cipherText is not less than the modulus of the key" );

        // perform the encryption...
        return pow( _cipherText, _key.dEncrypting(), _key.m() );
    }


    private static final int MAX_PRIME_ATTEMPTS_PER_BIT = 100;
    private static final int PRIME_CERTAINTY = 100;


    /**
     * Returns a randomly selected integer of the given bit length that is almost certainly a prime number (uncertainty is 2^-100) and is suitable for use as a p or q value.
     * A prime is suitable if it is not equal to one when taken modulo either the public encryption exponent (eE) or the public signing exponent (eS).
     *
     * @param _random The source of randomness to use.
     * @param _bitLength  //TODO: finish this!
     * @param _eE
     * @param _eS
     * @return
     */
    private static BigInteger generateRSAPrime( final SecureRandom _random, final int _bitLength, final BigInteger _eE, final BigInteger _eS ) {

        // figure out how many attempts we're willing to make before giving up...
        var attempts = MAX_PRIME_ATTEMPTS_PER_BIT * _bitLength;

        // iterate until we find a suitable prime, or we give up...
        while( attempts-- > 0 ) {

            // pick a random number of the desired size...
            var trial = new BigInteger( _bitLength, _random );

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
     * Returns a randomly selected integer, uniformly distributed over the range [m..n), where n is the RSA modulus, and m is the first integer larger than n^(1/3).  The
     * upper bound is the largest number that can be encrypted by RSA using the modulus n.  The lower bound is the smallest number such that m^3 != m^3 modulo n.
     *
     * @param _random A source of randomness.
     * @param _modulus The RSA modulus (n).
     * @return A randomly selected integer, uniformly distributed over the range [m..n).
     */
    private static BigInteger getRandomPlainText( final SecureRandom _random, final BigInteger _modulus ) {

        // sanity checks...
        if( isNull( _random, _modulus ) ) throw new IllegalArgumentException( "_random or _modulus is null" );
        if( _modulus.compareTo( BigInteger.valueOf( 1000 ) ) <= 0 ) throw new IllegalArgumentException( "_modulus is unreasonably small (" + _modulus + ")" );

        // compute our bounds [m..n], and the range (n - m)...
        // noinspection UnnecessaryLocalVariable
        var n = _modulus;
        var m = root( n, 3 ).add( ONE );
        var r = n.subtract( m );

        // iterate until we get a random number within our range...
        BigInteger result;
        do {
            result = new BigInteger( r.bitLength(), _random );
        } while( result.compareTo( r ) >= 0 );

        // add the lower bound and we've got the answer we wanted...
        return result.add( m );
    }


    public static BigInteger getRandomPlainText( final SecureRandom _random, final RSAPrivateKey _key ) {

        // sanity check...
        if( isNull( _key ) ) throw new IllegalArgumentException( "_key is null" );

        // extract the modulus from the key; use it to compute the random result...
        return getRandomPlainText( _random, _key.m().n() );
    }


    public static BigInteger getRandomPlainText( final SecureRandom _random, final RSAPublicKey _key ) {

        // sanity check...
        if( isNull( _key ) ) throw new IllegalArgumentException( "_key is null" );

        // extract the modulus from the key; use it to compute the random result...
        return getRandomPlainText( _random, _key.n() );
    }


    private static final int MAX_KEY_GENERATION_ATTEMPTS = 100;

    /**
     * Generates a pair of RSA keys (a private key and a public key) with the given modulus length (n) in bits, the given public encrypting exponent (eEncrypting), and the given
     * public signing exponent (eSigning).
     *
     * @param _random
     * @param _modulusBitLength
     * @param _ePubEncrypting
     * @param _ePubSigning
     * @param _bitLengthLoLimit
     * @param _bitLengthHiLimit
     * @return
     */
    public static Outcome<RSAKeyPair> generateKeys( final SecureRandom _random, final int _modulusBitLength,
                                                    final int _ePubEncrypting, final int _ePubSigning,
                                                    final int _bitLengthLoLimit, final int _bitLengthHiLimit ) {

        // check that we got a source of randomness...
        if( isNull( _random )) throw new IllegalArgumentException( "_random was null" );

// TODO: better checks for arguments...
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
     * Generates a pair of RSA keys (a private key and a public key) with the given modulus length (n) in bits, using 3 as the public encrypting exponent, and 5 as
     * the public signing exponent.
     *
     * @param _random
     * @param _modulusBitLength
     * @return
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
     * Generates a pair of RSA keys (a private key and a public key) with the given modulus length (n) in bits, using 3 as the public encrypting exponent, and 5 as
     * the public signing exponent.
     *
     * @param _random
     * @param _modulusBitLength
     * @return
     */
    public static Outcome<RSAKeyPair> generateKeys( final SecureRandom _random, final int _modulusBitLength ) {
        return generateKeys(
                _random, _modulusBitLength,
                DEFAULT_PUBLIC_ENCRYPTING_EXPONENT, DEFAULT_PUBLIC_SIGNING_EXPONENT,
                SHORTEST_REASONABLE_MODULUS_BIT_LENGTH, LONGEST_REASONABLE_MODULUS_BIT_LENGTH );
    }
}
