package com.dilatush.util.random;

/**
 * Some {@link Randomish} sources of pseudorandom integers (for instance, {@link XORShift32}), when treated as though their output was unsigned, include only non-zero integer.
 * For some purposes, this is undesirable.  This class fixes the issue through the simple expedient of subtracting one from the values (treated as unsigned) produced by the source.
 */
public class RangeCorrectorUnsigned implements Randomish {


    private final Randomish source;


    public RangeCorrectorUnsigned( final Randomish _source ) {
        source = _source;
    }

    /**
     * Returns the next integer in the pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        return (int) ((0xFFFFFFFFL & (long)source.nextInt()) - 1);
    }
}
