package com.dilatush.util.dns.rr;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSDomainName;
import com.dilatush.util.dns.DNSRRClass;
import com.dilatush.util.dns.DNSRRType;

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


    public final DNSDomainName name;
    public final DNSRRType type;
    public final DNSRRClass klass;
    public final int ttl;
    public final int dataLength;


    protected DNSResourceRecord( final Init _init ) {
        name       = _init.name;
        type       = _init.type;
        klass      = _init.klass;
        ttl        = _init.ttl;
        dataLength = _init.dataLength;
    }


    /**
     * Encode this domain name into the given DNS message {@link ByteBuffer} at the current position, using message compression when possible.  The
     * given map of name offsets is indexed by string representations of domain names and sub-domain names that are already directly encoded in the
     * message.  If this domain name (or its sub-domain names) matches any of them, an offset is encoded instead of the actual characters.  Otherwise,
     * the name is directly encoded.  Any directly encoded domains or sub-domains is added to the map of offsets.  For example, the first time (in a
     * given message) that "www.cnn.com" is encoded, offsets are added for "www.cnn.com", "cnn.com", and "com".  The outcome returned is ok if the
     * encoding was successful, and not ok (with a message) if there was a problem.
     *
     * @param _msgBuffer The {@link ByteBuffer} to encode this instance into.
     * @param _nameOffsets The map of domain and sub-domain names that have been directly encoded, and their associated offset.
     * @return the {@link Outcome}, either ok or not ok with an explanatory message.
     */
    public Outcome<?> encode( final ByteBuffer _msgBuffer, final Map<String,Integer> _nameOffsets ) {

        Outcome<?> result = name.encode( _msgBuffer, _nameOffsets);
        if( result.notOk() )
            return result;

        result = type.encode( _msgBuffer );
        if( result.notOk() )
            return result;

        result = klass.encode( _msgBuffer );
        if( result.notOk() )
            return result;

        if( _msgBuffer.remaining() < 6 )
            return encodeOutcome.notOk( "Insufficient space in buffer" );

        _msgBuffer.putInt( ttl );
        int dataLengthPos = _msgBuffer.position();
        _msgBuffer.putShort( (short) 0 );  // placeholder until we have the actual data length...
        int dataStartPos = _msgBuffer.position();

        result = encodeChild( _msgBuffer, _nameOffsets );
        if( result.notOk() )
            return result;

        _msgBuffer.putShort( dataLengthPos, (short)(_msgBuffer.position() - dataStartPos) );

        return encodeOutcome.ok();
    }


    protected abstract Outcome<?> encodeChild(  final ByteBuffer _msgBuffer, final Map<String,Integer> _nameOffsets  );


    public static Outcome<? extends DNSResourceRecord> decode( final ByteBuffer _msgBuffer ) {

        Outcome<DNSDomainName> nameOutcome = DNSDomainName.decode( _msgBuffer );
        if( nameOutcome.notOk() )
            return outcome.notOk( nameOutcome.msg() );

        Outcome<DNSRRType> typeOutcome = DNSRRType.decode( _msgBuffer );
        if( typeOutcome.notOk() )
            return outcome.notOk(typeOutcome.msg() );

        Outcome<DNSRRClass> classOutcome = DNSRRClass.decode( _msgBuffer );
        if(classOutcome.notOk() )
            return outcome.notOk( classOutcome.msg() );

        if( _msgBuffer.remaining() < 6 )  // four bytes of TTL, two bytes of dataLength...
            return outcome.notOk( "Insufficient room in message buffer" );

        int ttl = _msgBuffer.getInt();
        int dataLength = _msgBuffer.getShort();

        if( _msgBuffer.remaining() < dataLength )
            return outcome.notOk( "Buffer underflow: not enough bytes for data length" );

        Init init = new Init(nameOutcome.info(), typeOutcome.info(), classOutcome.info(), ttl, (short) dataLength );

        int beforeDataPos = _msgBuffer.position();

        Outcome<? extends DNSResourceRecord> result = switch( typeOutcome.info() ) {
                case A -> A.decode( _msgBuffer, init );
                default -> outcome.notOk( "Unimplemented resource record type: " + typeOutcome.info().getClass().getSimpleName() );
            };

        if( result.notOk() )
            return result;

        int decodedDataBytes = _msgBuffer.position() - beforeDataPos;
        if( decodedDataBytes != init.dataLength )
            return outcome.notOk( "Data length encoded as " + init.dataLength + ", but decoded " + decodedDataBytes + " bytes" );

        return result;
    }


    public String toString() {
        long longTtl = 0xFFFFFFFFL & ttl;
        int intLen = 0xFFFFFF & dataLength;
        return " (name: " + name.text + ", type: " + type.toString() + ", class: "
                + klass.toString() + ", ttl: " + longTtl + ", data bytes: " + intLen + ")";
    }


    protected record Init( DNSDomainName name, DNSRRType type, DNSRRClass klass, int ttl, int dataLength ) {}
}
