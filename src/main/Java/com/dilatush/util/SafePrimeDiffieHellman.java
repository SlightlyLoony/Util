package com.dilatush.util;


import javax.crypto.spec.DHParameterSpec;

import java.math.BigInteger;
import java.security.SecureRandom;

import static com.dilatush.util.General.isNull;

/**
 * Provides an implementation of the Diffie-Hellman key exchange protocol, where the p and g parameters are generated using the procedure given in "Cryptography Engineering",
 * in the section on safe primes for Diffie-Hellman parameters starting on page 186.  The overriding goal for this implementation is to provide a safe, thoroughly checked
 * key exchange.
 */
public class SafePrimeDiffieHellman {

    private final Outcome.Forge<Object> forgeObject = new Outcome.Forge<>();

    private BigInteger p;
    private BigInteger g;
    private int l;


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
        BigInteger[] tq = tp.subtract( BigInteger.ONE ).divideAndRemainder( BigInteger.TWO );
        if( tq[1].compareTo( BigInteger.ZERO ) != 0 )
            return forgeObject.notOk( "p is not in the form 2q + 1" );
        if( !tq[0].isProbablePrime( 100 ) )
            return forgeObject.notOk( "q is not a prime number" );

        // make sure that g is less than p, is positive, and is a square modulo p...
        BigInteger tg = _spec.getG();
        if( tg.compareTo( tp ) >= 0 )
            return forgeObject.notOk( "g is not smaller than p" );
        if( tg.signum() != 1 )
            return forgeObject.notOk( "g is not a positive integer" );
        if( Crypto.legendreSymbol( tg, tp ) != 1 )
            return forgeObject.notOk( "g modulo p is not a square" );


        // if we make it here, then our parameters check out, so it's time to save them...
        p = tp;
        g = tg;
        l = _spec.getL();

        return forgeObject.ok();
    }


    public static void main( final String[] _args ) {
        DHParameterSpec dhps = Crypto.getDHParameters( 512, new SecureRandom() );
        SafePrimeDiffieHellman sfdh = new SafePrimeDiffieHellman();
        Outcome<Object> setParametersOutcome = sfdh.setParameters( dhps );

        dhps.hashCode();
    }
}
