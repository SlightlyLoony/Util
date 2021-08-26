package com.dilatush.util.dns;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.dilatush.util.General.isNull;

/**
 * Enumerates the possible resource record types (including QTYPES), and defines their codes and text representations.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public enum DNSRRType {

    A      ( false, "A",     1   ),    // a host address
    NS     ( false, "NS",    2   ),    // an authoritative name server...
    MD     ( false, "MD",    3   ),    // a mail destination (Obsolete - use MX)...
    MF     ( false, "MF",    4   ),    // a mail forwarder (Obsolete - use MX)...
    CNAME  ( false, "CNAME", 5   ),    // the canonical name for an alias...
    SOA    ( false, "SOA",   6   ),    // marks the start of a zone of authority...
    MB     ( false, "MB",    7   ),    // a mailbox domain name (EXPERIMENTAL)...
    MG     ( false, "MG",    8   ),    // a mail group member (EXPERIMENTAL)...
    MR     ( false, "MR",    9   ),    // a mail rename domain name (EXPERIMENTAL)...
    NULL   ( false, "NULL",  10  ),    // a null RR (EXPERIMENTAL)...
    WKS    ( false, "WKS",   11  ),    // a well known service description...
    PTR    ( false, "PTR",   12  ),    // a domain name pointer...
    HINFO  ( false, "HINFO", 13  ),    // host information...
    MINFO  ( false, "MINFO", 14  ),    // mailbox or mail list information...
    MX     ( false, "MX",    15  ),    // mail exchange...
    TXT    ( false, "TXT",   16  ),    // text strings...
    AXFR   ( true,  "AXFR",  252 ),    // A request for a transfer of an entire zone...
    MAILB  ( true,  "MAILB", 253 ),    // A request for mailbox-related records (MB, MG or MR)...
    MAILA  ( true,  "MAILA", 254 ),    // A request for mail agent RRs (Obsolete - see MX)...
    ASTER  ( true,  "*",     255 );    // A request for all records...


    private static final Outcome.Forge<DNSRRType> outcome = new Outcome.Forge<>();

    /** {@code true} if this type is only valid in a query. */
    public final boolean isQTYPE;

    /** The textual representation of this type. */
    public final String  text;

    /** The value (code) of this type in a resource record. */
    public final int     code;

    /** The bytes that encode this type in a resource record. */
    public final byte[]  bytes;

    /** The number of bytes that encode this type in a resource record. */
    public final int     length;


    private static final Map<String,DNSRRType>   fromText = new HashMap<>();  // mapping of text representation to instances of this class...
    private static final Map<Integer, DNSRRType> fromCode = new HashMap<>();  // mapping of values (codes) to instances of this class...

    // initialized statically because we can't do it from the constructor...
    // see this good explanation:  https://stackoverflow.com/questions/443980/why-cant-enums-constructor-access-static-fields
    static {
        for( DNSRRType t : DNSRRType.values() ) {
            fromText.put( t.text, t );
            fromCode.put( t.code, t );
        }
    }
        

    DNSRRType( final boolean _isQTYPE, final String _text, final int _code ) {

        isQTYPE = _isQTYPE;
        text = _text;
        code = _code;
        length = 2;  // always encoded as 16 bits...
        bytes = new byte[2];
        bytes[0] = (byte)(code >>> 8);
        bytes[1] = (byte)(code);
    }


    /**
     * Returns the {@link DNSRRType} instance with the given value (code), or {@code null} if there are none.
     *
     * @param _code The value (code) for the desired {@link DNSRRType}.
     * @return the {@link DNSRRType} with the given value (code), or {@code null} if there are none.
     */
    public static DNSRRType fromCode( final int _code ) {
        return fromCode.get( _code );
    }


    /**
     * Returns the {@link DNSRRType} instance with the given text representation, or {@code null} if there are none.
     *
     * @param _text The text representation for the desired {@link DNSRRType}.
     * @return the {@link DNSRRType} with the given value (code), or {@code null} if there are none.
     */
    public static DNSRRType fromText( final String _text ) {
        return fromText.get( _text );
    }


    /**
     * Attempts to create an instance of {@link DNSRRType} from the given buffer, using bytes at the buffer's current position.  If the attempt is
     * successful, then the returned outcome is ok and the newly created instance of {@link DNSRRType} is the information in the outcome.  If the
     * attempt fails, then the outcome is not ok and the message explains why.
     *
     * @param _buffer The {@link ByteBuffer} containing the bytes encoding the label.
     * @return The {@link Outcome Outcome&lt;DNSRRType&gt;} giving the results of the attempt.
     */
    public static Outcome<DNSRRType> fromBuffer( final ByteBuffer _buffer ) {

        if( isNull( _buffer ) )
            return outcome.notOk( "Buffer is missing" );
        if( _buffer.remaining() < 2 )
            return outcome.notOk( "Buffer has insufficient bytes remaining");

        // extract the 16 bit code (value)...
        int code = 0xffff & _buffer.getShort();

        // try to decode it...
        DNSRRType result = fromCode( code );

        if( isNull( result ) )
            return outcome.notOk( "Could not decode resource record type code: " + code );

        return outcome.ok( result );
    }
}
