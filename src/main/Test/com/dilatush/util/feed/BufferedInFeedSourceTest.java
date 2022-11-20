package com.dilatush.util.feed;

import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class BufferedInFeedSourceTest {

    @SuppressWarnings( "resource" )
    @Test
    void read() {

        // create our feed...
        BufferedInFeedSource bbif = new BufferedInFeedSource( 100 );

        // give it some bytes to read...
        var bb = ByteBuffer.allocate( 100 );
        bb.putInt( 123456 );
        bb.flip();
        bbif.write( bb );

        // then read it back out...
        var bo = bbif.read( 50 );
        assertTrue( bo.ok(), "Read outcome was not ok: " + bo.msg() );
        var result = bo.info().getInt();
        assertEquals( 123456, result, "Mismatch" );

        // now make sure there's no more bytes to be read...
        bo = bbif.read( 50 );
        assertFalse( bo.ok(), "Bytes area available when they shouldn't be" );
        assertTrue( bo.cause() instanceof BufferUnderflowException, "Did not get expected BufferUnderflowException" );

        // now make sure a bogus minBytes value gets picked up...
        bb.clear();
        bb.putInt( 12 );
        bb.flip();
        bo = bbif.read( 0, 50 );
        assertFalse( bo.ok(), "minBytes of 0 didn't make an error" );
        assertTrue( bo.cause() instanceof IllegalArgumentException, "Did not get expected IllegalArgumentException" );
    }


    @Test
    void close() {

        // create our feed...
        BufferedInFeedSource bbif = new BufferedInFeedSource( 100 );

        // feed it some data...
        var bb = ByteBuffer.allocate( 100 );
        bb.putInt( 123456 );
        bb.flip();
        bbif.write( bb );

        // now close it...
        bbif.close();

        // then try to read it back out...
        var bo = bbif.read( 50 );
        assertFalse( bo.ok(), "Read outcome was ok" );
    }
}