package com.dilatush.util.dns.rr;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSDomainName;
import com.dilatush.util.dns.DNSRRClass;
import com.dilatush.util.dns.DNSRRType;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent a DNS Resource Record for the canonical domain name associated with this resource record's domain name (the alias).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class CNAME extends DNSResourceRecord {

    private static final Outcome.Forge<CNAME> outcome       = new Outcome.Forge<>();

    /** The canonical (primary) domain name associated with this DNS domain name (the alias). */
    public final DNSDomainName cname;


    /**
     * Create a new instance of this class with the given parameters.  Note that this constructor is private and is used only by factory methods and decoders in this class.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _dataLength The length (in bytes) of this resource record's data (not including the resource record's header).
     * @param _cname The canonical domain name associated with the named owner of this resource record.
     */
    private CNAME(
            final DNSDomainName _name, final DNSRRClass _klass, final long _ttl, final int _dataLength,
            final DNSDomainName _cname ) {

        super( new Init( _name, DNSRRType.CNAME, _klass, _ttl, _dataLength ) );
        cname = _cname;
    }


    /**
     * Create a new instance of this class from the given parameters.  Returns an ok outcome with the newly created instance if there were no problems.  Otherwise, returns a not ok
     * outcome with an explanatory message.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _cname The canonical domain name associated with the named owner of this resource record.
     * @return The {@link Outcome Outcome&lt;CNAME&gt;} with the result of this method.
     */
    public static Outcome<CNAME> create(
            final DNSDomainName _name, final DNSRRClass _klass, final int _ttl,
            final DNSDomainName _cname ) {

        if( isNull( _name, _klass, _cname ) )
            return outcome.notOk( "Missing argument (name, class, or cname)" );

        return outcome.ok( new CNAME( _name, _klass, _ttl, 4, _cname ) );
    }


    /**
     * Create a new instance of this class from the given parameters, with a {@link DNSRRClass} of "IN" (Internet).  Returns an ok outcome with the newly created instance if there
     * were no problems.  Otherwise, returns a not ok outcome with an explanatory message.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _cname The canonical domain name associated with the named owner of this resource record.
     * @return The {@link Outcome Outcome&lt;CNAME&gt;} with the result of this method.
     */
    public static Outcome<CNAME> create(
            final DNSDomainName _name, final int _ttl,
            final DNSDomainName _cname ) {
        return create( _name, DNSRRClass.IN, _ttl, _cname );
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
    protected static Outcome<CNAME> decode(final ByteBuffer _msgBuffer, final Init _init ) {

        // decode the domain name...
        Outcome<DNSDomainName> dnOutcome = DNSDomainName.decode( _msgBuffer );
        if( dnOutcome.notOk() )
            return outcome.notOk(dnOutcome.msg() );

        // create and return our instance...
        return outcome.ok( new CNAME(_init.name(), _init.klass(), _init.ttl(), _init.dataLength(), dnOutcome.info() ) );
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

        // encode the cname and skedaddle...
        return cname.encode( _msgBuffer, _nameOffsets );
    }


    /**
     * Return a string representing this instance.
     *
     * @return a string representing this instance.
     */
    public String toString() {
        return type.name() + ": " + cname.text + super.toString();
    }
}
