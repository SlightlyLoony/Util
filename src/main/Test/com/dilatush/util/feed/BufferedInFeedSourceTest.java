package com.dilatush.util.feed;

import com.dilatush.util.Outcome;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

class BufferedInFeedSourceTest {

    @SuppressWarnings( "resource" )
    @Test
    void read() throws InterruptedException {

        // create our feed...
        BufferedInFeedSource bbif = new BufferedInFeedSource( 100 );

        // give it some bytes to read...
        var bb = ByteBuffer.allocate( 100 );
        bb.putInt( 1234 );
        bb.putInt( 5678 );
        bb.flip();
        bbif.write( bb );

        // then read it back out...
        var bo = bbif.read( 4 );
        assertTrue( bo.ok(), "Read outcome was not ok: " + bo.msg() );
        var result = bo.info().getInt();
        assertEquals( 1234, result, "Mismatch" );
        bo = bbif.read( 4 );
        assertTrue( bo.ok(), "Read outcome was not ok: " + bo.msg() );
        result = bo.info().getInt();
        assertEquals( 5678, result, "Mismatch" );

        // now make sure there's no more bytes to be read...
        bbif.read( 4, 4, this::handle1 );
        sleep( 100 );
        assertNull( handledResult, "unexpected Outcome" );

        // now add some bytes and make sure we can read them...
        bb = ByteBuffer.allocate( 100 );
        bb.putInt( 3456 );
        bb.flip();
        bbif.write( bb );
        sleep( 100 );
        assertTrue( handledResult.ok(), "Read outcome was not ok: " + bo.msg() );
        assertEquals( 3456, handledResult.info().getInt(), "Mismatch" );

        // now make sure a bogus minBytes value gets picked up...
        bb.clear();
        bb.putInt( 12 );
        bb.flip();
        bo = bbif.read( 0, 50 );
        assertFalse( bo.ok(), "minBytes of 0 didn't make an error" );
        assertTrue( bo.cause() instanceof IllegalArgumentException, "Did not get expected IllegalArgumentException" );
    }

    private Outcome<ByteBuffer> handledResult = null;

    private void handle1( final Outcome<ByteBuffer> _outcome ) {
        handledResult = _outcome;
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