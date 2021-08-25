package com.dilatush.util.dns;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the possible resource class types, and defines their codes and text representations.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public enum DNSRRClass {

    IN     ( false, "IN", 1   ),    // A request for a transfer of an entire zone...
    CS     ( false, "CS", 2   ),    // A request for mailbox-related records (MB, MG or MR)...
    CH     ( false, "CH", 3   ),    // A request for mail agent RRs (Obsolete - see MX)...
    HS     ( false, "HS", 4   ),    // A request for all records...
    ASTER  ( true,  "*",  255 );    // A request for all classes...


    /** {@code true} if this class is only valid in a query. */
    public final boolean isQCLASS;

    /** The textual representation of this class. */
    public final String  text;

    /** The value (code) of this class in a resource record. */
    public final int     code;


    private static final Map<String, DNSRRClass>   fromText = new HashMap<>();  // mapping of text representation to instances of this class...
    private static final Map<Integer, DNSRRClass>  fromCode = new HashMap<>();  // mapping of values (codes) to instances of this class...

    // initialized statically because we can't do it from the constructor...
    // see this good explanation:  https://stackoverflow.com/questions/443980/why-cant-enums-constructor-access-static-fields
    static {
        for( DNSRRClass t : DNSRRClass.values() ) {
            fromText.put( t.text, t );
            fromCode.put( t.code, t );
        }
    }


    DNSRRClass( final boolean _isQCLASS, final String _text, final int _code ) {

        isQCLASS = _isQCLASS;
        text = _text;
        code = _code;
    }


    /**
     * Returns the {@link DNSRRClass} instance with the given value (code), or {@code null} if there are none.
     *
     * @param _code The value (code) for the desired {@link DNSRRClass}.
     * @return the {@link DNSRRClass} with the given value (code), or {@code null} if there are none.
     */
    public static DNSRRClass fromCode( final int _code ) {
        return fromCode.get( _code );
    }


    /**
     * Returns the {@link DNSRRClass} instance with the given text representation, or {@code null} if there are none.
     *
     * @param _text The text representation for the desired {@link DNSRRClass}.
     * @return the {@link DNSRRClass} with the given value (code), or {@code null} if there are none.
     */
    public static DNSRRClass fromText( final String _text ) {
        return fromText.get( _text );
    }
}
