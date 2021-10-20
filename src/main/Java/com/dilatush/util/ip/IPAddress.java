package com.dilatush.util.ip;

import com.dilatush.util.Checks;
import com.dilatush.util.Outcome;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.Strings.isEmpty;

abstract public class IPAddress {

    private final static Outcome.Forge<IPAddress> outcomeIP = new Outcome.Forge<>();
    private final static Outcome.Forge<Byte> outcomeByte = new Outcome.Forge<>();

    private final static Pattern IPv4PATTERN = Pattern.compile( "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$" );
    private final static Pattern IPv6SEP_PATTERN = Pattern.compile( "^(.*?)(:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})?$" );

    public abstract byte[] getAddress();

    public abstract InetAddress toInetAddress();

    public static IPAddress fromInetAddress( final InetAddress _address ) {

        Checks.required( _address );

        if( _address instanceof Inet4Address )
            return IPv4Address.fromBytes( _address.getAddress() ).info();   // this is safe, as we know there are 4 bytes of address in the argument...
        else if( _address instanceof Inet6Address )
            return IPv6Address.fromBytes( _address.getAddress() ).info();   // this is safe, as we know there are 16 bytes of address in the argument...

        // this should be impossible...
        else
            throw new IllegalStateException( "InetAddress that is not Inet4Address or Inet6Address" );
    }


    /**
     * Attempts to convert the given string into an instance of {@link IPv4Address} or {@link IPv6Address}, according to the string format.  If the string is a standard IPv4
     * "dotted decimal" format (like "12.66.144.14", then the result is ok with an {@link IPv4Address} instance.  If the given string is a format defined by RFC 5952 (such as
     * "A1:4:12:CCCC:aaaa:56:8d44:0", "ba1::45c:12", or "::ffff:10.2.3.1", then the result is ok with an {@link IPv6Address} instance.  Otherwise, the result is not ok with an
     * explanatory message.
     *
     * @param _address The string to convert into an instance of {@link IPv4Address} or {@link IPv6Address}.
     * @return An instance of {@link Outcome Outcome&lt;IPAddress&gt;} with the result.
     */
    public static Outcome<IPAddress> fromString( final String _address ) {

        // make sure we got a non-empty string...
        if( isEmpty( _address ) )
            return outcomeIP.notOk( "Cannot convert an empty string to an IP address" );

        // see if this looks like an IPv4 dotted-decimal address...
        Matcher mat = IPv4PATTERN.matcher( _address );
        if( mat.matches() ) {

            // get our four bytes of address...
            byte[] address = new byte[4];
            for( int i = 0; i < 4; i++ ) {

                Outcome<Byte> ob = dottedDecimalByte( mat.group( i + 1 ) );
                if( ob.notOk() )
                    return outcomeIP.notOk( "Could not convert \"" + _address + "\" IPv4 address: " + ob.msg() );
                address[i] = ob.info();
            }

            // then create and return the new IPv4Address instance...
            return outcomeIP.ok( IPv4Address.fromBytes( address ).info() );  // this is safe because we've already vetted the bytes...
        }

        // if we get here, we've concluded we don't have an IPv4 address string - so let's see if we have something plausibly an IPv6 address string...
        // first we'll see if it looks like an IPv6 address with an embedded IPv4 address...
        mat = IPv6SEP_PATTERN.matcher( _address );
        if( mat.matches() ) {

            // get our sixteen bytes of address...
            byte[] address = new byte[16];

            // figure out whether we have an embedded IPv4 address...
            boolean embeddedIPv4 = !isEmpty( mat.group( 2 ) );

            // if we have something that might be an embedded IPv4 address, parse it...
            if( embeddedIPv4 ) {
                for( int i = 12; i < 16; i++ ) {

                    Outcome<Byte> ob = dottedDecimalByte( mat.group( i - 11 ) );
                    if( ob.notOk() )
                        return outcomeIP.notOk( "Could not convert \"" + _address + "\" with an embedded IPv4 address: " + ob.msg() );
                    address[i] = ob.info();
                }
            }

            // we might have a double colon, representing 1 or more groups of 16-bit zero groups - with normally encoded groups before, after, or both...
            // if this split has one result, then there was no double colon; if two results, then there was one double colon.  Anything else is an error...
            String[] parts = mat.group( 1 ).split( "::", -1 );

            // if we got three or more results, then our address has multiple double colons and is not convertible...
            if( parts.length >= 3 )
                return outcomeIP.notOk( "Multiple double colons in IPv6 address string: " + _address );



        }
        else
            return outcomeIP.notOk( "Unrecognizable IP address string: " + _address );
        return null;
    }


    private static Outcome<Byte> dottedDecimalByte( final String _byteString ) {
        int b = Integer.parseInt( _byteString );    // this will not throw an exception if called with a matched decimal digit string...
        if( b > 255 )
            return outcomeByte.notOk( "Value out of range for dotted-decimal IP value: " + b );
        return outcomeByte.ok( (byte) b );
    }
}
