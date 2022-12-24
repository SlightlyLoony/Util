package com.dilatush.util.random;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RandomishTests {

    @Test
    void periodLengthRangeCorrector() {
        var src = new RangeCorrector( new XORShift32( 0, 0, 1 ) );
        var c = 0L;
        var gotZero = false;
        int i;
        do {
            c++;
            i = src.nextInt();
            if( i == 0 ) gotZero = true;
        } while( i != 1 );
        assertEquals( 0xFFFFFFFFL, c, "RangeCorrector range fail, measured range is wrong: " + c );
        assertTrue( gotZero, "RangeCorrector no zero value was ever output" );
    }

    @Test
    void periodLengthRangeCorrectorUnsigned() {
        var src = new RangeCorrectorUnsigned( new XORShift32( 0, 0, 2 ) );
        var c = 0L;
        var gotZero = false;
        int i;
        do {
            c++;
            i = src.nextInt();
            if( i == 0 ) gotZero = true;
        } while( i != 1 );
        assertEquals( 0xFFFFFFFFL, c, "RangeCorrectorUnsigned range fail, measured range is wrong: " + c );
        assertTrue( gotZero, "RangeCorrectorUnsigned no zero value was ever output" );
    }

    @Test
    void periodLengthPrimePeriod() {
        var src = new PrimePeriod( new RangeCorrectorUnsigned( new XORShift32( 0, 0, 2 ) ), 7 );
        var c = 0L;
        var gotZero = false;
        int i;
        do {
            c++;
            i = src.nextInt();
            if( i == 0 ) gotZero = true;
        } while( i != 1 );
        assertEquals( 4294967291L, c, "PrimePeriod range fail, measured range is wrong: " + c );
        assertTrue( gotZero, "RangeCorrector no zero value was ever output" );
    }


    @Test
    void valueCheckShuffler() {
        var chk = new PrimePeriod( new RangeCorrectorUnsigned( new XORShift32( 0, 0, 2 ) ), 7 );
        var src = new Shuffler( new PrimePeriod( new RangeCorrectorUnsigned( new XORShift32( 0, 0, 2 ) ), 7 ), 0, 7 );

        // first get one sheaf's worth of data from our source...
        var chkData = new int[211];
        for( int i = 0; i < 211; i++ ) chkData[i] = chk.nextInt();

        // now get one sheaf's worth of data from the shuffler...
        var shuffleData = new int[211];
        for( int i = 0; i < 211; i++ ) shuffleData[i] = src.nextInt();

        // now test...
        assertNotEquals( Arrays.equals( chkData, shuffleData ), "Shuffled data equals source data" );
        Arrays.sort( chkData );
        Arrays.sort( shuffleData );
        assertArrayEquals( chkData, shuffleData, "Shuffled data does not contains the same values as the source data" );
    }
}
