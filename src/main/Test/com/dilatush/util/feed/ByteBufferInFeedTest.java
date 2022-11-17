package com.dilatush.util.feed;

import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ByteBufferInFeedTest {

    @SuppressWarnings( "resource" )
    @Test
    void read() {

        // try a simple read, first setting up a buffer...
        var bb = ByteBuffer.allocate( 100 );
        bb.putInt( 123456 );
        bb.flip();

        // then create our feed...
        ByteBufferInFeed bbif = new ByteBufferInFeed( bb );

        // then read it back out...
        var bb2 = ByteBuffer.allocate( 50 );
        var bo = bbif.read( bb2 );
        assertTrue( bo.ok(), "Read outcome was not ok: " + bo.msg() );
        var result = bo.info().getInt();
        assertEquals( 123456, result, "Mismatch" );

        // now make sure there's no more bytes to be read...
        bb2.clear();
        bo = bbif.read( bb2 );
        assertFalse( bo.ok(), "Bytes area available when they shouldn't be" );
        assertTrue( bo.cause() instanceof BufferUnderflowException, "Did not get expected BufferUnderflowException" );

        // now make sure a bogus minBytes value gets picked up...
        bb.clear();
        bb.putInt( 12 );
        bb.flip();
        bo = bbif.read( bb2, 0 );
        assertFalse( bo.ok(), "minBytes of 0 didn't make an error" );
        assertTrue( bo.cause() instanceof IllegalArgumentException, "Did not get expected IllegalArgumentException" );
    }


    @Test
    void close() {

        // try a simple read, first setting up a buffer...
        var bb = ByteBuffer.allocate( 100 );
        bb.putInt( 123456 );
        bb.flip();

        // then create our feed...
        ByteBufferInFeed bbif = new ByteBufferInFeed( bb );

        // now close it...
        bbif.close();

        // then try to read it back out...
        var bb2 = ByteBuffer.allocate( 50 );
        var bo = bbif.read( bb2 );
        assertFalse( bo.ok(), "Read outcome was ok" );
        assertTrue( bo.cause() instanceof BufferUnderflowException, "Did not get expected BufferUnderflowException" );
    }
}