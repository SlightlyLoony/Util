package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static java.lang.Thread.sleep;

/**
 * Implements an asynchronous resolver for DNS queries to a particular DNS server.  Any number of resolvers can be instantiated concurrently, but
 * only one resolver for each DNS server.  Each resolver can process any number of queries concurrently.  Each resolver can connect using either UDP
 * or TCP (normally UDP, but switching to TCP as needed).  All resolver I/O is performed by a single thread owned by the singleton
 * {@link DNSResolverRunner}, which is instantiated on demand (when any {@link DNSResolver} is instantiated).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSResolver {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<DNSResolver> createOutcome = new Outcome.Forge<>();

    private static DNSResolverRunner runner;  // the singleton instance of the resolver runner...

    private final InetSocketAddress serverAddress;  // the DNS server's address for this resolver...

    protected final DatagramChannel   udpChannel;
    protected final SocketChannel     tcpChannel;
    protected final AtomicInteger     sendsOutstanding;
    protected final Deque<ByteBuffer> sendData;


    private DNSResolver( final InetSocketAddress _serverAddress, final DatagramChannel _udpChannel, final SocketChannel _tcpChannel ) {

        serverAddress = _serverAddress;
        udpChannel    = _udpChannel;
        tcpChannel    = _tcpChannel;

        sendsOutstanding = new AtomicInteger();
        sendData         = new LinkedBlockingDeque<>();
    }


    //TODO better comments...
    /**
     * Query for host address IPv4.
     *
     * @param _domain
     * @param _handler
     * @param _timeoutMillis
     */
    public void query( final String _domain, final Consumer<Outcome<DNSMessage>> _handler, final long _timeoutMillis ) {

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( true );

        // TODO: flesh out this prototype code...
        builder.addQuestion( DNSQuestion.create( DNSDomainName.fromString( _domain ).info(), DNSRRType.A ).info() );
        DNSMessage queryMsg = builder.getMessage();
        ByteBuffer data = queryMsg.encode().info();

        DNSQuery query = new DNSQuery( Transport.UDP, new Timeout( 500, this::timeoutHandler ), _handler, queryMsg, data, this );
        runner.send( query );
    }


    private void timeoutHandler() {
        LOGGER.info( "timeout handler" );
    }


    private static void ensureRunner() throws IOException {

        if( runner != null )
            return;

        synchronized( DNSResolverRunner.class ) {

            if( runner != null )
                return;

            runner = new DNSResolverRunner();
        }
    }


    public static Outcome<DNSResolver> create(  final InetSocketAddress _serverAddress  ) {

        if( isNull( _serverAddress ) )
            return createOutcome.notOk( "Server address is missing (null)" );

        try {
            ensureRunner();
            DatagramChannel udp = DatagramChannel.open();
            SocketChannel   tcp = SocketChannel.open();
            DNSResolver     resolver = new DNSResolver( _serverAddress, udp, tcp );
            udp.configureBlocking( false );
            udp.bind( null );
            udp.connect( _serverAddress );
            tcp.configureBlocking( false );
            tcp.bind( null );
            runner.register( resolver, udp, tcp );
            return createOutcome.ok( resolver );
        }

        catch( IOException _e ) {
            return createOutcome.notOk( "Problem creating DNSResolver", _e );
        }
    }


    //    public static void main( final String[] _args ) throws IOException {
//
//        DNSMessage.Builder b = new DNSMessage.Builder();
//        b.setId( 666 );
//        b.setOpCode( DNSOpCode.QUERY );
//        b.setRecurse( true );
//
//        DNSQuestion q = DNSQuestion.create( DNSDomainName.fromString( "cnn.com" ).info(), DNSRRType.TXT ).info();
//        //DNSQuestion q1 = DNSQuestion.create( DNSDomainName.fromString( "www.paradiseweather.info" ).info(), DNSRRType.CNAME ).info();
//        b.addQuestion( q );
//        //b.addQuestion( q1 );
//        DNSMessage m = b.getMessage();
//
//        Outcome<ByteBuffer> eo = m.encode();
//        ByteBuffer bb = eo.info();
//
//        InetAddress server = InetAddress.getByName( "10.2.5.200" );
//        byte[] packetBytes = new byte[bb.limit()];
//        bb.get( packetBytes );
//        DatagramPacket packet = new DatagramPacket( packetBytes, packetBytes.length, server, 53 );
//        DatagramSocket socket = new DatagramSocket();
//        socket.send( packet );
//
//        byte[] buff = new byte[512];
//        packet = new DatagramPacket( buff, buff.length );
//        socket.receive( packet );
//        int len = packet.getLength();
//        ByteBuffer rb = ByteBuffer.wrap( buff, 0, len );
//
//        Outcome<DNSMessage> decodeOutcome = DNSMessage.decode( rb );
//        DNSMessage received = decodeOutcome.info();
//
//        b.hashCode();
//    }
}
