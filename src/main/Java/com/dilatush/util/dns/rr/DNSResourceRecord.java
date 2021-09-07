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

/**
 * The abstract base class for all concrete resource record classes, defining their common API as well as holding the elements of the resource
 * record header.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */

public abstract class DNSResourceRecord {

    private static final Outcome.Forge<? extends DNSResourceRecord> outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<?>                           encodeOutcome = new Outcome.Forge<>();


    /** The name of the node to which this resource record pertains (the owner) */
    public final DNSDomainName name;

    /** The type of this resource record. */
    public final DNSRRType type;

    /** The class of this resource record.  In the real world, this is always IN (internet). */
    public final DNSRRClass klass;

    /** The unsigned 32 bit time-to-live (in seconds) for this resource record.  A value of zero means this record may not be cached.
     *  Though this is a 32 bit value, it is held in a 64 bit long so that all valid values are positive.
     */
    public final long ttl;

    /** The unsigned 16 bit length of the resource data field. Though this is a 16 bit value, it is held in a 32 bit int so that all valid
     *  values are positive.
     */
    public final int dataLength;


    /**
     * Constructor used only by child classes.
     *
     * @param _init The initializer for this instance.
     */
    protected DNSResourceRecord( final Init _init ) {
        name       = _init.name;
        type       = _init.type;
        klass      = _init.klass;
        ttl        = _init.ttl;
        dataLength = _init.dataLength;
    }


