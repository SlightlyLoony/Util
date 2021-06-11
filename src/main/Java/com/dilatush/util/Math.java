package com.dilatush.util;

/**
 * Static container class for math-related functions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Math {


    /**
     * Given a normalized value {@code x} (with a value in [0..1]), returns the equivalent denormalized value in [a..b].  For instance, given an x
     * value of 0.25, and an a and b value for an interval of [5..9], this function will return 6.
     *
     * @param _x The normalized value (which must be finite and in the interval [0..1]) to denormalize.
     * @param _a The lower bound (inclusive) of the denormalized interval [a..b].  Must be finite and less than b.
     * @param _b The upper bound (inclusive) of the denormalized interval [a..b].  Must be finite and greater than a.
     * @return The equivalent denormalized value of x in the interval [a,b].
     */
    public static double denormalize( final double _x, final double _a, final double _b ) {

        // sanity checks...
        if( !Double.isFinite( _x ) || !Double.isFinite( _a ) || !Double.isFinite( _b )  )
            throw new IllegalArgumentException( "Arguments must all be finite (x, a, b): " + _x + ", " + _a + ", " + _b );
        if( (_x < 0) || (_x > 1))
            throw new IllegalArgumentException( "X is not normal (not in [0..1]):" + _x );
        if( (_b < _a) )
            throw new IllegalArgumentException( "Upper bound b (in [a..b]) is not greater than a (a, b): " + _a + ", " + _b );

        return _a * (1 - _x) + _b * _x;
    }


    /**
     * Given a denormalized value {@code x} with a value in the given interval [a..b], returns the equivalent normalized value in [0..1].  For
     * instance, given an x value of 6, and an a and b value for an interval of [5..9], this function will return 0.25.
     *
     * @param _x The denormalized value (which must be finite and in the interval [a..b]) to normalize.
     * @param _a The lower bound (inclusive) of the denormalized interval [a..b].  Must be finite and less than b.
     * @param _b The upper bound (inclusive) of the denormalized interval [a..b].  Must be finite and greater than a.
     * @return The equivalent normalized value of x in the interval [a..b].
     */
    public static double normalize( final double _x, final double _a, final double _b ) {

        // sanity checks...
        if( !Double.isFinite( _x ) || !Double.isFinite( _a ) || !Double.isFinite( _b )  )
            throw new IllegalArgumentException( "Arguments must all be finite (x, a, b): " + _x + ", " + _a + ", " + _b );
        if( (_b < _a) )
            throw new IllegalArgumentException( "Upper bound b (in [a..b]) is not greater than a (a, b): " + _a + ", " + _b );
        if( (_x < _a) || (_x > _b))
            throw new IllegalArgumentException( "X is not normal (not in [a..b]):" + _x );

        return (_x - _a) / (_b - _a);
    }


    /**
     * Given a denormalized value {@code x} with a value in the interval [a1..b1], returns the equivalent value denormalized into the interval
     * [a2..b2].  For instance, given an x value of 14 in the interval [a1..b1] of [10..28], and an interval [a2..b2] of [0..9], this function will
     * return 2.
     *
     * @param _x The denormalized value (which must be finite and in the interval [a..b]) to normalize.
     * @param _a1 The lower bound (inclusive) of the denormalized interval [a1..b1].  Must be finite and less than b1.
     * @param _b1 The upper bound (inclusive) of the denormalized interval [a1..b1].  Must be finite and greater than a1.
     * @param _a2 The lower bound (inclusive) of the denormalized interval [a2..b2].  Must be finite and less than b2.
     * @param _b2 The upper bound (inclusive) of the denormalized interval [a2..b2].  Must be finite and greater than a2.
     * @return The equivalent denormalized value of x in the interval [a2..b2].
     */
    @SuppressWarnings( "unused" )
    public static double renormalize( final double _x, final double _a1, final double _b1, final double _a2, final double _b2 ) {

        // sanity checks...
        if( !Double.isFinite( _x ) || !Double.isFinite( _a1 ) || !Double.isFinite( _b1 ) || !Double.isFinite( _a2 ) || !Double.isFinite( _b2 )  )
            throw new IllegalArgumentException( "Arguments must all be finite (x, a1, b1, a2, b2): " + _x + ", " + _a1 + ", " + _b1 + ", " + _a2 + ", " + _b2 );
        if( (_b1 < _a1) )
            throw new IllegalArgumentException( "Upper bound b1 (in [a1..b1]) is not greater than a1 (a1, b1): " + _a1 + ", " + _b1 );
        if( (_b2 < _a2) )
            throw new IllegalArgumentException( "Upper bound b2 (in [a2..b2]) is not greater than a2 (a2, b2): " + _a2 + ", " + _b2 );
        if( (_x < _a1) || (_x > _b1))
            throw new IllegalArgumentException( "X is not normal (not in [a1..b1]):" + _x );

        return denormalize( normalize( _x, _a1, _b1), _a2, _b2 );
    }
}
