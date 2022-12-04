package com.dilatush.util.feed;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class BufferedOutFeedTest {

    @Test
    void write() {

        // first get an instance of BufferedOutFeed...
        // noinspection resource
        var bbof = new BufferedOutFeed( 100 );

        // then write a long to it...
        var b = ByteBuffer.allocate( 100 ).putLong( 123456789L ).flip();
        var bo = bbof.write( b );
        assertTrue( bo.ok(), "write outcome was not ok" );

        // then see if we can get it from the internal buffer...
        var ib = bbof.drainToByteBuffer();
        var r = ib.getLong();
        assertEquals( 123456789L, r, "not the expected value" );

        // then see if we can do it again...
        b.clear().putLong( 123456789L ).flip();
        bo = bbof.write( b );
        assertTrue( bo.ok(), "write outcome should have been ok" );
        ib = bbof.drainToByteBuffer();
        r = ib.getLong();
        assertEquals( 123456789L, r, "not the expected value" );

        // then make sure we detect no remaining...
        ib = bbof.drainToByteBuffer();
        assertNull( ib, "should be null, indicating no bytes available" );
    }


    @Test
    void close() {

        // make sure a closed instance doesn't work...
        var bbof = new BufferedOutFeed( 10 );
        var b = ByteBuffer.allocate( 10 );
        b.putInt( 12 );
        b.flip();
        bbof.close();
        var bo = bbof.write( b );
        assertFalse( bo.ok(), "write outcome shouldn't have been ok" );
    }
}