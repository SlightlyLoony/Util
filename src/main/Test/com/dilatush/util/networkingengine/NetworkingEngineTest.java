package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPv4Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

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
        var pipeOutcome = TCPOutboundPipe.getNewInstance( engine, IPv4Address.WILDCARD );
        assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + engineOutcome.msg() );
        var pipe = pipeOutcome.info();

        // try connecting it to a non-existent port...
        var connectOutcome = pipe.connect( IPv4Address.fromString( "13.52.82.44" ).info(), 81 );
        assertTrue( connectOutcome.notOk(), "TCP connect did not report timeout " );
        assertTrue( connectOutcome.msg().contains( "timed out" ) );
        pipe.close();

        // get a new outbound pipe...
        pipeOutcome = TCPOutboundPipe.getNewInstance( engine, IPv4Address.WILDCARD );
        assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + engineOutcome.msg() );
        pipe = pipeOutcome.info();

        // now try connecting it to a port that exists...
        connectOutcome = pipe.connect( IPv4Address.fromString( "13.52.82.44" ).info(), 80 );
        assertTrue( connectOutcome.ok(), "TCP connect did not connect: " + connectOutcome.msg() );


        pipe.close();
        engine.shutdown();
    }


    private static UDPServer udpServer;

    @Test
    void testSimpleUDP() throws IllegalStateException {

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // start up a UDP server...
        var serverOutcome = UDPServer.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onInboundUDP, 200, null, null );
        assertTrue( serverOutcome.ok(), "Problem creating UDPServer: " + serverOutcome.msg() );
        udpServer = serverOutcome.info();

        // get a UDP client...
        var clientOutcome = UDPClient.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, 200 );
        assertTrue( clientOutcome.ok(), "Problem creating UDP client: " + clientOutcome.msg() );
        var client = clientOutcome.info();

        // write an int...
        var wb = ByteBuffer.allocate( 100 );
        wb.putInt( 8217 );
        wb.flip();
        var datagram = new OutboundDatagram( wb, IPv4Address.LOOPBACK, 5555 );
        var sendOutcome = client.send( datagram );
        assertTrue( sendOutcome.ok(), "Problem sending: " + sendOutcome.msg() );

        // read back the echo and make sure it's correct...
        var readOutcome = client.receive();
        assertTrue( readOutcome.ok(), "Problem reading: " + readOutcome.msg() );
        var rd = readOutcome.info().getData().getInt();
        assertEquals( 8217, rd, "Data read does not match data transmitted" );

        // shut it all down...
        echo.inboundPipe.close();
        client.close();
        udpServer.close();
        engine.shutdown();
    }

    private static void onInboundUDP( final InboundDatagram _datagram ) {
        var datagram = new OutboundDatagram( _datagram.getData(), _datagram );
        try {
            udpServer.send( datagram );
        }
        catch( IllegalStateException _e ) {
            LOGGER.log( Level.WARNING, "Problem sending datagram: " + _e.getMessage(), _e );
        }
    }

    @Test
    void testTruncatedUDP() throws IllegalStateException, InterruptedException {
        LOGGER.finest( "test truncated UDP" );

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // start up a UDP server...
        var serverOutcome = UDPServer.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onTruncatedUDP, 4, null, null );
        assertTrue( serverOutcome.ok(), "Problem creating UDPServer: " + serverOutcome.msg() );
        udpServer = serverOutcome.info();

        // get a UDP client...
        var clientOutcome = UDPClient.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, 200 );
        assertTrue( clientOutcome.ok(), "Problem creating UDP client: " + clientOutcome.msg() );
        var client = clientOutcome.info();

        // write a four byte datagram...
        var wb = ByteBuffer.allocate( 100 );
        wb.putInt( 4 );
        wb.flip();
        var datagram = new OutboundDatagram( wb, IPv4Address.LOOPBACK, 5555 );
        var sendOutcome = client.send( datagram );
        assertTrue( sendOutcome.ok(), "Problem sending: " + sendOutcome.msg() );

        // write a five byte datagram...
        wb = ByteBuffer.allocate( 100 );
        wb.putInt( 5 );
        wb.put( (byte) 0 );
        wb.flip();
        datagram = new OutboundDatagram( wb, IPv4Address.LOOPBACK, 5555 );
        sendOutcome = client.send( datagram );
        assertTrue( sendOutcome.ok(), "Problem sending: " + sendOutcome.msg() );

        // wait briefly...
        Thread.sleep( 1000 );

        // shut it all down...
        client.close();
        udpServer.close();
        engine.shutdown();
    }

    private static void onTruncatedUDP( final InboundDatagram _datagram ) {
        var count = _datagram.getData().getInt();
        LOGGER.finest( "Truncation: " + _datagram.isTruncated() + ", count: " + count );
        assertEquals( _datagram.isTruncated(), count > 4, "Truncation problem" );
    }

    @Test
    void testUDPClients() throws IllegalStateException {

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // get one UDP client...
        var client1Outcome = UDPClient.getNewInstance( engine, IPv4Address.LOOPBACK, 5556, IPv4Address.LOOPBACK, 5555, 200 );
        assertTrue( client1Outcome.ok(), "Problem creating UDPServer: " + client1Outcome.msg() );
        var client1 = client1Outcome.info();

        // get another UDP client...
        var clientOutcome = UDPClient.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, IPv4Address.LOOPBACK, 5556, 200 );
        assertTrue( clientOutcome.ok(), "Problem creating TCPOutboundPipe: " + clientOutcome.msg() );
        var client2 = clientOutcome.info();

        // send 100 random 200 byte datagrams back and forth...
        var random = new Random( System.currentTimeMillis() );
        for( int i = 0; i < 100; i++ ) {

            // get some random data...
            var rdata = new byte[200];
            random.nextBytes( rdata );

            // send a random datagram from client 1 to client 2...
            var wb = ByteBuffer.allocate( 200 );
            wb.put( rdata );
            wb.flip();
            var datagram = new OutboundDatagram( wb, IPv4Address.LOOPBACK, 5555 );
            var sendOutcome = client1.send( datagram );
            assertTrue( sendOutcome.ok(), "Problem sending: " + sendOutcome.msg() );

            // read the data from client 2 and make sure it's correct...
            var readOutcome = client2.receive();
            assertTrue( readOutcome.ok(), "Problem reading: " + readOutcome.msg() );
            var rd = readOutcome.info().getData();
            var rb = new byte[rd.limit()];
            rd.get(rb);
            assertArrayEquals( rdata, rb, "Data read does not match data transmitted" );

            // send the datagram from client 2 back to client 1...
            wb = ByteBuffer.allocate( 200 );
            wb.put( rdata );
            wb.flip();
            datagram = new OutboundDatagram( wb, IPv4Address.LOOPBACK, 5556 );
            sendOutcome = client2.send( datagram );
            assertTrue( sendOutcome.ok(), "Problem sending: " + sendOutcome.msg() );

            // read the data from client 1 and make sure it's correct...
            readOutcome = client1.receive();
            assertTrue( readOutcome.ok(), "Problem reading: " + readOutcome.msg() );
            rd = readOutcome.info().getData();
            rb = new byte[rd.limit()];
            rd.get(rb);
            assertArrayEquals( rdata, rb, "Data read does not match data transmitted" );
        }

        // shut it all down...
        echo.inboundPipe.close();
        client1.close();
        client2.close();
        engine.shutdown();
    }


    @Test
    void testFastUDP() throws IllegalStateException, InterruptedException {

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // get one UDP client...
        var client1Outcome = UDPClient.getNewInstance( engine, IPv4Address.LOOPBACK, 5556, IPv4Address.LOOPBACK, 5555, 200 );
        assertTrue( client1Outcome.ok(), "Problem creating UDPServer: " + client1Outcome.msg() );
        var client1 = client1Outcome.info();

        // get another UDP client...
        var clientOutcome = UDPClient.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, IPv4Address.LOOPBACK, 5556, 200 );
        assertTrue( clientOutcome.ok(), "Problem creating TCPOutboundPipe: " + clientOutcome.msg() );
        var client2 = clientOutcome.info();

        // create and start our reader thread...
        var reader = new FastUDPRead( client2 );
        reader.start();

        // transmit 1000 serialized datagrams...
        for( int i = 1; i <= 1000; i++ ) {

            // send a random datagram from client 1 to client 2...
            var wb = ByteBuffer.allocate( 40 );
            wb.putInt( i );
            wb.flip();
            var datagram = new OutboundDatagram( wb, IPv4Address.LOOPBACK, 5555 );
            var sendOutcome = client1.send( datagram );
            assertTrue( sendOutcome.ok(), "Problem sending: " + sendOutcome.msg() );
        }

        Thread.sleep( 250 );
        if( reader.isAlive() )
            fail( "Fast reader thread did not complete" );

        // shut it all down...
        client1.close();
        client2.close();
        engine.shutdown();
    }


    private static class FastUDPRead extends Thread {

        private final UDPClient client;
        private int counter = 1;

        private FastUDPRead( final UDPClient _client ) {
            client = _client;
        }

        public void run() {

            // repeat until we get 1000...
            do {
                // read an int and make sure it matches our counter...

                // read the data from the client and make sure it's correct...
                Outcome<InboundDatagram> readOutcome = null;
                try {
                    readOutcome = client.receive();
                }
                catch( IllegalStateException _e ) {
                    fail( "Got unexpected exception: " + _e.getMessage(), _e );
                }
                assertTrue( readOutcome.ok(), "Problem reading: " + readOutcome.msg() );
                var rd = readOutcome.info().getData();
                var rdata = rd.getInt();
                assertEquals( rdata, counter, "Mismatched data and counter" );
                LOGGER.finest( "Read: " + counter );

                // update the counter...
                counter++;
            } while( counter <= 1000 );
        }
    }



    @Test
    void testSimpleTCP() throws IllegalStateException {

        LOGGER.info( "Starting testSimpleTCP()" );

        // get an engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        assertTrue( engineOutcome.ok(), "Problem creating NetworkingEngine: " + engineOutcome.msg() );
        var engine = engineOutcome.info();

        // start up a listener...
        var listenerOutcome = TCPListener.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onAcceptEcho );
        assertTrue( listenerOutcome.ok(), "Problem creating TCPListener: " + listenerOutcome.msg() );
        var listener = listenerOutcome.info();

        // get an outbound pipe and connect it...
        var pipeOutcome = TCPOutboundPipe.getNewInstance( engine, IPv4Address.LOOPBACK );
        assertTrue( pipeOutcome.ok(), "Problem creating TCPOutboundPipe: " + pipeOutcome.msg() );
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
                try {
                    LOGGER.info( "Reading TCP echo" );
                    inboundPipe.read( rb );
                    LOGGER.info( "Writing TCP echo" );
                    inboundPipe.write( rb );
                }
                catch( IllegalStateException _e ) {
                    fail( "Unexpected exception: " + _e.getMessage() );
                }
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
        var listenerOutcome = TCPListener.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onAcceptSlowReceiver );
        assertTrue( listenerOutcome.ok(), "Problem creating TCPListener: " + engineOutcome.msg() );
        var listener = listenerOutcome.info();

        // get an outbound pipe and connect it...
        var pipeOutcome = TCPOutboundPipe.getNewInstance( engine, IPv4Address.LOOPBACK );
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
            Outcome<?> writeOutcome = null;
            try {
                writeOutcome = pipe.write( wb );
            }
            catch( IllegalStateException _e ) {
                fail( "Unexpected exception: " + _e.getMessage() );
            }
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
                Outcome<ByteBuffer> readOutcome = null;
                try {
                    readOutcome = inboundPipe.read( rb );
                }
                catch( IllegalStateException _e ) {
                    fail( "Unexpected exception: " + _e.getMessage() );
                }
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
        var listenerOutcome = TCPListener.getNewInstance( engine, IPv4Address.LOOPBACK, 5555, NetworkingEngineTest::onAcceptMultipleTCP );
        assertTrue( listenerOutcome.ok(), "Problem creating TCPListener: " + engineOutcome.msg() );
        var listener = listenerOutcome.info();

        // get some outbound pipes and connect them...
        Set<TCPOutboundPipe> pipes = new HashSet<>();
        for( int i = 0; i < NUM_PIPES; i++ ) {
            var pipeOutcome = TCPOutboundPipe.getNewInstance( engine, IPv4Address.LOOPBACK );
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
                Outcome<?> writeOutcome = null;
                try {
                    writeOutcome = pipe.write( wb );
                }
                catch( IllegalStateException _e ) {
                    fail( "Unexpected exception: " + _e.getMessage() );
                }
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
                Outcome<ByteBuffer> readOutcome = null;
                try {
                    readOutcome = pipe.read( rb );
                }
                catch( IllegalStateException _e ) {
                    fail( "Unexpected exception: " + _e.getMessage() );
                }
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