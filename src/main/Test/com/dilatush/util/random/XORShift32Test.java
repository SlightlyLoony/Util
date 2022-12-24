package com.dilatush.util.random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XORShift32Test {

    private static final long PROPER_RANGE = 0xFFFFFFFFL;

    @Test
    void triplets() {

        for( int i = 60; i < 81; i++ ) {
            var rng = new XORShift32( i, 0, 1 );
            long c = 1;
            while( rng.nextInt() != 1 ) {
                c++;
            }
            assertEquals( PROPER_RANGE, c, "Triplet range fail, on triplet index " + i + ", measured range is wrong: " + c );
        }
    }


    @Test
    void generators() {

        for( int i = 0; i < 8; i++ ) {
            var rng = new XORShift32( 0, i, 1 );
            long c = 1;
            while( rng.nextInt() != 1 ) {
                c++;
            }
            assertEquals( PROPER_RANGE, c, "Generator fail, on generator index " + i + ", range is wrong: " + c );
        }
    }
}