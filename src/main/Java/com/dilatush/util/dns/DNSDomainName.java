package com.dilatush.util.dns;

import com.dilatush.util.Outcome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Instances of this class represent a DNS domain name, which is a sequence of DNS labels.  Instances of this class are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSDomainName {

    public final String text;
    public final int length;
    public final byte[] bytes;
    public final List<DNSLabel> labels;



    private DNSDomainName( final List<DNSLabel> _labels ) {

        labels = _labels;

        // compute the encoded name's length...
        int len = 0;
        for( DNSLabel label : labels )
            len += label.length;
        length = len + 1;  // the +1 is for the null termination byte...

        // get the byte representation...
        byte[] b = new byte[length + 1];  // the +1 is for the null termination byte...
        int pos = 0;
        for( DNSLabel label : labels ) {
            System.arraycopy( label.bytes, 0, b, pos, label.length );
            pos += label.length;
        }
        bytes = b;

        // get the text representation...
        StringBuilder sb = new StringBuilder( length + labels.size() - 1 );
        for( DNSLabel label : labels ) {
            if( !sb.isEmpty() )
                sb.append( '.' );
            sb.append( label.text );
        }
        text = sb.toString();
    }

    // TODO: needs argument checking...

    public static Outcome<DNSDomainName> fromLabels( final List<DNSLabel> _labels ) {

        // sum the lengths of all our labels...
        int sum = 0;
        for( DNSLabel label : _labels ) {
            sum += label.length;
        }

        if( sum > 255 )
            return new Outcome<>( false, "Domain name byte length may not exceed 255 bytes", null, null );

        return new Outcome<>( true, null, null, new DNSDomainName( _labels ) );
    }


    public static Outcome<DNSDomainName> fromLabels( final DNSLabel[] _labels ) {
        return fromLabels( Arrays.asList( _labels ) );
    }


    public static Outcome<DNSDomainName> fromString( final String _text ) {

        String[] labelTexts = _text.split( "\\." );
        List<DNSLabel> labels = new ArrayList<>();
        for( String text : labelTexts ) {
            Outcome<DNSLabel> result = DNSLabel.fromString( text );
            if( !result.ok() )
                return new Outcome<>( false, "Couldn't create label: " + result.msg(), null, null );
            labels.add( result.info() );
        }
        return fromLabels( labels );
    }


    public static void main( final String[] _args ) {
        Outcome<DNSDomainName> nameOutcome = DNSDomainName.fromString( "www.cnncnncnncnncnncnncnncncnncnncnncnncnncnncnncnncnn.com" );
        DNSDomainName name = nameOutcome.info();
        DNSDomainName.class.hashCode();
    }


}
