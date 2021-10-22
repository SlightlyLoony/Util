package com.dilatush.util.ip;

import com.dilatush.util.Checks;
import com.dilatush.util.Outcome;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.Strings.isEmpty;

@SuppressWarnings( "unused" )
abstract public class IPAddress {

    private final static Outcome.Forge<IPAddress> outcomeIP    = new Outcome.Forge<>();
    private final static Outcome.Forge<Byte>      outcomeByte  = new Outcome.Forge<>();
    private final static Outcome.Forge<byte[]>    outcomeBytes = new Outcome.Forge<>();

    private final static Pattern IPv4PATTERN = Pattern.compile(
                    "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$" );
    private final static Pattern IPv6SEP_PATTERN = Pattern.compile(
                    "^((?:(?:[0-9a-fA-F]{1,4}:){0,7}[0-9a-fA-F]{1,4})?(?:::(?:(?:[0-9a-fA-F]{1,4}:){0,7}[0-9a-fA-F]{1,4})?)?)" +
                    ":?(?:(?<=:)(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))?$" );
    private final static Pattern IPv6HEX_PATTERN = Pattern.compile(
                    "^([0-9a-fA-F]{1,4})?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?" +
                    "(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?$" );

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
        if( !mat.matches() )
            return outcomeIP.notOk( "Unrecognizable IP address string: " + _address );

        // if we get here, then it looks like we've got an IPv6 address string...

        // create a place for our sixteen bytes of address...
        byte[] address = new byte[16];

        // figure out whether we have an embedded IPv4 address...
        boolean embeddedIPv4 = !isEmpty( mat.group( 2 ) );

        // if we have something that might be an embedded IPv4 address, parse it...
        if( embeddedIPv4 ) {
            for( int i = 12; i < 16; i++ ) {

                Outcome<Byte> ob = dottedDecimalByte( mat.group( i - 10 ) );
                if( ob.notOk() )
                    return outcomeIP.notOk( "Could not convert \"" + _address + "\" with an embedded IPv4 address: " + ob.msg() );
                address[i] = ob.info();
            }
        }

        // get the hex groups part...
        String hexGroups = mat.group( 1 );

        // if the hex groups part has three (or more) sequential colons, we have a problem...
        if( hexGroups.contains( ":::" ) )
            return outcomeIP.notOk( "Hex groups in IPv6 address have three or more sequential colons: " + hexGroups );

        // we might have a double colon, representing 1 or more groups of 16-bit zero groups - with normally encoded groups before, after, or both...
        // if this split has one result, then there was no double colon; if two results, then there was one double colon.  Anything else is an error...
        String[] parts = mat.group( 1 ).split( "::", -1 );

        // what we do next depends on how many parts we got after the split, which will always be either one or two...
        if( parts.length == 1 ) {

            // with a single result (meaning no double colon), then we need an exact fit; get our bytes...
            Outcome<byte[]> converted = hexBytes( parts[0] );
            if( converted.notOk() )
                return outcomeIP.notOk( converted.msg() );
            byte[] bytes = converted.info();

            // make sure we have the right number of them...
            if( embeddedIPv4 ? (bytes.length != 12) : (bytes.length != 16) )
                return outcomeIP.notOk( "Wrong number of bytes in hex groups; should be " + (embeddedIPv4 ? 12 : 16) + ", was " + bytes.length );

            // copy them into our result, and we're done...
            System.arraycopy( bytes, 0, address, 0, bytes.length );
            return outcomeIP.ok( IPv6Address.fromBytes( address ).info() );
        }

        // it must be two parts, so we had a double colon...
        else {

            // with two results, we should have a run of at least one zero group between the two results; get our prefix and suffix bytes...
            Outcome<byte[]> prefix = hexBytes( parts[0] );
            if( prefix.notOk() )
                return outcomeIP.notOk( prefix.msg() );
            byte[] prefixBytes = prefix.info();
            Outcome<byte[]> suffix = hexBytes( parts[1] );
            if( suffix.notOk() )
                return outcomeIP.notOk( suffix.msg() );
            byte[] suffixBytes = suffix.info();

            // make sure we don't have too many bytes encoded...
            int numBytes = prefixBytes.length + suffixBytes.length + (embeddedIPv4 ? 4 : 0);
            if( numBytes > 14 )
                return outcomeIP.notOk( "Too many bytes encoded in: " + _address );

            // we're good, so copy the bytes to their appropriate destination, and we're done...
            System.arraycopy( prefixBytes, 0, address, 0, prefixBytes.length );
            System.arraycopy( suffixBytes, 0, address, (embeddedIPv4 ? 12 : 16) - suffixBytes.length, suffixBytes.length );
            return outcomeIP.ok( IPv6Address.fromBytes( address ).info() );
        }
    }


    /**
     * Parses the given hex group string into 16-bit pairs of bytes, from colon-separated groups of 1 to 4 hex digits.  There may not be any leading or trailing colons.  Returns
     * ok with the resulting bytes if there are no parsing issues.  Otherwise, returns a not ok with an explanatory message.  An empty string will produce an ok result with an
     * empty byte array result.
     *
     * @param _hexGroupString String with one or more colon separated groups of 1 to 4 hex digits.
     * @return The {@link Outcome Outcome&lt;byte[]&gt;} result.
     */
    private static Outcome<byte[]> hexBytes( final String _hexGroupString ) {

        // handle the case of an empty string...
        if( isEmpty( _hexGroupString ) )
            return outcomeBytes.ok( new byte[0] );

        // handle the case of leading or trailing colons...
        if( _hexGroupString.startsWith( ":" ) )
            return outcomeBytes.notOk( "Hex groups start with colon: " + _hexGroupString );
        if( _hexGroupString.endsWith( ":" ) )
            return outcomeBytes.notOk( "Hex groups end with colon: " + _hexGroupString );

        // parse however many groups we have with a regex...
        Matcher mat = IPv6HEX_PATTERN.matcher( _hexGroupString );

        // handle the (should be impossible) case of an unparseable input...
        if( !mat.matches() )
            return outcomeBytes.notOk( "Can't parse hex groups: " + _hexGroupString );

        // figure out how many groups we have...
        int g;
        for( g = 1; g <= 8; g++ ) {
            if( mat.group( g ) == null )
                break;
        }
        // when we get here, g is one greater than the index of the last group (note that the first index is 1)...

        // create our return value and fill it...
        byte[] result = new byte[ 2 * (g - 1) ];  // two bytes for each group...
        for( int i = 0; i < (g - 1) * 2; i += 2 ) {

            // convert the group from hex to an integer...
            int group = Integer.parseInt( mat.group( 1 + (i / 2) ), 16 );   // should be impossible to throw a NumberFormatException, as the regex vets the digits...

            // stuff the bytes away...
            result[i]   = (byte)(group >> 8);
            result[i+1] = (byte) group;
        }

        return outcomeBytes.ok( result );
    }


    /**
     * Converts the given string (which is presumed to be between 1 and 3 decimal digits) to a byte.  If the digits in the string represent a value in the range [0..255], then
     * an ok with the byte is returned.  Otherwise, a not ok is returned with an explanatory message.
     *
     * @param _byteString The string of 1 to 3 text digits.
     * @return The {@link Outcome Outcome&lt;Byte&gt;} result.
     */
    private static Outcome<Byte> dottedDecimalByte( final String _byteString ) {
        int b = Integer.parseInt( _byteString );    // this will not throw an exception if called with a matched decimal digit string...
        if( b > 255 )
            return outcomeByte.notOk( "Value out of range for dotted-decimal IP value: " + b );
        return outcomeByte.ok( (byte) b );
    }
}
