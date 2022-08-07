package com.dilatush.util;

import java.math.BigInteger;

import static com.dilatush.util.General.isNull;
import static java.math.BigInteger.*;

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
            return ZERO;

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

        // sanity checks...
        if( isNull( _a, _b ) )                       throw new IllegalArgumentException( "_a or _b is null" );
        if( (_a.signum() < 0) || (_b.signum() < 0) ) throw new IllegalArgumentException( "_a or _b is negative" );

        // some initialization...
        var c = _a;
        var d = _b;
        var uC = ONE;
        var vC = ZERO;
        var uD = ZERO;
        var vD = ONE;
        BigInteger scratch;

        // iterate until c has been reduced to zero...
        while( c.signum() != 0 ) {

            // at this point, (uC * a) + (vC * b) = c, and (uD * a) + (vD * b) = d...

            // Euclidean divide...
            var q = d.divide( c );

            // update the three pairs of variables (c,d), (uC,uD), (vC, vD)...

            // (c, d)...
            scratch = c;
            c = d.subtract( q.multiply( c ) );
            d = scratch;

            // (uC, uD)...
            scratch = uC;
            uC = uD.subtract( q.multiply( uC ) );
            uD = scratch;

            // (vC, vD)...
            scratch = vC;
            vC = vD.subtract( q.multiply( vC ) );
            vD = scratch;
        }

        // we've got our answers, so skedaddle with them...
        return new EGCD( d, uD, vD );
    }


    /**
     * The results of an extended GCD computation on the absolute values of two integers (a and b).  The fields in this record (all {@link BigInteger}s) are:
     * <ul>
     *     <li><i>gcd</i> - the greatest common divisor (GCD) of a and b.</li>
     *     <li><i>x</i> - the Bézout's identity coefficient x, where ax + by = GCD( a, b ).</li>
     *     <li><i>y</i> - the Bézout's identity coefficient y, where ax + by = GCD( a, b ).</li>
     * </ul>
     */
    public record EGCD( BigInteger gcd, BigInteger x, BigInteger y ) {}


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
        BigInteger u = extendedGCD( _b, _m ).x;

        // multiply by _a if _a != 1)...
        return (ONE.compareTo( _a ) == 0) ? u : _a.multiply( u ).mod( _m );
    }
}
