package com.dilatush.util.dns.rr;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSRRClass;
import com.dilatush.util.dns.message.DNSRRType;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent a DNS Resource Record for an IPv6 Internet address.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class AAAA extends DNSResourceRecord {

    private static final Outcome.Forge<AAAA> outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<?>    encodeOutcome = new Outcome.Forge<>();

    /** The IPv6 address associated with this DNS domain name. */
    public final Inet6Address address;


    /**
     * Create a new instance of this class with the given parameters.  Note that this constructor is private and is used only by factory methods and decoders in this class.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _dataLength The length (in bytes) of this resource record's data (not including the resource record's header).
     * @param _address The IPv6 address associated with the named owner of this resource record.
     */
    private AAAA(
            final DNSDomainName _name, final DNSRRClass _klass, final long _ttl, final int _dataLength,
            final Inet6Address _address ) {

        super( new Init( _name, DNSRRType.AAAA, _klass, _ttl, _dataLength ) );
        address = _address;
    }


    /**
     * Create a new instance of this class from the given parameters.  Returns an ok outcome with the newly created instance if there were no problems.  Otherwise, returns a not ok
     * outcome with an explanatory message.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _address The IPv6 address associated with the named owner of this resource record.
     * @return The {@link Outcome Outcome&lt;AAAA&gt;} with the result of this method.
     */
    public static Outcome<AAAA> create(
            final DNSDomainName _name, final DNSRRClass _klass, final int _ttl,
            final Inet6Address _address ) {

        if( isNull( _name, _klass, _address ) )
            return outcome.notOk( "Missing argument (name, class, or address)" );

        return outcome.ok( new AAAA( _name, _klass, _ttl, 16, _address ) );
    }


    /**
     * Create a new instance of this class from the given parameters, with a {@link DNSRRClass} of "IN" (Internet).  Returns an ok outcome with the newly created instance if there
     * were no problems.  Otherwise, returns a not ok outcome with an explanatory message.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _address The IPv6 address associated with the named owner of this resource record.
     * @return The {@link Outcome Outcome&lt;AAAA&gt;} with the result of this method.
     */
    public static Outcome<AAAA> create(
            final DNSDomainName _name, final int _ttl,
            final Inet6Address _address ) {
        return create( _name, DNSRRClass.IN, _ttl, _address);
    }


    /**
     * Decode the resource record from the given DNS message {@link ByteBuffer}. On entry, the message buffer must be positioned at the first byte
     * of this resource record's data.  On exit, the message buffer will be positioned at the first byte following the last byte encoding this resource
     * record.  Returns an ok outcome with the decoded resource record instance if the decoding encountered no problems.  Otherwise, returns a not ok outcome with an explanatory
     * message about the problem encountered.
     *
     * @param _msgBuffer The {@link ByteBuffer} to decode this resource record from.
     * @return The {@link Outcome} of the decoding operation.
     */
    protected static Outcome<AAAA> decode(final ByteBuffer _msgBuffer, final Init _init ) {

        // this resource record's data should always be 16 bytes long; check it...
        if( _init.dataLength() != 16 )
            return outcome.notOk( "Data length is not sixteen bytes" );

        // decode the IPv6 address...
        byte[] addrBytes = new byte[16];
        _msgBuffer.get( addrBytes );
        try {
            Inet6Address addr = (Inet6Address)InetAddress.getByAddress( _init.name().text, addrBytes );
            return outcome.ok( new AAAA(_init.name(), _init.klass(), _init.ttl(), _init.dataLength(), addr ) );
        }

        // this should be impossible, as it is only thrown if the wrong number of bytes is supplied...
        catch (UnknownHostException e) {
            return outcome.notOk( "We got the impossible UnknownHostException" );
        }
    }


    /**
     * Encode the resource data for the concrete resource record class.  On entry, the given DNS message {@link ByteBuffer} is positioned at the first
     * byte of the resource data, and the given map of name offsets contains pointers to all the previously encoded domain names.  On exit, the
     * message buffer must be positioned at the first byte following the resource data.  See {@link DNSDomainName#encode(ByteBuffer, Map)
     * DNSDomainName.encode(ByteBuffer,Map&lt;String,Integer&gt;)} for details about the message compression mechanism.  The outcome returned is ok if
     * the encoding was successful, and not ok (with a message) if there was a problem.  If the result was a buffer overflow, the outcome is not ok
     * with a cause of {@link BufferOverflowException}.
     *
     * @param _msgBuffer The {@link ByteBuffer} to encode this resource record into.
     * @param _nameOffsets The map of domain and sub-domain names that have been directly encoded, and their associated offset.
     * @return the {@link Outcome}, either ok or not ok with an explanatory message.
     */
    @Override
    protected Outcome<?> encodeChild( ByteBuffer _msgBuffer, Map<String, Integer> _nameOffsets ) {

        if( _msgBuffer.remaining() < 16 )
            return encodeOutcome.notOk( new BufferOverflowException() );

        _msgBuffer.put( address.getAddress() );
        return encodeOutcome.ok();
    }


    /**
     * Returns {@code true} if the given {@link DNSResourceRecord} has the same resource data as this record.
     *
     * @param _rr the {@link DNSResourceRecord} to compare with this one.
     * @return {@code true} if this record's resource data is the same as the given record's resource data.
     */
    @Override
    protected boolean sameResourceData( final DNSResourceRecord _rr ) {

        return Arrays.equals( address.getAddress(), ((A)_rr).address.getAddress() );
    }


    /**
     * Return a string representing this instance.
     *
     * @return a string representing this instance.
     */
    public String toString() {
        return type.name() + ": " + address.getHostAddress() + super.toString();
    }
}
