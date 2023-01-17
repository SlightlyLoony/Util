package com.dilatush.util.random;

import java.util.*;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class filter a source of pseudorandom integers to reduce the cycle length from 2^32 to one of the 256 largest prime numbers that are less than 2^32.  Multiple
 * sources with a prime cycle length then can be mixed (in various ways) to produce high quality pseudorandom sequences with arbitrarily long (but still finite) cycle lengths.
 */
public class PrimeCycleFilter implements Randomish {

    /*
     *   To get to a PRNG cycle length that is prime, we start with a 2^32 cycle length and remove numbers until the cycle length is prime.  I arbitrarily decided to
     *   have 256 prime cycle lengths to choose from.  The smallest of the 256 largest primes smaller than 2^32 is 4,294,961,873.  To get that cycle length
     *   requires removing 5,423 numbers from the 2^32 full cycle length, or 2,712 pairs of complements (to minimize bit-bias).
     *
     *   One way to do this is to search for the 32-bit integers that have 4 one bits, 2 of which are in a 5 bit field (and therefore 2
     *   of which are in the remaining 27 bit field).  There are 10 permutations of the former, and 351 of the latter, yielding 3,510 total permutations -
     *   more than enough to identify the 2,712 pairs of complements we need.
     */

    private static final long[] PRIMES    = initPrimes();  // the 256 largest prime numbers less than 2^32.
    private static final double TWO_TO_32 = Math.pow( 2, 32 );

    private final long      prime;     // the prime length of the cycle for this instance...
    private final Randomish source;    // the source for pseudorandom numbers to filter, with a cycle length of 2^32...
    private final Field     field5;
    private final Field     field27;
    private final int       skipException;  // the one integer marked in the skips table that should NOT be skipped...

    /*
     * This data structure is key to how this filter functions.  It is a table, created at instantiation, that provides an efficient way to identify those integers from the
     * pseudorandom sequence source that are to be skipped in order to provide a shorter pseudorandom sequence with a prime length.
     *
     * All the integers that might be eliminated share these characteristics:
     *   -- They have either exactly 4 bits set to a 1, or 4 bits set to a 0.
     *   -- Two of those bits are in a 5-bit field (this field is defined by mask5, above).  This field is different for different selected prime lengths, but never changes
     *      in a given instance of this class.
     *   -- The other two bits are in the complementary 27-bit field (this field is defined by mask27, above).
     *
     * The three dimensions of the skips data structure are:
     *   1. The bit number of the most significant bit that is set (to 1 or 0, depending on whether it has 2 ones or 2 zeroes set) in the 5-bit field.  This bit number will always
     *      be in [1..4], so we subtract 1 to get an offset bit number that is in [0..3].
     *   2. The bit number of the least significant bit that is set (as above) in the 5-bit field.  This bit number will always be in [0..3].
     *   3. The bit number of the most significant bit that is set (as above) in the 27-bit field.  This bit number will always be in [1..26], so we subtract 1 to get an offset
     *      bit number that is in [0..25].
     *
     * There is an implied 4th dimension in the bits of the integers stored in the 3rd dimension's array: the bit number of the least significant bit that is set (as above) in the
     * 27-bit field.  This bit number will always be in [0..25].  Exactly one integer has the four bits that address one specific bit in a particular entry in the skips data
     * structure.  If that bit is set to a 1, then that one integer should be skipped.
     *
     * Note that the first two dimensions define a table with 16 positions in it, but actually only 10 of them are valid.  In particular, all the positions for which the value of
     * the first dimension is less than or equal to the value of the second dimension are invalid (because the most significant bit number must be greater than the least
     * significant bit number.  The same general rule applies to the bits addressed by the 3rd and implied 4th dimensions: many of those bits will not be used, as they
     * represent invalid cases of the most significant bit number being less than or equal to the most significant bit number.
     */
    private final int[][][] skips = new int[4][4][];

