package com.dilatush.util.dns;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent a DNS question.  Instances are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSQuestion {

    private static final Outcome.Forge<DNSQuestion> outcome = new Outcome.Forge<>();

    public final DNSDomainName qname;
    public final DNSRRType     qtype;
    public final DNSRRClass    qclass;


    private DNSQuestion( final DNSDomainName _qname, final DNSRRType _qtype, final DNSRRClass _qclass ) {
        qname = _qname;
        qtype = _qtype;
        qclass = _qclass;
    }


    /**
     * Returns the bytes that encode this question.
     *
     * @return the bytes that encode this question.
     */
    public byte[] encode() {

        int length = qname.length + qtype.length + qclass.length;
        byte[] result = new byte[length];
        System.arraycopy( qname.bytes,  0, result, 0,                           qname.length  );
        System.arraycopy( qtype.bytes,  0, result, qname.length,                qtype.length  );
        System.arraycopy( qclass.bytes, 0, result, qname.length + qtype.length, qclass.length );
        return result;
    }


    public static Outcome<DNSQuestion> create(  final DNSDomainName _qname, final DNSRRType _qtype, final DNSRRClass _qclass ) {

        if( isNull( _qname, _qtype, _qclass ) )
            return outcome.notOk( "Required parameters not supplied" );

        return outcome.ok( new DNSQuestion( _qname, _qtype, _qclass ) );
    }


    /**
     * Attempts to create a new instance of this class from the bytes at the current position of the given {@link ByteBuffer}.
     *
     * @param _buffer The {@link ByteBuffer} containing the bytes encoding the question.
     * @return The {@link Outcome Outcome&lt;DNSQuestion&gt;} giving the results of the attempt.
     */
    public static Outcome<DNSQuestion> fromBuffer( final ByteBuffer _buffer ) {

        Outcome<DNSDomainName> domainNameOutcome = DNSDomainName.fromBuffer( _buffer );
        if( !domainNameOutcome.ok() )
            return outcome.notOk( "Could not decode qname: " + domainNameOutcome.msg() );
        Outcome<DNSRRType> rrTypeOutcome = DNSRRType.fromBuffer( _buffer );
        if( !rrTypeOutcome.ok() )
            return outcome.notOk( "Could not decode qtype: " + rrTypeOutcome.msg() );
        Outcome<DNSRRClass> rrClassOutcome = DNSRRClass.fromBuffer( _buffer );
        if( !rrClassOutcome.ok() )
            return outcome.notOk( "Could not decode qclass: " + rrClassOutcome.msg() );

        return outcome.ok( new DNSQuestion( domainNameOutcome.info(), rrTypeOutcome.info(), rrClassOutcome.info() ) );
    }


    public static void main( final String[] args ) {

        Outcome<DNSDomainName> dno = DNSDomainName.fromString( "www.paradiseweather.info" );
        DNSDomainName dn = dno.info();
        Outcome<DNSQuestion> qo = create( dn, DNSRRType.A, DNSRRClass.IN );
        DNSQuestion q = qo.info();
        byte[] qe = q.encode();

        ByteBuffer buffer = ByteBuffer.wrap( qe );

        qo = fromBuffer( buffer );

        new Object().hashCode();
    }
}
