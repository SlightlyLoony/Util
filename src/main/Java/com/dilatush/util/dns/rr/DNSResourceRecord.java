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

    public final DNSDomainName name;
    public final DNSRRType type;
    public final DNSRRClass klass;
    public final int ttl;
    public final int dataLength;


    private DNSResourceRecord( final Init _init ) {
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

    }


    protected abstract Outcome<?> encodeChild(  final ByteBuffer _msgBuffer, final Map<String,Integer> _nameOffsets  );


    public static Outcome<? extends DNSResourceRecord> decode( final ByteBuffer _msgBuffer ) {

    }


    protected record Init( DNSDomainName name, DNSRRType type, DNSRRClass klass, int ttl, int dataLength ) {}
}
