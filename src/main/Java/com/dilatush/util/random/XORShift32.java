package com.dilatush.util.random;

/**
 * This class implements all 648 variations (81 triplets of shift variations, and 8 generator variations) of the 32-bit pseudorandom number generator (PRNG) described in George
 * Marsaglia's paper <a href="https://www.jstatsoft.org/index.php/jss/article/view/v008i14/916"><i>Xorshift RNGs</i></a>.
 */
public class XORShift32 implements Randomish {

    // preloaded static table of all possible triplets...
    private final static Triplet[] TRIPLETS = initTriplets();

    // immutable state of this PRNG instance...
    private final int  generator;          // [0..7] for the 8 possible generator variants in George Marsaglia's paper...
    private final int  a;                  // the "a" value in the selected triplet from George Marsaglia's paper...
    private final int  b;                  // the "b" value...
    private final int  c;                  // the "c" value...
    private final int  insertZeroTrigger;  // the output value to insert a zero output after (or zero to never insert a zero)...

    // mutable state of the PRNG...
    private int     state;     // the current state of the PRNG...
    private boolean zeroNext;  // true if the next value returned by nextInt() should be the inserted zero, and the next state value...


    /**
     * Create a new instance of this class using the given triplet index, generator index, initial state, and whether zero states should be inserted.
     *
     * @param _triplet Index for the triplet to be used; must be in the range [0..80].
     * @param _generator Index for the generator to be used; must be in the range [0..7].
     * @param _initialState The initial state of the RNG; may be any value other than zero.
     * @param _zeroInsertion If {@code false}, all possible integer values except zero will be returned from {@link #nextInt()} in the course of one cycle of the pseudorandom
     *                       cycle (so the cycle length in that case is 2^32 - 1).  If {@code true}, all possible integer values will be returned (so in that case the cycle
     *                       length is 2^32.  The zero output will be inserted as the return value for {@link #nextInt()} after a call to {@link #nextInt()} returns a "trigger"
     *                       value that is computed from the generator index, the triplet index, and the initial state.
     */
    public XORShift32( final int _triplet, final int _generator, final int _initialState, final boolean _zeroInsertion ) {

        // sanity checks...
        if( (_triplet < 0) || (_triplet > 80)    ) throw new IllegalArgumentException( "_triplet is out of allowed range [0..80]: " + _triplet    );
        if( (_generator < 0) || (_generator > 7) ) throw new IllegalArgumentException( "_generator is out of allowed range [0..7]: " + _generator );
        if( _initialState == 0                   ) throw new IllegalArgumentException( "_initialState may not be zero"                            );

        // we have good parameters, so initialize our new instance...
        a                 = TRIPLETS[_triplet].a;
        b                 = TRIPLETS[_triplet].b;
        c                 = TRIPLETS[_triplet].c;
        insertZeroTrigger = calcInsertZeroTrigger( _zeroInsertion );
        generator         = _generator;
        state             = _initialState;
        zeroNext          = false;
    }


    /**
     * Create a new instance of this class that will not insert zeroes, using the given triplet index, generator index, and initial state.
     *
     * @param _triplet Index for the triplet to be used; must be in the range [0..80].
     * @param _generator Index for the generator to be used; must be in the range [0..7].
     * @param _initialState The initial state of the RNG; must not be zero.
     */
    public XORShift32( final int _triplet, final int _generator, final int _initialState ) {
        this( _triplet, _generator, _initialState, false );
    }


    /**
     * Returns the cycle length of the pseudorandom sequence provided by this instance, defined somewhat arbitrarily as the number of invocations of {@link #nextInt()} between the
     * start of a pattern of 10 integers and the start of the next repetition of those same 10 integers.
     *
     * @return the cycle length of the pseudorandom sequence provided by this instance.
     */
    @Override
    public double cycleLength() {
        return (insertZeroTrigger == 0) ? Math.pow( 2d, 32d ) - 1 : Math.pow( 2d, 32d );
    }


    private int calcInsertZeroTrigger( final boolean _zeroInsertion ) {

        // if we're not doing zero insertion, then the trigger is zero (which will, of course, never be reached)...
        if( !_zeroInsertion ) return 0;

        // otherwise, we munge our parameters until we get a non-zero trigger value...
        var izt = 0;
        var munge = (generator << 15) | (a << 10) | (b << 5) | c;
        do {
            izt = munge ^ state;
            munge = Integer.rotateLeft( munge, 1 );
        } while( izt == 0 );
        return izt;
    }


