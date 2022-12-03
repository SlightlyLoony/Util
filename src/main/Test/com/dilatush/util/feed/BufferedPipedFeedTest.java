package com.dilatush.util.feed;

import com.dilatush.util.Outcome;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferedPipedFeedTest {

    private BufferedPipedFeed bpf;
    private boolean useReadComplete;
    private boolean useWriteComplete;

    @Test
    void use() throws InterruptedException {

        // setup...
        useReadComplete = false;
        useWriteComplete = false;

        // create an instance that can buffer two integers...
        bpf = new BufferedPipedFeed( 8 );

        // write three integers, non-blocking...
        var b = ByteBuffer.allocate( 12 ).putInt( 123 ).putInt( 456 ).putInt( 789 ).flip();
        bpf.write( b, this::useWriteComplete1 );

        // start another thread to read them...
        var urt = new UseRead();
        urt.start();

        // wait a short while, then check to see if this all completed ok...
        sleep(100);
        assertTrue( useWriteComplete, "write did not complete" );
        assertTrue( useReadComplete, "read did not complete" );
    }


    private class UseRead extends Thread {

        public void run() {

            // try to read three integers: 123, 456, and 789...
            var rbo = bpf.read( 4, 8 );
            assertTrue( rbo.ok(), "should have been ok for 123 and 456");
            var rb = rbo.info();
            assertEquals( 8, rb.limit(), "should have read 8 bytes" );

            var i = rb.getInt();
            assertEquals( 123, i, "should have been 123" );

            i = rb.getInt();
            assertEquals( 456, i, "should have been 456" );

            rbo = bpf.read( 4, 8 );
            assertTrue( rbo.ok(), "should have been ok for 123 and 456");
            rb = rbo.info();
            assertEquals( 4, rb.limit(), "should have read 4 bytes" );

            i = rb.getInt();
            assertEquals( 789, i, "should have been 789" );

            useReadComplete = true;
        }
    }


    private void useWriteComplete1( final Outcome<?> _outcome ) {
        assertTrue( _outcome.ok(), "write outcome should have been ok" );
        useWriteComplete = true;
    }
}