    /**
     * Create a new instance of the class that produces a pseudorandom integer sequence with a cycle length of one of the 256 largest prime numbers less than 2^32.  The given
     * index, which must be in the range [0..255], chooses which prime number to use.  The given {@link Randomish} source must have a cycle length of 2^32.
     *
     * @param _source The source of pseudorandom numbers to filter.  The source must have a cycle length of 2^32.
     * @param _primeIndex the index [0..255] of the prime number to use as the cycle length.
     */
    public PrimeCycleFilter( final Randomish _source, final int _primeIndex ) {

        // sanity checks...
        if( isNull( _source ) )                        throw new IllegalArgumentException( "_source is null"                         );
        if( _source.cycleLength() != TWO_TO_32 )       throw new IllegalArgumentException( "_source does not have 2^32 cycle length" );
        if( (_primeIndex < 0) || (_primeIndex > 255) ) throw new IllegalArgumentException( "_primeIndex is not in [0..255]"          );

        // some basic setup...
        prime = PRIMES[_primeIndex];
        source = _source;

        // Get a pseudorandom source to use for the rest of our setup.  The goal is a source that is "randomly" different for each possible prime...
        var triplet      = (int) (prime % 81);                                                                        // modulo THIS prime to get a triplet...
        var generator    = (int) (7 & ((prime) ^ (prime >>> 3) ^ (prime >>> 6) ^ (prime >>> 8) ^ (prime >>> 11) ) );  // munge the lower 13 bits of THIS prime for a generator...
        var initialState = (int) (PRIMES[(_primeIndex + 3) & 0xFF] ^ PRIMES[(_primeIndex + 7) & 0xFF]);               // munge a couple OTHER primes to get initial state...
        var zeroInsert   = true;                                                                                      // we like our zeroes, though it doesn't really matter...
        var random       = new XORShift32( triplet, generator, initialState, zeroInsert );                            // and now, at last, we can construct our source...

        // get a sorted list of the randomly selected integer bits in our 5-bit and 27-bit fields...
        var bits27 = new ArrayList<Integer>( 32 );           // a place to hold the bit numbers of our 27 bit field, but we're going to start with all 32 bits...
        var bits5  = new ArrayList<Integer>( 5  );           // a place to hold the bit numbers of our 5 bit field...
        for( int i = 0; i < 32; i++ ) bits27.add( i );       // fill in all the bit numbers from 0 to 31...
        for( int i = 0; i < 5; i++ )                         // five times...
            bits5.add(                                       // add a bit number to our 5 bit field, selected at random from the 32 bits in an int...
                    bits27.remove(                           // remove a random bit from the initial collection of 32 bits...
                            (random.nextInt() >>> 7)         // this gets us a random positive 25 bit integer...
                                    % bits27.size()          // and this gets us a random number in [0..n), where n is the number of bits still in the 27 bit field's bit numbers...
                    )
            );
        bits5.sort( Comparator.comparingInt( a -> a ) );     // sort the 5 bit list (the 27 bit list is already sorted)...

        // get our field descriptions...
        field5  = getField( bits5  );
        field27 = getField( bits27 );

        // build our empty skips structure...
        for( int msb5 = 0; msb5 < 4; msb5++ ) {
            for( int lsb5 = 0; lsb5 <= msb5; lsb5++ ) {
                skips[msb5][lsb5] = new int[26];
            }
        }

        /*
         * Fill in the skips structure for the right number of integers to remove from the source sequence.  All but one of the bits set in the skips table represent two integers
         * to remove from the source sequence: one with 4 ones bits, and one with 4 zeroes bits.  Because we need to remove an odd number of integers, one bit represents a single
         * integer being removed; that integer is in skipException.
         */

        // some setup...
        var skipExceptionTemp = 0;
        var removalsLeft = (int)((long)TWO_TO_32 - prime);

        // loop until we've handled all the removals...
        while( removalsLeft > 0 ) {

            // randomly pick a 2-bit combination in each of the 5-bit field and the 27-bit field...
            var rand  = random.nextInt();
            var msb5  = rand & 0x3;                   // get the 5-bit msb position in [1..4], offset by 1 to [0..3] as an index into skips...
            var lsb5  = (rand >>> 3)  % (msb5 + 1);   // get the 5-bit lsb position in [0..msb5] as an index into skips...
            var msb27 = (rand >>> 7)  % 26;           // get the 27-bit msb position in [1..26], offset by 1 to [0..35] as an index into skips...
            var lsb27 = (rand >>> 14) % (msb27 + 1);  // get the 27-bit lsb position in [0..msb27] as an index into skips...

            // if we've already selected this integer as a removal, try again...
            if( (skips[msb5][lsb5][msb27] & (1 << lsb27)) != 0 ) continue;

            // if we haven't already set the skip exception, do so now...
            if( skipExceptionTemp == 0 ) {

                // synthesize the actual integer from the bit positions, then randomly invert it half the time...
                skipExceptionTemp =
                        (
                                (1 << field5.to32[msb5 + 1]) | (1 << field5.to32[lsb5])          // set the two bits in our 5-bit field...
                                | (1 << field27.to32[msb27 + 1]) | ((1 << field27.to32[lsb27]))  // set the two bits in our 27-bit field...
                        )
                                ^ ((random.nextInt() & 1) == 0 ? 0 : 0xFFFF_FFFF);               // randomly decide whether to invert it...

                removalsLeft--;     // only one integer with these bits will be removed, because of the skip exception...
            }
            else {
                removalsLeft -= 2;  // two integers with these bits (ones complements) will be removed...
            }

            // set the bit in skips for this one...
            skips[msb5][lsb5][msb27] |= (1 << lsb27);
        }

        // some cleanup...
        skipException = skipExceptionTemp;
    }


