package com.dilatush.util.random;

public interface Randomish {

    /**
     * Returns the next integer in the random or pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the random or pseudorandom sequence.
     */
    int nextInt();


    /**
     * Returns the cycle length of the random or pseudorandom sequence provided by this instance.  If the sequence is a truly random sequence, positive infinity will be returned.
     * Otherwise, for a pseudorandom sequence it returns the actual cycle length (defined somewhat arbitrarily as the number of invocations of {@link #nextInt()} between the start
     * of a pattern of 10 integers and the start of the next repetition of those same 10 integers).
     *
     * @return the cycle length of the random or pseudorandom sequence provided by this instance.
     */
    double cycleLength();
}
