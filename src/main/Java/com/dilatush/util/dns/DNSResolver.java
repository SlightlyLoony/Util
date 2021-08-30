package com.dilatush.util.dns;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSResolver {


    public static void main( final String[] _args ) throws IOException {

        DNSMessage.Builder b = new DNSMessage.Builder();
        b.setId( 666 );
        b.setOpCode( DNSOpCode.QUERY );
        b.setRecurse( true );

        DNSQuestion q = DNSQuestion.create( DNSDomainName.fromString( "cnn.com" ).info(), DNSRRType.TXT ).info();
        //DNSQuestion q1 = DNSQuestion.create( DNSDomainName.fromString( "www.paradiseweather.info" ).info(), DNSRRType.CNAME ).info();
        b.addQuestion( q );
        //b.addQuestion( q1 );
        DNSMessage m = b.getMessage();

        Outcome<ByteBuffer> eo = m.encode();
        ByteBuffer bb = eo.info();

        InetAddress server = InetAddress.getByName( "10.2.5.200" );
        byte[] packetBytes = new byte[bb.limit()];
        bb.get( packetBytes );
        DatagramPacket packet = new DatagramPacket( packetBytes, packetBytes.length, server, 53 );
        DatagramSocket socket = new DatagramSocket();
        socket.send( packet );

        byte[] buff = new byte[512];
        packet = new DatagramPacket( buff, buff.length );
        socket.receive( packet );
        int len = packet.getLength();
        ByteBuffer rb = ByteBuffer.wrap( buff, 0, len );

        Outcome<DNSMessage> decodeOutcome = DNSMessage.decode( rb );
        DNSMessage received = decodeOutcome.info();

        b.hashCode();
    }
}
