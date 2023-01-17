package com.dilatush.util.random;

import org.junit.jupiter.api.Test;

import static com.dilatush.util.General.isNull;
import static org.junit.jupiter.api.Assertions.*;

public class RandomishTests {

    /**
     * Tests the cycle length of the given pseudorandom source. The source's cycle length must be <= 2^32.
     *
     * @param _source The source to test.
     * @return {@code false} if the measured cycle length does not match the source's reported cycle length.
     */
    private boolean cycleLengthTest( final Randomish _source ) {

        // sanity checks...
        if( isNull( _source ) ) throw new IllegalArgumentException( "_source is null" );
        if( _source.cycleLength() > (1L << 32) ) throw new IllegalArgumentException( "_source has a cycle length > 2^32" );

        // collect 10 integers from the source...
        var startPattern = new int[10];
        for( int i = 0; i < startPattern.length; i++ ) startPattern[i] = _source.nextInt();

        // now collect integers until we see that pattern again...
        var patternIndex = 0;  // 0 means we haven't run into the first element of the start pattern yet...
        var count = 0L;
        while( count < (1L << 33) ) {
            count++;
            var ni = _source.nextInt();
            if( startPattern[patternIndex] == ni ) {
                patternIndex++;
                if( patternIndex == 10 ) {
                    return count == _source.cycleLength();
                }
            }

            else {
                patternIndex = 0;
            }
            if( count > _source.cycleLength() ) return false;
        }

        return _source.cycleLength() >= Math.pow( 2, 33 );
    }


    @Test
    void XORcycleLength() {

        assertTrue( cycleLengthTest( new XORShift32( 0, 0, 1, true ) ) );
        assertTrue( cycleLengthTest( new XORShift32( 1, 1, 1, false ) ) );

    }


    /**
     * A simple test of the cycle length for all 256 possible primes.  This test takes roughly one hour.
     */
    @Test
    void primeCycleLength() {

        var source = new XORShift32( 2, 2, 2, true );
        for( int i = 0; i < 256; i++ ) {
            System.out.println( "Running prime " + i );
            assertTrue( cycleLengthTest( new PrimeCycleFilter( source, i ) ) );
        }
    }
}
