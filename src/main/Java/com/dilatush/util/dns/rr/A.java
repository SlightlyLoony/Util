package com.dilatush.util.dns.rr;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSDomainName;
import com.dilatush.util.dns.DNSRRClass;
import com.dilatush.util.dns.DNSRRType;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent an IPv4 Internet address.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class A extends DNSResourceRecord {

    private static final Outcome.Forge<A> outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<?> encodeOutcome = new Outcome.Forge<>();

    public final Inet4Address address;


    private A(
            final DNSDomainName _name, final DNSRRClass _klass, final int _ttl, final int _dataLength,
            final Inet4Address _address ) {

        super( new Init( _name, DNSRRType.A, _klass, _ttl, _dataLength ) );
        address = _address;
    }


    public static Outcome<A> create(
            final DNSDomainName _name, final DNSRRClass _klass, final int _ttl,
            final Inet4Address _address ) {

        if( isNull( _name, _klass, _address ) )
            return outcome.notOk( "Missing argument (name, class, or address)" );

        return outcome.ok( new A( _name, _klass, _ttl, 4, _address ) );
    }


    public static Outcome<A> create(
            final DNSDomainName _name, final int _ttl,
            final Inet4Address _address ) {
        return create( _name, DNSRRClass.IN, _ttl, _address);
    }


    public static Outcome<A> decode( final ByteBuffer _msgBuffer, final Init _init ) {

        if( _init.dataLength() != 4 )
            return outcome.notOk( "Data length is not four bytes" );

        byte[] addrBytes = new byte[4];
        _msgBuffer.get( addrBytes );
        try {
            Inet4Address addr = (Inet4Address)InetAddress.getByAddress( addrBytes );
            return outcome.ok( new A(_init.name(), _init.klass(), _init.ttl(), _init.dataLength(), addr ) );
        }

        // this should be impossible, as it is only thrown if the wrong number of bytes is supplied...
        catch (UnknownHostException e) {
            return outcome.notOk( "We got the impossible UnknownHostException" );
        }
    }


    @Override
    protected Outcome<?> encodeChild( ByteBuffer _msgBuffer, Map<String, Integer> _nameOffsets ) {

        if( _msgBuffer.remaining() < 4 )
            return encodeOutcome.notOk( "Insufficient space in buffer" );

        _msgBuffer.put( address.getAddress() );
        return encodeOutcome.ok();
    }


    public String toString() {
        return type.name() + ": " + address.getHostAddress() + super.toString();
    }
}