    /**
     * Returns the next integer in the random or pseudorandom sequence provided by this instance.
     *
     * @return The next integer in the random or pseudorandom sequence.
     */
    @Override
    public int nextInt() {

        // loop until we get a usable integer...
        while( true ) {

            // draw a trial integer from our source...
            var trial = source.nextInt();

            // if the population of 1s is neither 4 nor 28, we've got valid answer (returns a valid answer about 99.9999% of the time)...
            if( (Integer.bitCount( trial ) != 4) && (Integer.bitCount( trial ) != 28) ) return trial;

            // if this integer is the skip exception, we've got a valid answer...
            if( skipException == trial ) return trial;

            // if we make it to here, then we have to look up our integer in the skips table to see if it should be skipped...

            // get the trial in normalized form, with 4 ones set, then isolate our two fields...
            var normalized = (Integer.bitCount( trial ) == 4) ? trial : ~trial;
            var fld5  = normalized & field5.mask();
            var fld27 = normalized & field27.mask();

            // if we don't have 2 bits set in each field, then we've got a valid integer (returns a valid answer about 99.9826% of the time)...
            if( Integer.bitCount( fld5 ) != 2 ) return trial;

            // look up this number in the skips table, and if it's not set then we've got a valid integer...
            // the probability of returned valid from here depends on the prime; for the 0 index prime it's about 25%; for the 255 index prime it's about 99.8%...
            var dim1 = field5.from32[31 - Integer.numberOfLeadingZeros( fld5 )] - 1;
            var dim2 = field5.from32[Integer.numberOfTrailingZeros( fld5 )];
            var dim3 = field27.from32[31 - Integer.numberOfLeadingZeros( fld27 )] - 1;
            var msk = 1 << (field27.from32[Integer.numberOfTrailingZeros( fld27 )]);
            if( (skips[dim1][dim2][dim3] & msk) == 0 ) return trial;
        }
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

        return prime;
    }


    /**
     * Holds values related to a field (the 5-bit field or the 27-bit field).
     *
     * @param size the number of bits in the field.
     * @param bits a list of the integer bit numbers in the field.
     * @param mask an integer mask of the bits in the field.
     * @param from32 an array indexed by integer bit number, with each indexed entry containing the field bit number.
     * @param to32 an array indexed by field bit number, with each indexed entry containing the integer bit number.
     */
    private record Field( int size, List<Integer> bits, int mask, int[] from32, int[]to32 ) {}


