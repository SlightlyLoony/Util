package com.dilatush.util;

import java.math.BigInteger;
import java.security.SecureRandom;

import static com.dilatush.util.General.isNull;
import static java.math.BigInteger.ONE;

/**
 * Static container class for functions related to RSA public key encryption.  The design of these functions relies heavily on the RSA chapters of <i>Cryptography Engineering</i>.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class RSA {

    private static final int MAX_KEY_GENERATION_ATTEMPTS           = 100;
    private static final int DEFAULT_PUBLIC_ENCRYPTING_EXPONENT    = 5;
    private static final int DEFAULT_PUBLIC_SIGNING_EXPONENT       = 3;
    private static final int SMALLEST_ALLOWABLE_MODULUS_BIT_LENGTH = 10;   // TODO: put this back to 2000...
    private static final int LARGEST_ALLOWABLE_MODULUS_BIT_LENGTH  = 10000;


    /**
     *
     * @param _key
     * @param _plainText
     * @return
     */
    public static BigInteger encryptPublic( final RSAPublicKey _key, final BigInteger _plainText ) {

        // sanity checks...
        if( isNull( _key, _plainText ) ) throw new IllegalArgumentException( "_key or _plainText is null" );
        if( _key.n.compareTo( _plainText ) < 0 ) throw new IllegalArgumentException( "_plainText is not less than the modulus of the key" );

        // perform the encryption...
        return _plainText.modPow( _key.eEncrypting, _key.n );
    }


    /**
     *
     * @param _key
     * @param _cipherText
     * @return
     */
    public static BigInteger decryptPrivate( final RSAPrivateKey _key, final BigInteger _cipherText ) {

        // sanity checks...
        if( isNull( _key, _cipherText ) ) throw new IllegalArgumentException( "_key or _cipherText is null" );
        if( _key.m.n().compareTo( _cipherText ) < 0 ) throw new IllegalArgumentException( "_cipherText is not less than the modulus of the key" );

        // perform the encryption...
        return CompositeModuloMath.pow( _cipherText, _key.dEncrypting, _key.m );
    }


    private static final int MAX_PRIME_ATTEMPTS_PER_BIT = 100;
    private static final int PRIME_CERTAINTY = 100;

    private static BigInteger generateRSAPrime( final SecureRandom _random, final int _bitLength, final BigInteger _pubKey1, final BigInteger _pubKey2 ) {

        // figure out how many attempts we're willing to make before giving up...
        var attempts = MAX_PRIME_ATTEMPTS_PER_BIT * _bitLength;

        // iterate until we find a suitable prime, or we give up...
        while( attempts-- > 0 ) {

            // pick a random number of the desired size...
            var trial = new BigInteger( _bitLength, _random );

            // make sure that modulo either public key, the number does not equal 1...
            if( trial.mod( _pubKey1 ).compareTo( ONE ) == 0 ) continue;
            if( trial.mod( _pubKey2 ).compareTo( ONE ) == 0 ) continue;

            // if our number is prime, to a certainty of 1-1/2^100, then we have a winner - return it...
            if( trial.isProbablePrime( PRIME_CERTAINTY ) )
                return trial;
        }

        // if we get here, we couldn't find a suitable prime with our maximum number of attempts...
        throw new IllegalStateException( "couldn't find a suitable RSA prime within " + (MAX_PRIME_ATTEMPTS_PER_BIT * _bitLength) + " attempts" );
    }


    /**
     * Generates a pair of RSA keys (a private key and a public key) with the given modulus length (n) in bits, the given public encrypting exponent (eEncrypting), and the given
     * public signing exponent (eSigning).
     *
     * @param _random
     * @param _modulusBitLength
     * @param _ePubEncrypting
     * @param _ePubSigning
     * @return
     */
    public static RSAKeyPair generateKeys( final SecureRandom _random, final int _modulusBitLength, final int _ePubEncrypting, final int _ePubSigning ) {

        // check that we got a source of randomness...
        if( isNull( _random ))
            throw new IllegalArgumentException( "_random was null" );

        // check for a reasonable modulus bit length...
        if( _modulusBitLength < SMALLEST_ALLOWABLE_MODULUS_BIT_LENGTH  ) throw new IllegalArgumentException( _modulusBitLength + " is unreasonably small for a modulus" );
        if( _modulusBitLength > LARGEST_ALLOWABLE_MODULUS_BIT_LENGTH   ) throw new IllegalArgumentException( _modulusBitLength + " is unreasonably large for a modulus" );

        // check for reasonable public exponents...
        if( (_ePubEncrypting < 3) || (_ePubEncrypting > 99999) ) throw new IllegalArgumentException( _ePubEncrypting + " _ePubEncrypting is not in [3..99999]" );
        if( (_ePubSigning    < 3) || (_ePubSigning    > 99999) ) throw new IllegalArgumentException( _ePubSigning    + " _ePubSigning is not in [3..99999]"    );
        if( (_ePubEncrypting & 1) == 0 ) throw new IllegalArgumentException( "_ePubEncrypting is not odd" );
        if( (_ePubSigning    & 1) == 0 ) throw new IllegalArgumentException( "_ePubSigning is not odd" );
        if( _ePubEncrypting == _ePubSigning ) throw new IllegalArgumentException( "_ePubEncrypting cannot be the same value as _ePubSigning" );

        // make sure that the two public exponents have no common factors...
        var ePubEncrypting = BigInteger.valueOf( _ePubEncrypting );
        var ePubSigning    = BigInteger.valueOf( _ePubSigning    );
        if( BigIntegers.extendedGCD( ePubEncrypting, ePubSigning ).gcd().compareTo( ONE) != 0 )
            throw new IllegalArgumentException( _ePubEncrypting + " and " + _ePubSigning + " have one or more common factors" );

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
            var t = BigIntegers.lcm( p.subtract( ONE ), q.subtract( ONE ) );

            // compute the private encrypting exponent, and verify that it shares no factors with t...
            var egcd = BigIntegers.extendedGCD( ePubEncrypting, t );
            if( egcd.gcd().compareTo( ONE ) != 0 )   // if the gcd is anything other than 1, we've got a nasty shared factor, so try again...
                continue;
            var ePriEncrypting = egcd.x().mod( t );

            // compute the private signing exponent, and verify that it shares no factors with t...
            egcd = BigIntegers.extendedGCD( ePubSigning, t );
            if( egcd.gcd().compareTo( ONE ) != 0 )   // if the gcd is anything other than 1, we've got a nasty shared factor, so try again...
                continue;
            var ePriSigning = egcd.x().mod( t );

            // if we get here, then we have all the information we need to make a usable pair of keys - so construct our result and skedaddle...
            var pubKey = new RSAPublicKey( n, ePubEncrypting, ePubSigning );
            var priKey = new RSAPrivateKey( new CompositeModuloMath.CompositeIntegerModulus( n, p, q ), t, ePriEncrypting, ePriSigning );
            return new RSAKeyPair( pubKey, priKey );
        }

        // if we get here, we couldn't generate keys within a reasonable number of attempts...
        throw new IllegalStateException( "could not generate RSA keys after " + attempts + " attempts" );
    }


    /**
     * Generates a pair of RSA keys (a private key and a public key) with the given modulus length (n) in bits, using 3 as the public encrypting exponent, and 5 as
     * the public signing exponent.
     *
     * @param _random
     * @param _modulusBitLength
     * @return
     */
    public static RSAKeyPair generateKeys( final SecureRandom _random, final int _modulusBitLength ) {
        return generateKeys( _random, _modulusBitLength, DEFAULT_PUBLIC_ENCRYPTING_EXPONENT, DEFAULT_PUBLIC_SIGNING_EXPONENT );
    }


    /**
     * An RSA public key.
     *
     * @param n The modulus for this key.
     * @param eEncrypting The encrypting exponent for this key.
     * @param eSigning The signing exponent for this key.
     */
    public record RSAPublicKey( BigInteger n, BigInteger eEncrypting, BigInteger eSigning ) {}


    /**
     * An RSA private key.
     *
     * @param m
     * @param t
     * @param dEncrypting
     * @param dSigning
     */
    public record RSAPrivateKey( CompositeModuloMath.CompositeIntegerModulus m, BigInteger t, BigInteger dEncrypting, BigInteger dSigning ) {}


    /**
     * A complementary pair of RSA keys, one public, one private.
     *
     * @param publicKey
     * @param privateKey
     */
    public record RSAKeyPair( RSAPublicKey publicKey, RSAPrivateKey privateKey ) {}
}
