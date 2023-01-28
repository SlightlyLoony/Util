package com.dilatush.util.random;

/**
 * Instances of this class "mix" two or more {@link Randomish} pseudorandom sequences together to produce a high quality pseudorandom sequence with a longer cycle length.  The
 * mixing is performed by XORing the {@link #nextInt()} result from each {@link Randomish} source to produce the next {@link #nextInt()} output from an instance of this class.
 */
public class XORMixer implements Randomish {


    /**
     * Create a new instance of this class using the given sources.
     *
     * @param _sources
     */
    public XORMixer( final Randomish... _sources ) {

    }


    /**
     * Returns the next integer in the random or pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the random or pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        return 0;
    }


    /**
     * <p>Returns the cycle length of the random or pseudorandom sequence provided by this instance.  Note that the result may be the exact length, a minimum length, an infinite
     * length (for a source than never repeats a cycle), or even an unknown length (for a source that repeats cyclically but where the length of the cycle is not computable).<p>
     * <p>Cycle length is defined somewhat arbitrarily as the number of invocations of {@link #nextInt()} between the start
     * of a pattern of 10 integers and the start of the next repetition of those same 10 integers</p>.
     *
     * @return the cycle length of the random or pseudorandom sequence provided by this instance.
     */
    @Override
    public CycleLength cycleLength() {

        return null;
    }
}
