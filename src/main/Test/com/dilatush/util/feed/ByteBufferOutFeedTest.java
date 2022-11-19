package com.dilatush.util.feed;

import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ByteBufferOutFeedTest {

    @Test
    void write() {

        // first get an instance of ByteBufferOutFeed...
        // noinspection resource
        var bbof = new ByteBufferOutFeed( 100 );

        // then write a long to it...
        var b = ByteBuffer.allocate( 100 );
        b.putLong( 123456789L );
        b.flip();
        var bo = bbof.write( b );
        assertTrue( bo.ok(), "write outcome was not ok" );

        // then see if we can get it from the internal buffer...
        var ib = bbof.getBuffer();
        var r = ib.getLong();
        assertEquals( 123456789L, r, "not the expected value" );

        // then see if we can do it again...
        b.clear();
        b.putLong( 123456789L );
        b.flip();
        bo = bbof.write( b );
        assertTrue( bo.ok(), "write outcome should have been ok" );
        ib = bbof.getBuffer();
        r = ib.getLong();
        assertEquals( 123456789L, r, "not the expected value" );

        // then make sure we detect no remaining...
        b.clear();
        b.limit( 0 );
        bo = bbof.write( b );
        assertFalse( bo.ok(), "write outcome shouldn't have been ok" );
        assertTrue( bo.cause() instanceof BufferUnderflowException, "didn't get the expected BufferUnderflowException" );

        // then make sure we detect not enough remaining...
        // noinspection resource
        bbof = new ByteBufferOutFeed( 10 );
        b.clear();
        b.limit( 12 );
        bo = bbof.write( b );
        assertFalse( bo.ok(), "write outcome shouldn't have been ok" );
        assertTrue( bo.cause() instanceof BufferOverflowException, "didn't get the expected BufferOverflowException" );
    }


    @Test
    void close() {

        // make sure a closed instance doesn't work...
        var bbof = new ByteBufferOutFeed( 10 );
        var b = ByteBuffer.allocate( 10 );
        b.putInt( 12 );
        b.flip();
        bbof.close();
        var bo = bbof.write( b );
        assertFalse( bo.ok(), "write outcome shouldn't have been ok" );
        assertTrue( bo.cause() instanceof BufferOverflowException, "didn't get the expected BufferOverflowException" );
    }
}