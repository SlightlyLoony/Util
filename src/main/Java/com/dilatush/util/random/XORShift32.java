package com.dilatush.util.random;

/**
 * This class implements all 648 variations (81 triplets of shift variations, and 8 generator variations) of the 32-bit pseudorandom number generator (PRNG) described in George
 * Marsaglia's paper <a href="https://www.jstatsoft.org/index.php/jss/article/view/v008i14/916"><i>Xorshift RNGs</i></a>.
 */
public class XORShift32 implements Randomish {
    
    private final static Triplet[] TRIPLETS = initTriplets();

    private final int  generator;  // [0..7] for the 8 possible generator variants in George Marsaglia's paper...
    private final int  a;
    private final int  b;
    private final int  c;

    private int state;             // the current state of the RNG...


    /**
     *
     * @param _triplet Index for the triplet to be used; must be in the range [0..80].
     * @param _generator Index for the generator to be used; must be in the range [0..7].
     * @param _initalState The initial state of the RNG; must not be zero.
     */
    public XORShift32( final int _triplet, final int _generator, final int _initalState ) {

        a = TRIPLETS[_triplet].a;
        b = TRIPLETS[_triplet].b;
        c = TRIPLETS[_triplet].c;

        generator = _generator;
        state = _initalState;
    }


    /**
     * Returns the next integer in the pseudorandom sequence provided by this instance. The integer's value will be in the range (-2^31..2^31).  Note that the range is exclusive,
     * and is not the same as the usual Java int range of [-2^31..2^31).  The returned value has 2^32-1 possible values, and is symmetrical around zero.
     *
     * @return The next integer in the pseudorandom sequence.
     */
    public int nextInt() {

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

            default -> {
                throw new IllegalStateException( "Invalid generator value: " + generator );
            }
        }

        return state;
    }


    private record Triplet( int a, int b, int c ){};


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
