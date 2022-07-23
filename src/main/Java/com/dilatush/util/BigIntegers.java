package com.dilatush.util;

import java.math.BigInteger;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for functions related to instances of {@link BigInteger}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class BigIntegers {


    /**
     * Returns the least common multiple (LCM) of the two given integers.  If both the given integers are equal to zero, returns a zero.
     *
     * @param _a One of the integers
     * @param _b The other integer
     * @return The least common multiple of the absolute value of the two given integers.
     */
    public static BigInteger lcm( final BigInteger _a, final BigInteger _b ) {

        // sanity check...
        if( isNull( _a, _b ) )
            throw new IllegalArgumentException( "missing lcm argument(s)" );

        // if the gcd is zero, return zero...
        BigInteger gcd =  _a.gcd( _b );
        if( gcd.signum() == 0 )
            return BigInteger.ZERO;

        // LCM(a, b) = (a x b) / GCD(a, b)
        return _a.abs().multiply( _b.abs() ).divide( gcd );
    }


    /**
     * Computes the following information about the absolute values of the two given integers:
     * <ul>
     *     <li>The greatest common divisor (GCD) - the largest integer by which the two given integers (a and b) are evenly divisible.</li>
     *     <li>The coefficients of <a href="https://en.wikipedia.org/wiki/B%C3%A9zout%27s_identity">Bézout's identity</a> such that ax + by = GCD( a, b ).</li>
     *     <li>The quotients of the two given integers (a and b) divided by their GCD.</li>
     * </ul>
     * <p>This method uses the <a href="https://en.wikipedia.org/wiki/Extended_Euclidean_algorithm">extended Euclidean algorithm</a> to calculate all the return values.</p>
     *
     * @param _a One of the given integers
     * @param _b The other given integer
     * @return An instance of the record {@link EGCD}, containing the values computed by this method.
     */
    public static EGCD extendedGCD( final BigInteger _a, final BigInteger _b ) {

        // sanity check...
        if( isNull( _a, _b ) )
            throw new IllegalArgumentException( "_a or _b is null" );

        // some setup...
        var r1 = _b.abs();
        var r2 = _a.abs();
        var s1 = BigInteger.ZERO;
        var s2 = BigInteger.ONE;
        var t1 = BigInteger.ONE;
        var t2 = BigInteger.ZERO;

        // iterate until r1 == 0...
        while( r1.signum() != 0 ) {

            // compute the quotient for this iteration...
            var q = r2.divide( r1 );

            // calculate the results for this iteration...
            var rx = r1;
            r1 = r2.subtract( q.multiply( r1 ) );
            r2 = rx;
            var sx = s1;
            s1 = s2.subtract( q.multiply( s1 ) );
            s2 = sx;
            var tx = t1;
            t1 = t2.subtract( q.multiply( t1 ) );
            t2 = tx;
        }

        // we're done, so return our answers...
        return new EGCD( r2, s2, t2, t1.abs(), s1.abs() );
    }


    /**
     * The results of an extended GCD computation on the absolute values of two integers (a and b).  The fields in this record (all {@link BigInteger}s) are:
     * <ul>
     *     <li><i>gcd</i> - the greatest common divisor (GCD) of a and b.</li>
     *     <li><i>bcx</i> - the Bézout's identity coefficient x, where ax + by = GCD( a, b ).</li>
     *     <li><i>bcy</i> - the Bézout's identity coefficient y, where ax + by = GCD( a, b ).</li>
     *     <li><i>qax</i> - a / GCD( a, b ).</li>
     *     <li><i>qby</i> - b / GCD( a, b ).</li>
     * </ul>
     */
    public record EGCD( BigInteger gcd, BigInteger bcx, BigInteger bcy, BigInteger qax, BigInteger qby ) {}


    /**
     * <p>Returns (_a / _b) mod _m, where _m is a prime number.  This method does not check for _m being a prime number.  To divide modulo a composite number, factor the number to
     * its prime factors, do the division for each of those, then multiply the results modulo _m.</p>
     * <p>The author has no clue why this works; he got the algorithm from Cryptography Engineering, section 10.3.5 on the Extended Euclidean Algorithm, page 171.</p>
     *
     * @param _a The dividend.
     * @param _b The divisor.
     * @param _m The modulus, which must be a prime number.
     * @return The quotient (_a / _b) mod _m.
     */
    public static BigInteger divMod( final BigInteger _a, final BigInteger _b, final BigInteger _m ) {

        // find the Bézout's identity coefficient x in the results from the extended GCD of _b, _m...
        BigInteger u = extendedGCD( _b, _m ).bcx;

        // multiply by _a if _a != 1)...
        return (BigInteger.ONE.compareTo( _a ) == 0) ? u : _a.multiply( u ).mod( _m );
    }
}
