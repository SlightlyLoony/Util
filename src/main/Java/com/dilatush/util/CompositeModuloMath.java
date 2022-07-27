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
     * Computes x modulo n from the Chinese Remainder Theorem (CRT) representation as (a modulo p, a modulo q).  For example, given the composite modulus 6 = 2 * 3 and the pair
     * (1 modulo 2, 2 modulo 3), it returns 5 (modulo 6).
     *
     * @param _m The composite integer modulus.
     * @param _a The number modulo _m.p.
     * @param _b The number modulo _m.q.
     * @return The number modulo _m.n.
     */
    public static BigInteger deCRT( final CompositeIntegerModulus _m, final BigInteger _a, final BigInteger _b ) {

        // sanity check...
        if( isNull( _m, _a, _b ) )
            throw new IllegalArgumentException( "_m, _a, or _b is null" );

        // compute the result using Garner's formula: x = (((a - b)((1/q) mod p)) mod p) * q + b...
        return _a.subtract( _b ).mod( _m.p ).multiply( _m.gf ).mod( _m.p ).multiply( _m.q ).add( _b );
    }


    public static BigInteger multiply( final CompositeIntegerModulus _m, final BigInteger _x, final BigInteger _y ) {

        // sanity check...
        if( isNull( _m, _x, _y ) )
            throw new IllegalArgumentException( "_m, _x, or _y is null" );

        // compute the pairs (ax, bx) and (ay, by) as (x mod p, x mod q) and (y mod p, y mod q)...
        var ax = _x.mod( _m.p );
        var bx = _x.mod( _m.q );
        var ay = _y.mod( _m.p );
        var by = _y.mod( _m.q );

        // do the multiplication...
        var axy = ax.multiply( ay ).mod( _m.p );
        var bxy = bx.multiply( by ).mod( _m.q );

        // convert it to modulo n and return...
        return deCRT( _m, axy, bxy );
    }


    public static BigInteger modPow( final CompositeIntegerModulus _m, final BigInteger _x, final BigInteger _y ) {

        // sanity check...
        if( isNull( _m, _x, _y ) )
            throw new IllegalArgumentException( "_m, _x, or _y is null" );

        // compute the pairs (ax, bx) and (ay, by) as (x mod p, x mod q) and (y mod p, y mod q)...
        var ax = _x.mod( _m.p );
        var bx = _x.mod( _m.q );
        var ay = _y.mod( _m.p );
        var by = _y.mod( _m.q );

        // do the multiplication...
        var axy = ax.multiply( ay ).mod( _m.p );
        var bxy = bx.multiply( by ).mod( _m.q );

        // convert it to modulo n and return...
        return deCRT( _m, axy, bxy );
    }


    /**
     * Represents the composite integer modulus n and its two prime factors p and q (that is, n = p * q).
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
         * computes the Garner's formula factor gf as 1/q mod p
         *
         * @param n The composite integer modulus, which has exactly two prime factors.
         * @param p One of the prime factors of n.
         * @param q One of the prime factors of n.
         */
        public CompositeIntegerModulus( BigInteger n, BigInteger p, BigInteger q ) {
            this( n, p, q, BigIntegers.divMod( BigInteger.ONE, q, p ) );
        }
    }
}
