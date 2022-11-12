package com.dilatush.util.crc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CRCTest {

    @Test
    void testCRC8() {
        var c = new CRC( CRCAlgorithm.CRC8 );
        c.update( CRCAlgorithm.CHECK_INPUT );
        assertTrue( c.check( c.getCRC() ) );
    }

    @Test
    void testCRC16() {
        var c = new CRC( CRCAlgorithm.CRC16 );
        c.update( CRCAlgorithm.CHECK_INPUT );
        assertTrue( c.check( c.getCRC() ) );
    }

    @Test
    void testCRC24() {
        var c = new CRC( CRCAlgorithm.CRC24 );
        c.update( CRCAlgorithm.CHECK_INPUT );
        assertTrue( c.check( c.getCRC() ) );
    }

    @Test
    void testCRC24_OS9() {
        var c = new CRC( CRCAlgorithm.CRC24_OS9 );
        c.update( CRCAlgorithm.CHECK_INPUT );
        assertTrue( c.check( c.getCRC() ) );
    }

    @Test
    void testCRC32() {
        var c = new CRC( CRCAlgorithm.CRC32 );
        c.update( CRCAlgorithm.CHECK_INPUT );
        assertTrue( c.check( c.getCRC() ) );
    }

    @Test
    void testCRC64() {
        var c = new CRC( CRCAlgorithm.CRC64 );
        c.update( CRCAlgorithm.CHECK_INPUT );
        assertTrue( c.check( c.getCRC() ) );
    }

}