package com.dilatush.util.random;

import java.math.BigInteger;

public interface Randomish {

    static BigInteger TWO_TO_THIRTY_TWO = BigInteger.valueOf( 0x1_0000_0000L );

    /**
     * Returns the next integer in the random or pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the random or pseudorandom sequence.
     */
    int nextInt();


    /**
     * <p>Returns the cycle length of the random or pseudorandom sequence provided by this instance.  Note that the result may be the exact length, a minimum length, an infinite
     * length (for a source than never repeats a cycle), or even an unknown length (for a source that repeats cyclically but where the length of the cycle is not computable).<p>
     * <p>Cycle length is defined somewhat arbitrarily as the number of invocations of {@link #nextInt()} between the start
     * of a pattern of 10 integers and the start of the next repetition of those same 10 integers</p>.
     *
     * @return the cycle length of the random or pseudorandom sequence provided by this instance.
     */
    CycleLength cycleLength();
}
