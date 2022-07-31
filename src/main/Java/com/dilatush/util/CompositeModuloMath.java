package com.dilatush.util;


import java.math.BigInteger;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for functions performing math modulo n, where n is not a prime, but is the product of two primes p and q.  This special case allows
 * higher performance math by taking advantage of the Chinese Remainder Theorem (see <i>Cryptography Engineering</i> section 12.2 <i>The Chinese Remainder Theorem</i>).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class CompositeModuloMath {


    /**
     * Represents the composite integer modulus n and its two prime factors p and q (that is, n = p * q), along with the Garner's formula pre-computed factor gf = (1/q) mod p.
     */
    public record CompositeIntegerModulus( BigInteger n, BigInteger p, BigInteger q, BigInteger gf ) {

        public CompositeIntegerModulus {

            // sanity checks...
            if( isNull( n, p, q, gf ) )
               throw new IllegalArgumentException( "n, p, q, or gf is null" );

            // make sure n, p, and q are all non-zero and positive...
            if( (n.signum() != 1) || (p.signum() != 1) || (q.signum() !=1) )
                throw new IllegalArgumentException( "at least one of n, p, or q is negative or zero");
        }


        /**
         * Creates a new instance of this record with the given parameters.  Throws an {@link IllegalArgumentException} if any parameters are {@code null}, or if any parameters are
         * less than 1.  The parameters p and q are <i>not</i> checked for prime, and the parameter n is <i>not</i> checked for being the product of p and q.  This constructor
         * computes the Garner's formula factor gf as (1/q) mod p.
         *
         * @param n The composite integer modulus, which has exactly two prime factors.
         * @param p One of the prime factors of n.
         * @param q One of the prime factors of n.
         */
        public CompositeIntegerModulus( BigInteger n, BigInteger p, BigInteger q ) {
            this( n, p, q, BigIntegers.divMod( BigInteger.ONE, q, p ) );
        }
    }


    /**
     * Represents a number x mod n represented as the pair (a mod p, b mod q) per the Chinese Remainder Theorem (see <i>Cryptography Engineering</i> section 12.2 <i>The Chinese
     * Remainder Theorem</i>).
     */
    public record XCRT( CompositeIntegerModulus m, BigInteger a, BigInteger b ) {


        /**
         * Converts the given x mod n to the pair (a mod p, b mod q) per the Chinese Remainder Theorem (see <i>Cryptography Engineering</i> section 12.2 <i>The Chinese
         * Remainder Theorem</i>).
         *
         * @param m the composite integer modulus n and its two prime factors p and q.
         * @param x the number mod n.
         */
        public XCRT( CompositeIntegerModulus m, BigInteger x ) {

            this( m, x.mod( m.p ), x.mod( m.q ) );
        }


        /**
         * Computes x modulo n from the Chinese Remainder Theorem (CRT) representation as (a modulo p, a modulo q).  For example, given the composite modulus 6 = 2 * 3 and the pair
         * (1 modulo 2, 2 modulo 3), it returns 5 (modulo 6).
         *
         * @return The number modulo m.n.
         */
        public BigInteger deCRT() {

            // compute the result using Garner's formula: x = (((a - b)((1/q) mod p)) mod p) * q + b...
            return a.subtract( b ).mod( m.p ).multiply( m.gf ).mod( m.p ).multiply( m.q ).add( b );
        }


        public XCRT multiply( final XCRT _y ) {

            // sanity check...
            if( isNull( _y ) )
                throw new IllegalArgumentException( "_y is null" );

            // do the multiplication...
            var axy = a.multiply( _y.a ).mod( m.p );
            var bxy = b.multiply( _y.b ).mod( m.q );

            // return the new instance with the product...
            return new XCRT( m, axy, bxy );
        }
    }


    /**
     * Returns the given integer x raised to the given integer power exp, modulo the composite modulus mod.  This method uses the Chinese Remainder Theory
     * (per <i>Cryptography Engineering</i> page 198) to optimize the exponentiation.
     *
     * @param _x   The integer to raise to a power.
     * @param _exp The integer power to raise x to.
     * @param _mod The modulus of the result; the modulus must be the product of two primes.
     * @return The given integer x raised to the given integer power exp, modulo the composite modulus mod.
     */
    public static BigInteger pow( final BigInteger _x, final BigInteger _exp, final CompositeIntegerModulus _mod ) {

        // sanity checks...
        if( isNull( _x, _exp, _mod ) )
            throw new IllegalArgumentException( "_x, _exp, or _mod is null" );

        // first we reduce the exponent for each factor of our composite modulus...
        var pExp = _exp.mod( _mod.p.subtract( BigInteger.ONE ) );
        var qExp = _exp.mod( _mod.q.subtract( BigInteger.ONE ) );

        // next we raise our number to each factor's reduced power...
        var a = _x.modPow( pExp, _mod.p );
        var b = _x.modPow( qExp, _mod.q );

        // construct our result in CRT form...
        var result = new XCRT( _mod, a, b );

        // now return the result as an integer, and we're done...
        return result.deCRT();
    }
}
