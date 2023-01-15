package com.dilatush.util.random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RandomishTests {

    private boolean cycleLengthTest( final Randomish _source ) {

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


    @Test
    void primeCycleLength() {

        var source = new XORShift32( 2, 2, 2, true );
        for( int i = 0; i < 256; i++ ) {
            System.out.println( "Running prime " + i );
            assertTrue( cycleLengthTest( new PrimeCycleFilter( source, i ) ) );
        }
    }
}
