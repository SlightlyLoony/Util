package com.dilatush.util;


import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;
import java.security.SecureRandom;

import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.General.isNull;

/**
 * <p>Provides an implementation of the Diffie-Hellman key exchange protocol, where the p and g parameters have been generated using the procedure given in "Cryptography
 * Engineering", in the section on safe primes for Diffie-Hellman parameters starting on page 186.  The overriding goal for this implementation is to provide a safe, thoroughly
 * checked key exchange.</p>
 * <p>The key exchange protocol has two "sides".  Usually that means two computers connected by a network, but it could be any two processes that need a shared
 * secret.  For the discussion below, we use "this side" to refer to the process using an instance of this class, and "the other side" to refer to another
 * process implementing the same protocol, possibly with another instance of this class.  To use this class, follow these steps in order:</p>
 * <ol>
 *     <li>Create a new instance of this class for this side.</li>
 *     <li>Set the Diffie-Hellman parameters (p, g, and bit length) for this side using the {@link #setParameters(DHParameterSpec)} method.  Note that both this side
 *     and the other side must use identical Diffie-Hellman parameters.</li>
 *     <li>Generate the private and public keys for this side using the {@link #generateKeys()} method.  The generated keys are stored in this instance.</li>
 *     <li>Get the public key (using the {@link #getPublicKey()} method) and send it to the other side.</li>
 *     <li>Get the other side's public key and set it in this instance, using the {@link #setOtherSidePublicKey(BigInteger)} method.</li>
 *     <li>Get the shared secret, using the {@link #getSharedSecret()} method.</li>
 * </ol>
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.</p>
 */
public class SafePrimeDiffieHellman {

    // forges for all of this class' possible outcome types...
    private final Outcome.Forge<Object>     forgeObject     = new Outcome.Forge<>();
    private final Outcome.Forge<BigInteger> forgeBigInteger = new Outcome.Forge<>();

    // the source of randomness for this instance...
    private final SecureRandom secureRandom;

    /**
     * The Diffie-Hellman "p" parameter: the modulus of the shared secret.  It is set <i>only</i> by the {@link #setParameters(DHParameterSpec)} method.  This will be {@code null}
     * if it hasn't been set, or it will be a positive integer prime number that contains the number of bits specified in the "l"
     * parameter.  Note that this class will not allow p values with less than 512 bits.  The p value will also be a "safe" prime, which means that (p - 1)/2 (the "q" value)
     * will also be a positive prime number.  For example, 11 is a safe prime because (11 - 1)/2 = 5, which is also a prime.  However, 13 is not a safe prime because (13 - 1)/2 = 6
     * which is not a prime.
     */
    private BigInteger p;

    /**
     * The Diffie-Hellman "g" parameter: the generator for the shared secret.  It is set <i>only</i> by the {@link #setParameters(DHParameterSpec)} method.  This will be
     * {@code null} if it hasn't been set, or it will be a positive integer in the range [2..p-2] that is a square modulo p.
     */
    private BigInteger g;

    /**
     * The bit length of p, and the shared secret.  It is set <i>only</i> by the {@link #setParameters(DHParameterSpec)} method.  This will be zero if it hasn't been set, or
     * it will be the number of bits in p.
     */
    private int l;


    /**
     * The private key.  This will be a {@code null} if the {@link #generateKeys()} method has not been called, or it will be the integer private key in the range [1..p-1].  It is
     * selected randomly in the {@link #generateKeys()} method.
     */
    private BigInteger privateKey;

    /**
     * The public key.  This will be a {@code null} if the {@link #generateKeys()} method has not been called, or it will be the integer private key in the range [1..p-1].  It is
     * computed in the {@link #generateKeys()} method as g^(private key) modulo p.
     */
    private BigInteger publicKey;

    /**
     * The public key from the other side.
     */
    private BigInteger otherSidePublicKey;

    /**
     * The computed shared secret.
     */
    private BigInteger sharedSecret;


