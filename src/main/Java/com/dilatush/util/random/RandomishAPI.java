package com.dilatush.util.random;

public class RandomishAPI implements Randomish {

    private final Randomish source;

    private int bytesRemaining;
    private int bytesSource;


    public RandomishAPI( final Randomish _source ) {
        source = _source;
    }


    /**
     * Returns the next integer in the pseudorandom sequence provided by this instance. The integer's value will be in the range (-2^31..2^31).  Note that the range is exclusive,
     * and is not the same as the usual Java int range of [-2^31..2^31).  The returned value has 2^32-1 possible values, and is symmetrical around zero.
     *
     * @return The next integer in the pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        return source.nextInt();
    }


    public byte nextByte() {

        if( bytesRemaining == 0 ) {
            bytesSource = nextInt();
            bytesRemaining = 4;
        }

        byte result = (byte) bytesSource;
        bytesSource >>>= 8;
        bytesRemaining--;
        return result;
    }


    public byte[] getBytes( final byte[] _destination ) {

        for( int i = 0; i < _destination.length; i++ ) {
            _destination[i] = nextByte();
        }
        return _destination;
    }


    public long nextLong() {
        return (long)nextInt() << 32 | (long)nextInt();
    }


    /**
     * Returns the cycle length of the random or pseudorandom sequence provided by this instance.  If the sequence is a truly random sequence, positive infinity will be returned.
     * Otherwise, for a pseudorandom sequence it returns the actual cycle length (defined somewhat arbitrarily as the number of invocations of {@link #nextInt()} between the start
     * of a pattern of 10 integers and the start of the next repetition of those same 10 integers).
     *
     * @return the cycle length of the random or pseudorandom sequence provided by this instance.
     */
    @Override
    public double cycleLength() {

        return 0;
    }
}