    /**
     * Creates and returns a {@link Field} record, using the given list of integer bit numbers.  For example, given the list [4,15] this function will return a {@link Field} record
     * with a size of 2, a bit list of [4,15], a mask of 0x0000_8010, a from32 of [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0], and a to32 of [4,15].
     *
     * @param _bits a list of the integer bit numbers in this field.
     * @return the {@link Field} record created.
     */
    private static Field getField( final List<Integer> _bits ) {

        // some setup...
        var size = _bits.size();
        var mask   = 0;
        var from32 = new int[32];
        var to32   = new int[size];

        // for each bit in the field...
        for( int fieldBit = 0; fieldBit < _bits.size(); fieldBit++ ) {
            var intBit = _bits.get( fieldBit );    // get the integer bit number for this field bit number...
            mask |= (1 << intBit);                         // set the corresponding bit in the integer mask...
            from32[ intBit ] = fieldBit;                   // set the field bit number in from32...
            to32[ fieldBit ] = intBit;                     // set the integer bit number in to32...
        }

        return new Field( size, Collections.unmodifiableList(_bits), mask, from32, to32 );
    }


    /**
     * Returns the 256 largest prime numbers less than 2^32.  Shamelessly scraped from <a href="http://compoasso.free.fr/primelistweb/page/prime/liste_online_en.php">this
     * fabulous source of primes</a>.
     *
     * @return the 256 largest prime numbers less than 2^32.
     */
    private static long[] initPrimes() {

        // Only the 13 LSBs of these numbers are different from each other - the 19 MSBs are all ones.
        return new long[] {
                4_294_961_873L, 4_294_961_893L, 4_294_961_897L, 4_294_961_921L, 4_294_961_927L, 4_294_961_941L, 4_294_961_959L, 4_294_961_963L,
                4_294_962_019L, 4_294_962_047L, 4_294_962_079L, 4_294_962_137L, 4_294_962_151L, 4_294_962_211L, 4_294_962_223L, 4_294_962_233L,
                4_294_962_271L, 4_294_962_277L, 4_294_962_299L, 4_294_962_313L, 4_294_962_341L, 4_294_962_349L, 4_294_962_367L, 4_294_962_377L,
                4_294_962_389L, 4_294_962_391L, 4_294_962_401L, 4_294_962_409L, 4_294_962_449L, 4_294_962_473L, 4_294_962_499L, 4_294_962_533L,
                4_294_962_541L, 4_294_962_589L, 4_294_962_619L, 4_294_962_629L, 4_294_962_641L, 4_294_962_653L, 4_294_962_689L, 4_294_962_691L,
                4_294_962_703L, 4_294_962_719L, 4_294_962_731L, 4_294_962_751L, 4_294_962_757L, 4_294_962_779L, 4_294_962_809L, 4_294_962_817L,
                4_294_962_827L, 4_294_962_853L, 4_294_962_887L, 4_294_962_899L, 4_294_962_911L, 4_294_962_929L, 4_294_962_953L, 4_294_963_039L,
                4_294_963_051L, 4_294_963_093L, 4_294_963_097L, 4_294_963_111L, 4_294_963_117L, 4_294_963_171L, 4_294_963_237L, 4_294_963_291L,
                4_294_963_313L, 4_294_963_333L, 4_294_963_349L, 4_294_963_369L, 4_294_963_427L, 4_294_963_429L, 4_294_963_459L, 4_294_963_499L,
                4_294_963_523L, 4_294_963_537L, 4_294_963_553L, 4_294_963_571L, 4_294_963_583L, 4_294_963_619L, 4_294_963_637L, 4_294_963_639L,
                4_294_963_643L, 4_294_963_667L, 4_294_963_681L, 4_294_963_723L, 4_294_963_747L, 4_294_963_781L, 4_294_963_787L, 4_294_963_847L,
                4_294_963_853L, 4_294_963_891L, 4_294_963_901L, 4_294_963_921L, 4_294_963_943L, 4_294_963_957L, 4_294_963_987L, 4_294_963_993L,
                4_294_964_017L, 4_294_964_027L, 4_294_964_029L, 4_294_964_039L, 4_294_964_081L, 4_294_964_123L, 4_294_964_131L, 4_294_964_159L,
                4_294_964_173L, 4_294_964_203L, 4_294_964_207L, 4_294_964_209L, 4_294_964_213L, 4_294_964_221L, 4_294_964_239L, 4_294_964_249L,
                4_294_964_257L, 4_294_964_263L, 4_294_964_281L, 4_294_964_287L, 4_294_964_309L, 4_294_964_327L, 4_294_964_341L, 4_294_964_381L,
                4_294_964_419L, 4_294_964_437L, 4_294_964_441L, 4_294_964_461L, 4_294_964_489L, 4_294_964_491L, 4_294_964_521L, 4_294_964_537L,
                4_294_964_543L, 4_294_964_561L, 4_294_964_579L, 4_294_964_599L, 4_294_964_621L, 4_294_964_633L, 4_294_964_683L, 4_294_964_689L,
                4_294_964_749L, 4_294_964_771L, 4_294_964_789L, 4_294_964_809L, 4_294_964_827L, 4_294_964_833L, 4_294_964_879L, 4_294_964_887L,
                4_294_964_893L, 4_294_964_897L, 4_294_964_899L, 4_294_964_903L, 4_294_964_923L, 4_294_964_929L, 4_294_964_939L, 4_294_964_959L,
                4_294_964_969L, 4_294_964_977L, 4_294_964_981L, 4_294_965_019L, 4_294_965_131L, 4_294_965_137L, 4_294_965_151L, 4_294_965_161L,
                4_294_965_193L, 4_294_965_203L, 4_294_965_229L, 4_294_965_251L, 4_294_965_263L, 4_294_965_307L, 4_294_965_313L, 4_294_965_331L,
                4_294_965_347L, 4_294_965_361L, 4_294_965_383L, 4_294_965_413L, 4_294_965_457L, 4_294_965_461L, 4_294_965_487L, 4_294_965_529L,
                4_294_965_581L, 4_294_965_601L, 4_294_965_613L, 4_294_965_617L, 4_294_965_641L, 4_294_965_659L, 4_294_965_671L, 4_294_965_673L,
                4_294_965_679L, 4_294_965_683L, 4_294_965_691L, 4_294_965_721L, 4_294_965_733L, 4_294_965_737L, 4_294_965_757L, 4_294_965_767L,
                4_294_965_793L, 4_294_965_821L, 4_294_965_839L, 4_294_965_841L, 4_294_965_847L, 4_294_965_887L, 4_294_965_911L, 4_294_965_937L,
                4_294_965_949L, 4_294_965_967L, 4_294_965_971L, 4_294_965_977L, 4_294_966_001L, 4_294_966_007L, 4_294_966_043L, 4_294_966_073L,
                4_294_966_087L, 4_294_966_099L, 4_294_966_121L, 4_294_966_129L, 4_294_966_153L, 4_294_966_163L, 4_294_966_177L, 4_294_966_187L,
                4_294_966_217L, 4_294_966_231L, 4_294_966_237L, 4_294_966_243L, 4_294_966_297L, 4_294_966_337L, 4_294_966_367L, 4_294_966_373L,
                4_294_966_427L, 4_294_966_441L, 4_294_966_447L, 4_294_966_477L, 4_294_966_553L, 4_294_966_583L, 4_294_966_591L, 4_294_966_619L,
                4_294_966_639L, 4_294_966_651L, 4_294_966_657L, 4_294_966_661L, 4_294_966_667L, 4_294_966_769L, 4_294_966_813L, 4_294_966_829L,
                4_294_966_877L, 4_294_966_909L, 4_294_966_927L, 4_294_966_943L, 4_294_966_981L, 4_294_966_997L, 4_294_967_029L, 4_294_967_087L,
                4_294_967_111L, 4_294_967_143L, 4_294_967_161L, 4_294_967_189L, 4_294_967_197L, 4_294_967_231L, 4_294_967_279L, 4_294_967_291L,
        };
    }
}