    /**
     * Creates a new instance of this class with the given source of random numbers.
     *
     * @param _secureRandom The {@link SecureRandom} source of random numbers.
     */
    public SafePrimeDiffieHellman( final SecureRandom _secureRandom ) {

        // sanity check...
        if( isNull( _secureRandom ) )
            throw new IllegalArgumentException( "missing source of randomness" );

        secureRandom = _secureRandom;
    }

    /**
     * <p>Set the Diffie-Hellman parameters (p, g, and p's bit length) for this instance.  The other side of the key exchange must be using exactly the same parameters.</p>
     * <p>This method performs the following checks, in order.  If a check fails, this method exits with "not ok" and does nothing else.</p>
     * <ol>
     *     <li>Fail if parameters have already been set. The existing parameters are left unchanged.</li>
     *     <li>Fail if the given {@link DHParameterSpec} is {@code null}, or if the p or g parameter is {@code null}.</li>
     *     <li>Fail if the given bit length is less than 512 bits.</li>
     *     <li>Fail if the bit length parameter is not equal to the bit length of the p parameter.</li>
     *     <li>Fail if the given p parameter is not a positive integer.</li>
     *     <li>Fail if the given p parameter is not a prime number.</li>
     *     <li>Fail if q (computed as (p - 1)/2)) is not a prime number.</li>
     *     <li>Fail if g is not smaller than p - 1.</li>
     *     <li>Fail if g is not larger than 1.</li>
     *     <li>Fail if g modulo p is not a square.</li>
     * </ol>
     * <p>If the given parameters pass all of these checks, then the parameters are set.</p>
     *
     * @param _spec The Diffie-Hellman parameters to set.
     * @return The outcome of the operation, either "ok" (meaning the new parameters have been set), or "not ok" (meaning the new parameters have not been set, and an
     *         explanatory string is included in the outcome.
     */
    public Outcome<Object> setParameters( final DHParameterSpec _spec ) {

        // sanity checks...
        if( !isNull( p ) || !isNull( g ) || (l != 0) )
            return forgeObject.notOk( "parameters have already been set" );
        if( isNull( _spec ) )
            return forgeObject.notOk( "_spec is null" );
        if( isNull( _spec.getP() ) )
            return forgeObject.notOk( "p is null" );
        if( isNull( _spec.getG() ) )
            return forgeObject.notOk( "g is null" );
        if( _spec.getL() < 512 )
            return forgeObject.notOk( "specified bit length of p is less than 512 bits" );
        if( _spec.getL() != _spec.getP().bitLength() )
            return forgeObject.notOk( "bit length of p does not match l" );

        // make sure that p has the form 2q + 1, where both p and q are primes...
        BigInteger tp = _spec.getP();
        if( tp.signum() != 1)
            return forgeObject.notOk( "p is not a positive integer" );
        if( !tp.isProbablePrime( 100 ) )
            return forgeObject.notOk( "p is not a prime number" );
        BigInteger tq = tp.shiftRight( 1 );  // we know this is safe, as p is a prime and therefore is odd so this computes (p - 1)/2...
        if( !tq.isProbablePrime( 100 ) )
            return forgeObject.notOk( "q ((p - 1)/2) is not a prime number" );

        // make sure that g is less than p, is positive, and is a square modulo p...
        BigInteger tg = _spec.getG();
        if( tg.compareTo( tp.subtract( BigInteger.ONE ) ) >= 0 )
            return forgeObject.notOk( "g is not smaller than p - 1" );
        if( tg.compareTo( BigInteger.ONE ) <= 0 )
            return forgeObject.notOk( "g is not larger than one" );
        if( Crypto.legendreSymbol( tg, tp ) != 1 )
            return forgeObject.notOk( "g modulo p is not a square" );


        // if we make it here, then our parameters check out, so it's time to save them...
        p = tp;
        g = tg;
        l = _spec.getL();

        return forgeObject.ok();
    }


