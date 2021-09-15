package com.dilatush.util.dns;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRClass;
import com.dilatush.util.dns.message.DNSRRType;
import com.dilatush.util.dns.rr.DNSResourceRecord;

import java.util.List;

/**
 * Static container class for functions related to DNS.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSUtil {

    private static final Outcome.Forge<DNSQuestion> outcomeQuestion = new Outcome.Forge<>();


    public static Outcome<DNSQuestion> getQuestion( final String _domainName, final DNSRRType _type, final DNSRRClass _class ) {

        Outcome<DNSDomainName> dno = DNSDomainName.fromString( _domainName );
        if( dno.notOk() )
            return outcomeQuestion.notOk( dno.msg(), dno.cause() );

        DNSQuestion question = new DNSQuestion( dno.info(), _type, _class );
        return outcomeQuestion.ok( new DNSQuestion( dno.info(), _type, _class ) );
    }


    public static Outcome<DNSQuestion> getQuestion( final String _domainName, final DNSRRType _type ) {
        return getQuestion( _domainName, _type, DNSRRClass.IN );
    }


    /**
     * Returns a string representation of the given list of {@link DNSResourceRecord}s, with each resource record on its own line.
     *
     * @param _rrs The list resource records to get a string for.
     * @return the string representation of the list of resource records.
     */
    public static String toString( final List<DNSResourceRecord> _rrs ) {
        StringBuilder sb = new StringBuilder();
        _rrs.forEach( (rr) -> {sb.append( rr ); sb.append( "\n" );} );
        return sb.toString();
    }


    // prevent instantiation...
    private DNSUtil() {}
}
