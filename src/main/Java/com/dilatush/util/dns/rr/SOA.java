package com.dilatush.util.dns.rr;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSRRClass;
import com.dilatush.util.dns.message.DNSRRType;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent a DNS Resource Record for the Start of Authority record in the zone of the name server that supplied the answers
 * in a response.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class SOA extends DNSResourceRecord {

    private static final Outcome.Forge<SOA> outcome       = new Outcome.Forge<>();

    /** The domain name of the name server that supplied the primary answers for a response. */
    public final DNSDomainName mname;

    /** The domain name for the mailbox of the person responsible for the zone. */
    public final DNSDomainName rname;

    /** The unsigned 32-bit serial number of the zone. */
    public final long serial;

    /** The unsigned 32-bit interval (in seconds) between refreshes for this zone. */
    public final long refresh;

    /** The unsigned 32-bit minimum interval (in seconds) before retrying a failed refresh for this zone. */
    public final long retry;

    /** The unsigned 32-bit maximum interval (in seconds) before the zone is no longer authoritative. */
    public final long expire;

    /** The unsigned 32-bit minimum time-to-live (in seconds) for any resource record exported from this zone. */
    public final long minimum;


    /**
     * Create a new instance of this class with the given parameters.  Note that this constructor is private and is used only by factory methods and
     * decoders in this class.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _dataLength The length (in bytes) of this resource record's data (not including the resource record's header).
     * @param _mname The domain name of the name server that supplied the primary answers for a response.
     * @param _rname The domain name for the mailbox of the person responsible for the zone.
     * @param _serial The unsigned 32-bit serial number of the zone.
     * @param _refresh The unsigned 32-bit interval (in seconds) between refreshes for this zone.
     * @param _retry The unsigned 32-bit minimum interval (in seconds) before retrying a failed refresh for this zone.
     * @param _expire The unsigned 32-bit maximum interval (in seconds) before the zone is no longer authoritative.
     * @param _minimum The unsigned 32-bit minimum time-to-live (in seconds) for any resource record exported from this zone
     */
    private SOA(
            final DNSDomainName _name, final DNSRRClass _klass, final long _ttl, final int _dataLength,
            final DNSDomainName _mname, final DNSDomainName _rname,
            final long _serial, final long _refresh, final long _retry, final long _expire, final long _minimum ) {

        super( new Init( _name, DNSRRType.SOA, _klass, _ttl, _dataLength ) );

        mname   = _mname;
        rname   = _rname;
        serial  = _serial;
        refresh = _refresh;
        retry   = _retry;
        expire  = _expire;
        minimum = _minimum;
    }


    /**
     * Create a new instance of this class from the given parameters.  Returns an ok outcome with the newly created instance if there were no
     * problems.  Otherwise, returns a not ok outcome with an explanatory message.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _mname The domain name of the name server that supplied the primary answers for a response.
     * @param _rname The domain name for the mailbox of the person responsible for the zone.
     * @param _serial The unsigned 32-bit serial number of the zone.
     * @param _refresh The unsigned 32-bit interval (in seconds) between refreshes for this zone.
     * @param _retry The unsigned 32-bit minimum interval (in seconds) before retrying a failed refresh for this zone.
     * @param _expire The unsigned 32-bit maximum interval (in seconds) before the zone is no longer authoritative.
     * @param _minimum The unsigned 32-bit minimum time-to-live (in seconds) for any resource record exported from this zone
     * @return The {@link Outcome Outcome&lt;SOA&gt;} with the result of this method.
     */
    public static Outcome<SOA> create(
            final DNSDomainName _name, final DNSRRClass _klass, final int _ttl,
            final DNSDomainName _mname, final DNSDomainName _rname,
            final long _serial, final long _refresh, final long _retry, final long _expire, final long _minimum ) {

        if( isNull( _name, _klass, _mname, _rname ) )
            return outcome.notOk( "Missing argument (name, class, mname, or rname)" );

        return outcome.ok( new SOA( _name, _klass, _ttl, 4, _mname, _rname, _serial, _refresh, _retry, _expire, _minimum ) );
    }


    /**
     * Create a new instance of this class from the given parameters, with a {@link DNSRRClass} of "IN" (Internet).  Returns an ok outcome with the
     * newly created instance if there were no problems.  Otherwise, returns a not ok outcome with an explanatory message.
     *
     * @param _name The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _ttl This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _mname The domain name of the name server that supplied the primary answers for a response.
     * @param _rname The domain name for the mailbox of the person responsible for the zone.
     * @param _serial The unsigned 32-bit serial number of the zone.
     * @param _refresh The unsigned 32-bit interval (in seconds) between refreshes for this zone.
     * @param _retry The unsigned 32-bit minimum interval (in seconds) before retrying a failed refresh for this zone.
     * @param _expire The unsigned 32-bit maximum interval (in seconds) before the zone is no longer authoritative.
     * @param _minimum The unsigned 32-bit minimum time-to-live (in seconds) for any resource record exported from this zone
     * @return The {@link Outcome Outcome&lt;SOA&gt;} with the result of this method.
     */
    public static Outcome<SOA> create(
            final DNSDomainName _name, final int _ttl,
            final DNSDomainName _mname, final DNSDomainName _rname,
            final long _serial, final long _refresh, final long _retry, final long _expire, final long _minimum ) {
        return create( _name, DNSRRClass.IN, _ttl, _mname, _rname, _serial, _refresh, _retry, _expire, _minimum );
    }


    /**
     * Decode the resource record from the given DNS message {@link ByteBuffer}. On entry, the message buffer must be positioned at the first byte
     * of this resource record's data.  On exit, the message buffer will be positioned at the first byte following the last byte encoding this resource
     * record.  Returns an ok outcome with the decoded resource record instance if the decoding encountered no problems.  Otherwise, returns a not ok
     * outcome with an explanatory message about the problem encountered.
     *
     * @param _msgBuffer The {@link ByteBuffer} to decode this resource record from.
     * @return The {@link Outcome} of the decoding operation.
     */
    protected static Outcome<SOA> decode(final ByteBuffer _msgBuffer, final Init _init ) {

        // decode the domain name of the name server...
        Outcome<DNSDomainName> nso = DNSDomainName.decode( _msgBuffer );
        if( nso.notOk() )
            return outcome.notOk( nso.msg() );

        // decode the domain name of the responsible person's mailbox...
        Outcome<DNSDomainName> rpo = DNSDomainName.decode( _msgBuffer );
        if( rpo.notOk() )
            return outcome.notOk( rpo.msg() );

        // do we have enough bytes remaining to decode five 32-bit integers (20 bytes)?
        if( _msgBuffer.remaining() < 20 )
            return outcome.notOk( "Insufficient bytes in message buffer" );

        // we did have room, so decode them all...
        long serial  = 0xFFFFFFFFL & _msgBuffer.getInt();
        long refresh = 0xFFFFFFFFL & _msgBuffer.getInt();
        long retry   = 0xFFFFFFFFL & _msgBuffer.getInt();
        long expire  = 0xFFFFFFFFL & _msgBuffer.getInt();
        long minimum = 0xFFFFFFFFL & _msgBuffer.getInt();

        // create and return our instance...
        return outcome.ok( new SOA(_init.name(), _init.klass(), _init.ttl(), _init.dataLength(),
                nso.info(), rpo.info(), serial, refresh, retry, expire, minimum ) );
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

        // encode the name server domain name...
        Outcome<?> dno = mname.encode( _msgBuffer, _nameOffsets );
        if( dno.notOk() ) return dno;

        // encode the responsible person's mailbox domain name...
        Outcome<?> rpo = rname.encode( _msgBuffer, _nameOffsets );
        if( rpo.notOk() ) return rpo;

        // do we have enough room remaining to encode five 32-bit integers (20 bytes)?
        if( _msgBuffer.remaining() < 20 )
            return outcome.notOk( new BufferOverflowException() );

        // encode our five numbers...
        _msgBuffer.putInt( (int) serial  );
        _msgBuffer.putInt( (int) refresh );
        _msgBuffer.putInt( (int) retry   );
        _msgBuffer.putInt( (int) expire  );
        _msgBuffer.putInt( (int) minimum );

        // if we get here, all is well...
        return outcome.ok();
    }


    /**
     * Return a string representing this instance.
     *
     * @return a string representing this instance.
     */
    public String toString() {
        return type.name() + ": " + mname.text + super.toString();
    }
}
