package com.dilatush.util;

import java.math.BigInteger;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for functions related to instances of {@link BigInteger}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
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
}
