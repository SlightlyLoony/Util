package com.dilatush.util.dns;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.util.Map;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent a DNS question, with a domain name (the subject of the question), the resource record type desired, and the
 * resource record.  Instances are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class DNSQuestion {

    private static final Outcome.Forge<DNSQuestion> outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<?>           encodeOutcome = new Outcome.Forge<>();

    /** The domain name to ask a question about. */
    public final DNSDomainName qname;

    /** The type of resource records wanted that pertain to the queried domain name. */
    public final DNSRRType     qtype;

    /** The class of resource records wanted that pertain to the queried domain name. */
    public final DNSRRClass    qclass;


    /**
     * Creates a new instance of this class.  Note that this is a private constructor, used only by factory methods and decoders.
     *
     * @param _qname  The domain name to ask a question about.
     * @param _qtype  The type of resource records wanted that pertain to the queried domain name.
     * @param _qclass  The class of resource records wanted that pertain to the queried domain name.
     */
    private DNSQuestion( final DNSDomainName _qname, final DNSRRType _qtype, final DNSRRClass _qclass ) {
        qname  = _qname;
        qtype  = _qtype;
        qclass = _qclass;
    }


    /**
     * Encodes this instance into the given {@link ByteBuffer} at its current position.  If the encoding was successful, an ok {@link Outcome} is
     * returned.  Otherwise, a not ok {@link Outcome} with an explanatory message is returned.
     *
     * @param _msgBuffer The {@link ByteBuffer} to encode this instance into.
     * @param _nameOffsets The map of domain and sub-domain names that have been directly encoded, and their associated offset.
     * @return the bytes that encode this question.
     */
    public Outcome<?> encode( final ByteBuffer _msgBuffer, final Map<String,Integer> _nameOffsets ) {

        // encode the qname...
        Outcome<?> result = qname.encode( _msgBuffer, _nameOffsets );
        if( !result.ok() )
            return result;

        // encode the qtype...
        result = qtype.encode( _msgBuffer );
        if( !result.ok() )
            return result;

        // encode the qclass...
        return qclass.encode( _msgBuffer );
    }


    /**
     * Return a string representing this instance.
     *
     * @return a string representing this instance.
     */
    public String toString() {
        return "DNS Question: " + qname.text + ", type: " + qtype.text + ", class: " + qclass.text;
    }


    /**
     * Attempts to create a new instance of this class with the given parameters.  If successful, an ok outcome containing the new instance is
     * returned.  Otherwise, a not ok outcome is returned with a message explaining the problem.
     *
     * @param _qname  The domain name to ask a question about.
     * @param _qtype  The type of resource records wanted that pertain to the queried domain name.
     * @param _qclass  The class of resource records wanted that pertain to the queried domain name.
     * @return The {@link Outcome Outcome&lt;DNSQuestion&gt;} with the results of the attempt.
     */
    public static Outcome<DNSQuestion> create(  final DNSDomainName _qname, final DNSRRType _qtype, final DNSRRClass _qclass ) {

        if( isNull( _qname, _qtype, _qclass ) )
            return outcome.notOk( "Required parameters not supplied" );

        return outcome.ok( new DNSQuestion( _qname, _qtype, _qclass ) );
    }


    /**
     * Attempts to create a new instance of this class with the given parameters and a resource record class of {@link DNSRRClass#IN} (Internet class
     * records, which are nearly always what is desired).  If successful, an ok outcome containing the new instance is
     * returned.  Otherwise, a not ok outcome is returned with a message explaining the problem.
     *
     * @param _qname  The domain name to ask a question about.
     * @param _qtype  The type of resource records wanted that pertain to the queried domain name.
     * @return The {@link Outcome Outcome&lt;DNSQuestion&gt;} with the results of the attempt.
     */
    public static Outcome<DNSQuestion> create(  final DNSDomainName _qname, final DNSRRType _qtype ) {
        return create( _qname, _qtype, DNSRRClass.IN );
    }


    /**
     * Attempts to create a new instance of this class from the bytes at the current position of the given {@link ByteBuffer}.
     *
     * @param _buffer The {@link ByteBuffer} containing the bytes encoding the question.
     * @return The {@link Outcome Outcome&lt;DNSQuestion&gt;} giving the results of the attempt.
     */
    public static Outcome<DNSQuestion> decode( final ByteBuffer _buffer ) {

        // decode the domain name...
        Outcome<DNSDomainName> domainNameOutcome = DNSDomainName.decode( _buffer );
        if( !domainNameOutcome.ok() )
            return outcome.notOk( "Could not decode qname: " + domainNameOutcome.msg() );

        // decode the resource record type...
        Outcome<DNSRRType> rrTypeOutcome = DNSRRType.decode( _buffer );
        if( !rrTypeOutcome.ok() )
            return outcome.notOk( "Could not decode qtype: " + rrTypeOutcome.msg() );

        // decode the resource record class...
        Outcome<DNSRRClass> rrClassOutcome = DNSRRClass.decode( _buffer );
        if( !rrClassOutcome.ok() )
            return outcome.notOk( "Could not decode qclass: " + rrClassOutcome.msg() );

        // if we get here we decoded everything, so leave with our shiny newly decoded instance...
        return outcome.ok( new DNSQuestion( domainNameOutcome.info(), rrTypeOutcome.info(), rrClassOutcome.info() ) );
    }
}
