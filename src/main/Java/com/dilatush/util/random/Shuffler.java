package com.dilatush.util.random;


/**
 * This class permutes the order of the source pseudorandom sequence.  The idea is to reduce linear patterns in the source's sequence, and also to make analysis more
 * challenging.
 */
public class Shuffler implements Randomish {

    private final static int[] SHEAF_LENGTHS  = new int[] { 211, 223, 227, 229, 233, 239, 241, 251 };
    private final static int[] SKIP_DISTANCES = new int[] { 3, 5, 7, 11, 13, 17, 19, 23 };

    private final Randomish source;
    private final int       sheafLength;
    private final int       skipDistance;
    private final int[]     sheafBuffer;

    private int   sheafLoadIndex;    // the index to the next element to be loaded into the sheaf (and also the count of elements already loaded)...
    private int   sheafOutputIndex;  // the index to the next element of the sheaf to be output (skipDistance - 1 to start with)...
    private int   outputCount;


    public Shuffler( final Randomish _source, final int _sheaf, final int _skip ) {

        source = _source;
        sheafLength  = SHEAF_LENGTHS[_sheaf & 0x7];
        skipDistance = SKIP_DISTANCES[_skip  & 0x7];

        sheafLoadIndex   = 0;
        outputCount      = 0;
        sheafOutputIndex = skipDistance - 1;
        sheafBuffer      = new int[sheafLength];
    }


    /**
     * Returns the next integer in the pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        // if we've already output all the elements in a sheaf, then start over...
        if( outputCount >= sheafLength ) {
            sheafLoadIndex   = 0;
            outputCount      = 0;
            sheafOutputIndex = skipDistance - 1;
        }

        // if the sheaf buffer needs more data, then read some from the source...
        if( sheafLoadIndex < sheafLength ) {

            // load some data from the source - generally the skip distance, unless we fill the sheaf...
            var maxLoad = Math.min( skipDistance, sheafLength - sheafLoadIndex );
            for( int i = 0; i < maxLoad; i++ ) {
                sheafBuffer[ sheafLoadIndex++ ] = source.nextInt();
            }
        }

        // get our shuffled value, and update the indexes and counters...
        var result = sheafBuffer[sheafOutputIndex];
        sheafOutputIndex = (sheafOutputIndex + skipDistance) % sheafLength;
        outputCount++;

        return result;
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
