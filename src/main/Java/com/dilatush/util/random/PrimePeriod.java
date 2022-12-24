package com.dilatush.util.random;


/**
 * Instances of this class modify the period of a pseudorandom integer sequence from the "natural" length of 2^32 (or 2^32 - 1) to one of eight prime numbers slightly less
 * than 2^32.
 */
public class PrimePeriod implements Randomish {


    // the largest eight prime numbers less than 2^32...
    private final static long[] PRIMES = new long[] {4294967111L, 4294967143L, 4294967161L, 4294967189L, 4294967197L, 4294967231L, 4294967279L, 4294967291L};


    private final Randomish source;   // the source to modify...
    private final int       prime;    // the index of the prime length, which must be in the range [0..7]...


    /**
     * <p>Creates a new instance of this class using the given source of pseudorandom integers and the given index [0..7] to one of the eight largest primes that are less than
     * 2^32.  The source must produce a sequence in the range [-2^32..2^32-1], or [-2^32+1..2^32-1].  Note that those ranges <i>include</i> zero.  The index can actually be any
     * value; only the 3 LSBs are used.</p>
     * <p>This class works by treating the integer as if it were unsigned, then filtering out (not returning) any values >= the indexed prime.  This method is efficient, and is
     * likely what most callers would expect - but it has a side-effect that will affect some uses: by eliminating only the largest integers, this method introduces a bias in
     * the bit distribution.  The smallest indexed prime (4,294,967,111) would filter out 185 integers, each of which has the 2^31 bit (the MSB) set to one.  The 2^0 bit (the LSB),
     * on the other hand, will be set to one for only half that many integers.  The resulting sequence of pseudorandom integers has 93 more ones in the MSB than in the LSB.  For
     * some applications this may matter.  If so, see {@link UnbiasedPrimePeriod} for an alternative approach.</p>
     *
     * @param _source The source of pseudorandom integers.
     * @param _prime The index of the prime to use for the period length.
     */
    public PrimePeriod( final Randomish _source, final int _prime ) {
        source = _source;
        prime  = 7 & _prime;
    }


    /**
     * Returns the next integer in the pseudorandom sequence provided by this instance. The integer's value will be in the range (-2^31..2^31).  Note that the range is exclusive,
     * and is not the same as the usual Java int range of [-2^31..2^31).  The returned value has 2^32-1 possible values, and is symmetrical around zero.
     *
     * @return The next integer in the pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        long n;

        do {
            n = 0xFFFFFFFFL & source.nextInt();
        } while( n >= PRIMES[prime] );

        return (int) n;
    }
}
