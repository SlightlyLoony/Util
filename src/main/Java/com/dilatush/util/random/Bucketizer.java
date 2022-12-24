package com.dilatush.util.random;

import java.util.Arrays;

public class Bucketizer {

    private final int n;           // will be in the range [2..30]...
    private final int numBuckets;  // will be 2^n, where n is in the range [2..30]...
    private final int bucketSize;  // the number of values in each bucket...
    private final int mask;        // mask for the bits defining a bucket...
    private final int maskShift;   // the number of shifts to the right to get the mask definition bits to the LSBs...


    /**
     * Creates a new instance of this class with at least the given number of buckets.
     *
     * @param _minBuckets The minimum number of buckets for this instance, in the range [3..2^30].  Note that the <i>actual</i> number of buckets will be 2^n, where n is the
     *                    smallest value such that 2^n >= the specified minimum number of buckets.  For example, if 1,000 is the specified minimum number of buckets, then n will
     *                    be 10, as 2^10 is 1,024 - the smallest value of n such that 2^n >= 1,000.
     */
    public Bucketizer( final int _minBuckets) {

        n          = ((Integer.bitCount( _minBuckets ) == 1) ? 31 : 32) - Integer.numberOfLeadingZeros( _minBuckets );
        numBuckets = 1 << n;
        maskShift  = 32 - n;
        bucketSize = 1 << maskShift;
        mask       = (~0) << maskShift;
    }


    public int[] valueBuckets( final Randomish _randomish, final int _passes ) {

        // set up our buckets...
        var buckets = new int[numBuckets];

        // make our passes...
        for( int p = 0; p < _passes; p++ ) {

            // generate the pseudorandom numbers for one pass...
            for( int n = 0; n < numBuckets; n++ ) {

                // get a pseudorandom number...
                var num = _randomish.nextInt();

                // increment the bucket it belongs to...
                buckets[ (num & mask) >>> maskShift ]++;
            }
        }

        // and return the result!
        return buckets;
    }


    public Analytics analyze( final int[] _buckets ) {

        // sort the array, from min to max value...
        var sorted = Arrays.copyOf( _buckets, _buckets.length );
        Arrays.sort( sorted );

        return new Analytics( sorted );
    }


    public int getN() {

        return n;
    }


    public int getNumBuckets() {

        return numBuckets;
    }


    public int getBucketSize() {

        return bucketSize;
    }


    public record Analytics( int[] sorted ){}


    public static void main( String[] _args ) {

        var random = new XORShift32( 0, 0, 1 );
        var tester = new Bucketizer( 100 );
        var result = tester.valueBuckets( random, 1000 );
        var analytics = tester.analyze( result );

        random.hashCode();
    }
}