    /**
     * Encode this resource record into the given DNS message {@link ByteBuffer} at the current position, using message compression when possible.
     * See {@link DNSDomainName#encode(ByteBuffer,Map) DNSDomainName.encode(ByteBuffer,Map&lt;String,Integer&gt;)} for details about the message
     * compression mechanism.  The outcome returned is ok if the encoding was successful, and not ok (with a message) if there was a problem.  If the
     * result was a buffer overflow, the outcome is not ok with a cause of {@link BufferOverflowException}.
     *
     * @param _msgBuffer The {@link ByteBuffer} to encode this instance into.
     * @param _nameOffsets The map of domain and sub-domain names that have been directly encoded, and their associated offset.
     * @return the {@link Outcome}, either ok or not ok with an explanatory message.
     */
    public Outcome<?> encode( final ByteBuffer _msgBuffer, final Map<String,Integer> _nameOffsets ) {

        // encode the resource record header, common to all types of resource records...

        // first the domain name that this resource record pertains to...
        Outcome<?> result = name.encode( _msgBuffer, _nameOffsets);
        if( result.notOk() ) return result;

        // then the resource record type...
        result = type.encode( _msgBuffer );
        if( result.notOk() ) return result;

        // then the resource record class (which is basically always IN for internet)...
        result = klass.encode( _msgBuffer );
        if( result.notOk() ) return result;

        // at this point we must have at least 6 bytes to encode the TTL and resource data length fields, so check that...
        if( _msgBuffer.remaining() < 6 )
            return encodeOutcome.notOk( new BufferOverflowException() );

        // encode the TTL...
        _msgBuffer.putInt( (int)ttl );

        // the buffer is now positioned where the resource data length is encoded; remember this as we have come back and put the actual length in...
        int dataLengthPos = _msgBuffer.position();

        // for now, just stuff a placeholder here to get the buffer positioned for the resource data...
        _msgBuffer.putShort( (short) 0 );  // placeholder until we have the actual data length...

        // remember this position so that we can later calculate the resource data's length...
        int dataStartPos = _msgBuffer.position();

        // encode the child class' data...
        result = encodeChild( _msgBuffer, _nameOffsets );
        if( result.notOk() ) return result;

        // calculate the size of the resource's data, and stuff that into the length field...
        _msgBuffer.putShort( dataLengthPos, (short)(_msgBuffer.position() - dataStartPos) );

        // somehow we made it through all this successfully!
        return encodeOutcome.ok();
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
    protected abstract Outcome<?> encodeChild(  final ByteBuffer _msgBuffer, final Map<String,Integer> _nameOffsets  );


    /**
     * Decode the resource record from the given DNS message {@link ByteBuffer}. On entry, the message buffer must be positioned at the first byte
     * of this resource record.  On exit, the message buffer will be positioned at the first byte following the last byte encoding this resource
     * record.  Returns an ok outcome with the decoded resource record instance (which will be a subclass of {@link DNSResourceRecord}) if the
     * decoding encountered no problems.  Otherwise, returns a not ok outcome with an explanatory message about the problem encountered.
     *
     * @param _msgBuffer The {@link ByteBuffer} to decode this resource record from.
     * @return The {@link Outcome} of the decoding operation.
     */
    public static Outcome<? extends DNSResourceRecord> decode( final ByteBuffer _msgBuffer ) {

        // decode the resource record header, common to all types of resource records...

        // decode the domain name that this resource pertains to...
        Outcome<DNSDomainName> nameOutcome = DNSDomainName.decode( _msgBuffer );
        if( nameOutcome.notOk() )
            return outcome.notOk( nameOutcome.msg() );

        // decode this resource record type...
        int typeCodePos = _msgBuffer.position();
        Outcome<DNSRRType> typeOutcome = DNSRRType.decode( _msgBuffer );
        if( typeOutcome.notOk() )
            return outcome.notOk(typeOutcome.msg() );

        // decode this resource record class (which is basically always IN for internet)...
        Outcome<DNSRRClass> classOutcome = DNSRRClass.decode( _msgBuffer );
        if(classOutcome.notOk() )
            return outcome.notOk( classOutcome.msg() );

        // at this point we must have at least 6 bytes left to decode, so check that...
        if( _msgBuffer.remaining() < 6 )  // four bytes of TTL, two bytes of dataLength...
            return outcome.notOk( "Insufficient room in message buffer" );

        // decode the TTL and the resource data length...
        long ttl = 0xFFFFFFFFL & _msgBuffer.getInt();
        int dataLength = _msgBuffer.getShort();

        // make sure we have all the resource data...
        if( _msgBuffer.remaining() < dataLength )
            return outcome.notOk( "Buffer underflow: not enough bytes for data length" );

        // create the base class initializer, so we can hand it to the subclass decoder...
        Init init = new Init( nameOutcome.info(), typeOutcome.info(), classOutcome.info(), ttl, (short) dataLength );

        // remember where our resource data starts, so we can later calculate how much data was actually decoded...
        int firstDataPos = _msgBuffer.position();

        // call the appropriate subclass decoder to decode the resource data...
        Outcome<? extends DNSResourceRecord> result = switch( typeOutcome.info() ) {

                case A     -> A.decode            ( _msgBuffer, init );
                case AAAA  -> AAAA.decode         ( _msgBuffer, init );
                case CNAME -> CNAME.decode        ( _msgBuffer, init );
                case NS    -> NS.decode           ( _msgBuffer, init );
                case SOA   -> SOA.decode          ( _msgBuffer, init );
                case TXT   -> TXT.decode          ( _msgBuffer, init );
                default    -> UNIMPLEMENTED.decode( _msgBuffer, init, typeCodePos );
            };

        // if the subclass decoder had a problem, bail out...
        if( result.notOk() )
            return result;

        // make sure we decoded the same number of bytes that we decoded for the resource data length...
        int decodedDataBytes = _msgBuffer.position() - firstDataPos;
        if( decodedDataBytes != init.dataLength )
            return outcome.notOk( "Data length encoded as " + init.dataLength + ", but decoded " + decodedDataBytes + " bytes" );

        // whew - we made it!
        return result;
    }


    /**
     * Return a string representing this instance.
     *
     * @return a string representing this instance.
     */
    public String toString() {
        long longTtl = 0xFFFFFFFFL & ttl;
        int intLen = 0xFFFFFF & dataLength;
        return " (name: " + name.text + ", type: " + type.toString() + ", class: "
                + klass.toString() + ", ttl: " + longTtl + ", data bytes: " + intLen + ")";
    }


    /** The base class initializer, used only by subclasses and decoders. */
    protected record Init( DNSDomainName name, DNSRRType type, DNSRRClass klass, long ttl, int dataLength ) {}
}