    /**
     * Returns the next integer in the pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the random or pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        // if it's time to insert a zero, do so...
        if( zeroNext ) {
            zeroNext = false;
            return 0;
        }

        // These generators are all lifted straight from George Marsaglia's paper, referenced in the javadoc for this class...
        switch( generator ) {

            case 0 -> {
                state ^= (state <<  a);
                state ^= (state >>> b);
                state ^= (state <<  c);
            }

            case 1 -> {
                state ^= (state <<  c);
                state ^= (state >>> b);
                state ^= (state <<  a);
            }

            case 2 -> {
                state ^= (state >>> a);
                state ^= (state <<  b);
                state ^= (state >>> c);
            }

            case 3 -> {
                state ^= (state >>> c);
                state ^= (state <<  b);
                state ^= (state >>> a);
            }

            case 4 -> {
                state ^= (state <<  a);
                state ^= (state <<  c);
                state ^= (state >>> b);
            }

            case 5 -> {
                state ^= (state <<  c);
                state ^= (state <<  a);
                state ^= (state >>> b);
            }

            case 6 -> {
                state ^= (state >>> a);
                state ^= (state >>> c);
                state ^= (state <<  b);
            }

            case 7 -> {
                state ^= (state >>> c);
                state ^= (state >>> a);
                state ^= (state <<  b);
            }

            default -> throw new IllegalStateException( "Invalid generator value: " + generator );
        }

        // see if it's time to insert a zero on the next call...
        zeroNext = (state == insertZeroTrigger);

        return state;
    }


    private record Triplet( int a, int b, int c ){}


    /**
     * <p>Implements the 32 bit triplet table from George Marsaglia's Xorshift paper, but corrects one apparent typo in the table.  The 61st triplet is given in the paper as
     * (9,5,1), which is clearly wrong (as 9 > 1).  Empirically I discovered that (9,5,14) works, giving the full range of 2^32-1, so I substituted that triplet.  Then in an effort
     * to validate my finding, I searched and found <a href="https://forum.dlang.org/post/krejkp$1a6k$1@digitalmars.com">this reference</a> (relevant part excerpted below):</p>
     * <p><i>It's definitely not the only such typo in the paper -- Panneton and L'Ecuyer (2005) note that the (a, b, c) triple (9, 5, 1) should be (9, 5, 14).</i></p>
     * <p>I'm therefore feeling pretty safe that my correction is itself correct.</p>
     *
     * @return An array of 81 {@link Triplet}s of shift values, as taken from George Marsaglia's Xorshift paper.
     */
    private static Triplet[] initTriplets() {

        return new Triplet[] {
            new Triplet(  1, 3,10 ), new Triplet(  1, 5,16 ), new Triplet(  1, 5,19 ), new Triplet(  1, 9,29 ), new Triplet(  1,11, 6 ), new Triplet(  1,11,16 ),
            new Triplet(  1,19, 3 ), new Triplet(  1,21,20 ), new Triplet(  1,27,27 ), new Triplet(  2, 5,15 ), new Triplet(  2, 5,21 ), new Triplet(  2, 7, 7 ),
            new Triplet(  2, 7, 9 ), new Triplet(  2, 7,25 ), new Triplet(  2, 9,15 ), new Triplet(  2,15,17 ), new Triplet(  2,15,25 ), new Triplet(  2,21, 9 ),
            new Triplet(  3, 1,14 ), new Triplet(  3, 3,26 ), new Triplet(  3, 3,28 ), new Triplet(  3, 3,29 ), new Triplet(  3, 5,20 ), new Triplet(  3, 5,22 ),
            new Triplet(  3, 5,25 ), new Triplet(  3, 7,29 ), new Triplet(  3,13, 7 ), new Triplet(  3,23,25 ), new Triplet(  3,25,24 ), new Triplet(  3,27,11 ),
            new Triplet(  4, 3,17 ), new Triplet(  4, 3,27 ), new Triplet(  4, 5,15 ), new Triplet(  5, 3,21 ), new Triplet(  5, 7,22 ), new Triplet(  5, 9, 7 ),
            new Triplet(  5, 9,28 ), new Triplet(  5, 9,31 ), new Triplet(  5,13, 6 ), new Triplet(  5,15,17 ), new Triplet(  5,17,13 ), new Triplet(  5,21,12 ),
            new Triplet(  5,27, 8 ), new Triplet(  5,27,21 ), new Triplet(  5,27,25 ), new Triplet(  5,27,28 ), new Triplet(  6, 1,11 ), new Triplet(  6, 3,17 ),
            new Triplet(  6,17, 9 ), new Triplet(  6,21, 7 ), new Triplet(  6,21,13 ), new Triplet(  7, 1, 9 ), new Triplet(  7, 1,18 ), new Triplet(  7, 1,25 ),
            new Triplet(  7,13,25 ), new Triplet(  7,17,21 ), new Triplet(  7,25,12 ), new Triplet(  7,25,20 ), new Triplet(  8, 7,23 ), new Triplet(  8, 9,23 ),
            new Triplet(  9, 5,14 ), new Triplet(  9, 5,25 ), new Triplet(  9,11,19 ), new Triplet(  9,21,16 ), new Triplet( 10, 9,21 ), new Triplet( 10, 9,25 ),
            new Triplet( 11, 7,12 ), new Triplet( 11, 7,16 ), new Triplet( 11,17,13 ), new Triplet( 11,21,13 ), new Triplet( 12, 9,23 ), new Triplet( 13, 3,17 ),
            new Triplet( 13, 3,27 ), new Triplet( 13, 5,19 ), new Triplet( 13,17,15 ), new Triplet( 14, 1,15 ), new Triplet( 14,13,15 ), new Triplet( 15, 1,29 ),
            new Triplet( 17,15,20 ), new Triplet( 17,15,23 ), new Triplet( 17,15,26 )
        };
    }
}
