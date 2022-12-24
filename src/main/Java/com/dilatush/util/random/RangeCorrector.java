package com.dilatush.util.random;

/**
 * Some {@link Randomish} sources of pseudorandom integers (for instance, {@link XORShift32}) include positive and negative integers, but not zero.  For some purposes, this
 * discontinuity in the pseudorandom output is undesirable.  This class fixes the issue through the simple expedient of adding one to any negative integers produced by the source.
 * Thus, -1 becomes 0, -11 becomes -10, etc.  The result is a continuous range from the smallest negative number through the largest positive number in the sequence's range.
 */
public class RangeCorrector implements Randomish {


    private final Randomish source;


    public RangeCorrector( final Randomish _source ) {
        source = _source;
    }

    /**
     * Returns the next integer in the pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        var n = source.nextInt();

        // This tricksy looking code transforms the range of the result from discontinuous at zero to continuous from the smallest negative number through the largest positive
        // number in the sequence's range...
        // All it really does is add 1 to any negative values and return positive values unmodified...
        return (n >= 0) ? n : n + 1;
    }
}
