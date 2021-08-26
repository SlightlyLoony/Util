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

    /** This domain name as a Java string.  */
    public final String         text;

    /** The length of the bytes representing this domain name. */
    public final int            length;

    /** This domain name as a sequence of labels encoded in bytes, with a byte containing zero as a suffix. */
    public final byte[]         bytes;

    /** This domaint name as a sequence of {@link DNSLabel} instances. */
    public final List<DNSLabel> labels;


    /**
     * Creates a new instance of this class from the given sequence of {@link DNSLabel} instances.  Note that this constructor is private, and is
     * called only from static factory methods; it assumes that the parameter is valid.
     *
     * @param _labels The sequence of {@link DNSLabel} instances to create this domain name from.
     */
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


    /**
     * Attempts to create a new instance of this class from the given sequence of labels.  If the attempt is successful, an
     * {@link Outcome Outcome&lt;DNSDomainName&gt;} that is ok and contains the new instance is returned.  Otherwise, the returned
     * {@link Outcome Outcome&lt;DNSDomainName&gt;} is not ok and contains an explanatory message.
     *
     * @param _labels The labels to create the new {@link DNSDomainName} instance from.
     * @return The {@link Outcome Outcome&lt;DNSDomainName&gt;} containing the result of this attempt.
     */
    public static Outcome<DNSDomainName> fromLabels( final List<DNSLabel> _labels ) {

        // we must have at least one label...
        if( (_labels == null) || (_labels.size() == 0) )
            return new Outcome<>( false, "Labels are missing or empty", null, null );

        // sum the lengths of all our labels...
        int sum = 0;
        for( DNSLabel label : _labels ) {
            sum += label.length;
        }

        // aggregate length of encoded labels, not including the null terminator, must be <= 255...
        if( sum > 255 )
            return new Outcome<>( false, "Domain name byte length may not exceed 255 bytes", null, null );

        // we're good!
        return new Outcome<>( true, null, null, new DNSDomainName( _labels ) );
    }


    /**
     * Attempts to create a new instance of this class from the given array of labels.  If the attempt is successful, an
     * {@link Outcome Outcome&lt;DNSDomainName&gt;} that is ok and contains the new instance is returned.  Otherwise, the returned
     * {@link Outcome Outcome&lt;DNSDomainName&gt;} is not ok and contains an explanatory message.
     *
     * @param _labels The array labels to create the new {@link DNSDomainName} instance from.
     * @return The {@link Outcome Outcome&lt;DNSDomainName&gt;} containing the result of this attempt.
     */
    public static Outcome<DNSDomainName> fromLabels( final DNSLabel[] _labels ) {

        // we must have at least one label...
        if( (_labels == null) || (_labels.length == 0) )
            return new Outcome<>( false, "Labels are missing or empty", null, null );

        return fromLabels( Arrays.asList( _labels ) );
    }


    /**
     * Attempts to create a new instance of this class from the given string, which must be formatted as a classic dot-separated domain name (like
     * "www.cnn.com").
     *
     * @param _text The text domain name to create the new {@link DNSDomainName} instance from.
     * @return The {@link Outcome Outcome&lt;DNSDomainName&gt;} containing the result of this attempt.
     */
    public static Outcome<DNSDomainName> fromString( final String _text ) {

        // get an array of label texts...
        String[] labelTexts = _text.split( "\\." );

        // convert the label texts to DNSLabel instances...
        List<DNSLabel> labels = new ArrayList<>();
        for( String text : labelTexts ) {
            Outcome<DNSLabel> result = DNSLabel.fromString( text );
            if( !result.ok() )
                return new Outcome<>( false, "Couldn't create label: " + result.msg(), null, null );
            labels.add( result.info() );
        }

        return fromLabels( labels );
    }
}
