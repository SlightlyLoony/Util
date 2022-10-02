package com.dilatush.util.networkingengine;

import com.dilatush.util.ip.IPv4Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings( { "unused", "BusyWait" } )
class NetworkingEngineTest {

    private static final Logger LOGGER                    = getLogger();

    @BeforeAll
    public static void setup() {
    }


    @Test
    void testTCPConnectionTimeout() {

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // get an outbound pipe...
        var pipeOutcome = TCPOutboundPipe.getTCPOutboundPipe( engine, IPv4Address.WILDCARD, 0 );
        assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + engineOutcome.msg() );
        var pipe = pipeOutcome.info();

        // try connecting it to a non-existent port...
        var connectOutcome = pipe.connect( IPv4Address.fromString( "13.52.82.44" ).info(), 81 );
        assertTrue( connectOutcome.notOk(), "TCP connect did not report timeout " );
        assertTrue( connectOutcome.msg().contains( "timed out" ) );
        pipe.close();

        // get a new outbound pipe...
        pipeOutcome = TCPOutboundPipe.getTCPOutboundPipe( engine, IPv4Address.WILDCARD, 0 );
        assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + engineOutcome.msg() );
        pipe = pipeOutcome.info();

        // now try connecting it to a port that exists...
        connectOutcome = pipe.connect( IPv4Address.fromString( "13.52.82.44" ).info(), 80 );
        assertTrue( connectOutcome.ok(), "TCP connect did not connect: " + connectOutcome.msg() );


        pipe.close();
        engine.shutdown();
    }


    @Test
    void testSimpleTCP() {

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // start up a listener...
        var listenerOutcome = TCPListener.getInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onAcceptEcho );
        assertTrue( listenerOutcome.ok(), "Problem creating TCPListener: " + engineOutcome.msg() );
        var listener = listenerOutcome.info();

        // get an outbound pipe and connect it...
        var pipeOutcome = TCPOutboundPipe.getTCPOutboundPipe( engine, IPv4Address.LOOPBACK, 0 );
        assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + engineOutcome.msg() );
        var pipe = pipeOutcome.info();
        var connectOutcome = pipe.connect( IPv4Address.LOOPBACK, 5555 );
        assertTrue( connectOutcome.ok(), "Problem with connect: " + connectOutcome.msg() );

        // write an int...
        var wb = ByteBuffer.allocate( 100 );
        wb.putInt( 8383 );
        wb.flip();
        var writeOutcome = pipe.write( wb );
        assertTrue( writeOutcome.ok(), "Problem writing: " + writeOutcome.msg() );

        // read back the echo and make sure it's correct...
        var rb = ByteBuffer.allocate( 100 );
        var readOutcome = pipe.read( rb );
        assertTrue( readOutcome.ok(), "Problem reading: " + readOutcome.msg() );
        var rd = rb.getInt();
        assertEquals( 8383, rd, "Data read does not match data transmitted" );

        // shut it all down...
        echo.inboundPipe.close();
        pipe.close();
        listener.close();
        engine.shutdown();
    }

    private static Echo echo;

    private static void onAcceptEcho( final TCPInboundPipe _pipe ) {
        echo = new Echo( _pipe );
        echo.start();
    }


    private static class Echo extends Thread {
        private final TCPInboundPipe inboundPipe;
        private Echo( final TCPInboundPipe _inboundPipe ) {
            inboundPipe = _inboundPipe;
        }

        public void run() {
            var rb = ByteBuffer.allocate( 100 );
            while( !interrupted() ) {
                inboundPipe.read( rb );
                inboundPipe.write( rb );
            }
        }
    }


    @Test
    void testWriteWaiting() throws InterruptedException {

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // start up a listener...
        var listenerOutcome = TCPListener.getInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onAcceptSlowReceiver );
        assertTrue( listenerOutcome.ok(), "Problem creating TCPListener: " + engineOutcome.msg() );
        var listener = listenerOutcome.info();

        // get an outbound pipe and connect it...
        var pipeOutcome = TCPOutboundPipe.getTCPOutboundPipe( engine, IPv4Address.LOOPBACK, 0 );
        assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + engineOutcome.msg() );
        var pipe = pipeOutcome.info();
        var connectOutcome = pipe.connect( IPv4Address.LOOPBACK, 5555 );
        assertTrue( connectOutcome.ok(), "Problem with connect: " + connectOutcome.msg() );

        // set the write buffer to 1000 bytes...
        var setOutcome = pipe.setOption( StandardSocketOptions.SO_SNDBUF, 1000 );
        assertTrue( setOutcome.ok(), "Problem setting SO_SNDBUF option: " + setOutcome.msg() );

        // write 1000 ints, 0-999...
        var startTime = System.currentTimeMillis();
        for( int i = 0; i < 1000; i++ ) {
            var wb = ByteBuffer.allocate( 100 );
            wb.putInt( i );
            wb.flip();
            var writeOutcome = pipe.write( wb );
            assertTrue( writeOutcome.ok(), "Problem writing: " + writeOutcome.msg() );
        }
        var endTime = System.currentTimeMillis();

        // verify that write time is over 100ms, so we got writeable events...
        assertTrue( (endTime - startTime) >= 100, "Writes were not blocked" );

        // wait for all data to be read...
        slowReceiverSemaphore.acquire();

        // shut it all down...
        slowReceiver.inboundPipe.close();
        pipe.close();
        listener.close();
        engine.shutdown();
    }

    private static SlowReceiver slowReceiver;
    private static final Semaphore slowReceiverSemaphore = new Semaphore( 0 );

    private static void onAcceptSlowReceiver( final TCPInboundPipe _pipe ) {
        slowReceiver = new SlowReceiver( _pipe );
        slowReceiver.start();
    }


    @SuppressWarnings( "BusyWait" )
    private static class SlowReceiver extends Thread {
        private final TCPInboundPipe inboundPipe;
        private SlowReceiver( final TCPInboundPipe _inboundPipe ) {
            inboundPipe = _inboundPipe;
        }

        public void run() {
            int shouldBe = 0;
            while( !interrupted() ) {
                var rb = ByteBuffer.allocate( 4 );
                var readOutcome = inboundPipe.read( rb );
                assertTrue( readOutcome.ok(), "Problem reading: " + readOutcome.msg() );
                var dataRead = rb.getInt();
                assertEquals( shouldBe, dataRead, "Wrong data read, should be " + shouldBe + ", was " + dataRead );
                shouldBe++;
                if( shouldBe == 1000 ) {
                    slowReceiverSemaphore.release();
                    break;
                }
                try {
                    sleep( 1 );
                }
                catch( InterruptedException _e ) {
                    throw new RuntimeException( _e );
                }
            }
        }
    }


    @Test
    void testMultipleTCP() {

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // start up a listener...
        var listenerOutcome = TCPListener.getInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onAcceptMultipleTCP );
        assertTrue( listenerOutcome.ok(), "Problem creating TCPListener: " + engineOutcome.msg() );
        var listener = listenerOutcome.info();

        // get some outbound pipes and connect them...
        Set<TCPOutboundPipe> pipes = new HashSet<>();
        for( int i = 0; i < NUM_PIPES; i++ ) {
            var pipeOutcome = TCPOutboundPipe.getTCPOutboundPipe( engine, IPv4Address.LOOPBACK, 0 );
            assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + engineOutcome.msg() );
            var pipe = pipeOutcome.info();
            var connectOutcome = pipe.connect( IPv4Address.LOOPBACK, 5555 );
            assertTrue( connectOutcome.ok(), "Problem with connect: " + connectOutcome.msg() );
            pipes.add( pipe );
        }

        // start a reader in a separate thread for each of our pipes...
        var iter = pipes.iterator();
        //noinspection MismatchedQueryAndUpdateOfCollection
        var readers = new HashSet<EchoReader>();
        while( iter.hasNext() ) {
            var pipe = iter.next();
            var reader = new EchoReader( pipe );
            reader.start();
            readers.add( reader );
        }

        // write the ints 0..999 to each of our pipes...
        for( int i = 0; i < 1000; i++ ) {
            int finalI = i;
            pipes.forEach( pipe -> {
                var wb = ByteBuffer.allocate( 100 );
                wb.putInt( finalI );
                wb.flip();
                var writeOutcome = pipe.write( wb );
                assertTrue( writeOutcome.ok(), "Problem writing: " + writeOutcome.msg() );
            } );
        }

        // wait until we've read everything...
        while( READ_COUNTER.get() > 0 ) {
            try {
                sleep( 10 );
            }
            catch( InterruptedException _e ) {
                throw new RuntimeException( _e );
            }
        }

        // shut it all down...
        multipleEcho.forEach( _echo -> _echo.inboundPipe.close() );
        pipes.forEach( TCPPipe::close );
        listener.close();
        engine.shutdown();
    }

    private static final int NUM_PIPES = 10;
    private static final AtomicInteger READ_COUNTER = new AtomicInteger( NUM_PIPES );

    private static final Set<Echo> multipleEcho = new HashSet<>();

    private static void onAcceptMultipleTCP( final TCPInboundPipe _pipe ) {
        var newEcho = new Echo( _pipe );
        multipleEcho.add( newEcho );
        newEcho.start();
    }

    private static class EchoReader extends Thread {
        private final TCPOutboundPipe pipe;
        private EchoReader( final TCPOutboundPipe _pipe ) {
            pipe = _pipe;
        }
        public void run() {
            var shouldBe = 0;
            while( !interrupted() ) {
                var rb = ByteBuffer.allocate( 4 );
                var readOutcome = pipe.read( rb );
                assertTrue( readOutcome.ok(), "Read error: " + readOutcome.msg() );
                var readData = rb.getInt();
                assertEquals( shouldBe, readData, "Error in data, should be " + shouldBe + ", but was " + readData );
                shouldBe++;
                if( shouldBe == 1000 )
                    break;
            }
            READ_COUNTER.decrementAndGet();
        }
    }
}