    /**
     * Generates the private key (randomly chosen in the range [1..p-1]) and the public key (computed as g^(private key) modulo p), saves both keys in this instance, and returns
     * an "ok".  If the parameters have not been set, or if the keys have already been generated, returns a "not ok" with an explanatory message.
     *
     * @return The outcome of the operation, "ok" if the keys have been generated and "not ok" (with an explanatory message) if they were not.
     */
    public Outcome<Object> generateKeys() {

        // make sure we have parameters...
        if( isNull( p, g ) || (l == 0) )
            return forgeObject.notOk( "parameters have not been set" );

        // make sure we haven't already generated keys...
        if( isNotNull( privateKey, publicKey ) )
            return forgeObject.notOk( "keys have already been generated" );

        // all is well, so let's pick a random private key in the range [1..p-1]...
        do {
            privateKey = new BigInteger( l, secureRandom );
        } while( (privateKey.compareTo( BigInteger.ONE ) < 0) || (privateKey.compareTo( p ) >= 0) );

        // and then compute the public key...
        publicKey = g.modPow( privateKey, p );

        return forgeObject.ok();
    }


    /**
     * Returns the public key generated by calling {@link #generateKeys()}. If the keys have not been generated for any reason, returns a "not ok" with an explanatory message.
     *
     * @return The outcome of the operation, "ok" with the public key, or "not ok" with no key and an explanatory message.
     */
    public Outcome<BigInteger> getPublicKey() {

        // make sure we actually have generated keys...
        if( isNull( publicKey ) )
            return forgeBigInteger.notOk( "keys have not been generated" );

        return forgeBigInteger.ok( publicKey );

    }


    /**
     * <p>Sets the other side public key to the given public key.  This method performs the following checks, in order.  If a check fails, this method exits with "not ok" and
     * does nothing else.</p>
     * <ol>
     *     <li>Fail if the other side public key has already been set.</li>
     *     <li>Fail if the given other side public key is {@code null}.</li>
     *     <li>Fail if the given other side public key is not in the range [1..p-1].</li>
     *     <li>Fail if the given other side public key is not a square modulo p.</li>
     * </ol>
     * <p></p>
     *
     * @param _publicKey
     * @return
     */
    public Outcome<Object> setOtherSidePublicKey( final BigInteger _publicKey ) {

        return null;

    }


    public Outcome<BigInteger> getSharedSecret() {

        return null;

    }


    public static void main( final String[] _args ) {

        // some setup...
        SecureRandom secureRandom = new SecureRandom();
        DHParameterSpec dhps = Crypto.getDHParameters( 512, secureRandom );

        // this side...
        SafePrimeDiffieHellman tsspdh = new SafePrimeDiffieHellman( secureRandom );
        Outcome<Object> setParametersOutcome = tsspdh.setParameters( dhps );
        if( setParametersOutcome.notOk()) throw new IllegalStateException( "this side setParameter: " + setParametersOutcome.msg() );
        Outcome<Object> generateOutcome = tsspdh.generateKeys();
        if( generateOutcome.notOk() ) throw new IllegalStateException( "this side generateKeys: " + generateOutcome.msg() );
        Outcome<BigInteger> thisSideGetPublicKeyOutcome = tsspdh.getPublicKey();
        if( thisSideGetPublicKeyOutcome.notOk() ) throw new IllegalStateException( "this side getPublicKey: " + thisSideGetPublicKeyOutcome.msg() );
        BigInteger thisSidePublicKey = thisSideGetPublicKeyOutcome.info();

        // other side...
        SafePrimeDiffieHellman osspdh = new SafePrimeDiffieHellman( secureRandom );
        setParametersOutcome = osspdh.setParameters( dhps );
        if( setParametersOutcome.notOk()) throw new IllegalStateException( "other side setParameter: " + setParametersOutcome.msg() );
        generateOutcome = osspdh.generateKeys();
        if( generateOutcome.notOk() ) throw new IllegalStateException( "other side generateKeys: " + generateOutcome.msg() );
        Outcome<BigInteger> otherSideGetPublicKeyOutcome = osspdh.getPublicKey();
        if( otherSideGetPublicKeyOutcome.notOk() ) throw new IllegalStateException( "other side getPublicKey: " + thisSideGetPublicKeyOutcome.msg() );
        BigInteger otherSidePublicKey = otherSideGetPublicKeyOutcome.info();

        // this side...


        dhps.hashCode();
    }
}
