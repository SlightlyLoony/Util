package com.dilatush.util.ip;

import com.dilatush.util.Checks;
import com.dilatush.util.Outcome;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.Strings.isEmpty;


/**
 * The abstract base class for concrete classes representing IP addresses.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
abstract public class IPAddress implements Comparable<IPAddress> {

    private final static Outcome.Forge<IPAddress> outcomeIP    = new Outcome.Forge<>();
    private final static Outcome.Forge<Byte>      outcomeByte  = new Outcome.Forge<>();
    private final static Outcome.Forge<byte[]>    outcomeBytes = new Outcome.Forge<>();

    // recognizes the general form of a dotted-decimal IPv4 address (like "10.0.3.222"), but does NOT validate the value of each quad...
    private final static Pattern IPv4PATTERN = Pattern.compile(
                    "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$" );

    // recognizes the general form of an IPv6 address, possibly including an embedded IPv4 address; group 1 is the hexadecimal group portion
    // of the IPv6 address (which may be the entire address), and groups 2..5 (if non-null) are the dotted-decimal quads...
    private final static Pattern IPv6SEP_PATTERN = Pattern.compile(
                    "^((?:(?:[0-9a-fA-F]{1,4}:){0,7}[0-9a-fA-F]{1,4})?(?:::(?:(?:[0-9a-fA-F]{1,4}:){0,7}[0-9a-fA-F]{1,4})?)?)" +
                    ":?(?:(?<=:)(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))?$" );

    // recognizes the 0..8 colon-separated hexadecimal groups in an IPv6 address, but NOT the double-colon indicator of a run of zeroes...
    private final static Pattern IPv6HEX_PATTERN = Pattern.compile(
                    "^([0-9a-fA-F]{1,4})?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?" +
                    "(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?(?::([0-9a-fA-F]{1,4}))?$" );


    // the bytes of this address, stored as positive values (for ease of comparison) in a read-only list so that other classes in the same package cannot modify it...
    protected final List<Integer> address;


    /**
     * Create a new instance of this class with an address comprised of the given bytes.  Note that this constructor is protected, and should be called only from a subclass.  The
     * only validation performed is a check for the presence of the argument.
     *
     * @param _address The bytes that comprise the address.
     */
    protected IPAddress( final byte[] _address ) {

        // noinspection all
        Checks.required( (Object) _address );

        // create and fill a temporary list with the positive integer values of the given bytes...
        List<Integer> addr = new ArrayList<>( _address.length );
        for( byte b : _address ) {
            addr.add( b & 0xff );
        }

        // prevent other classes in the package (or subclasses) from meddling with the address value...
        address = Collections.unmodifiableList( addr );
    }


    /**
     * Return the bytes of the IP address, in a byte array.  The bytes are in network order, that is, the byte at index 0 is the most significant byte and is the first byte when
     * serialized for transmission over the network.  The MSB of the first byte is the first bit when serialized for transmission over the network.
     *
     * @return the {@code byte[]} containing the address' bytes.
     */
    public byte[] getAddress() {

        // a place to store the bytes...
        byte[] result = new byte[address.size()];

        // fill in the bytes from our address list of integers...
        for( int i = 0; i < result.length; i++ ) {
            result[i] = address.get( i ).byteValue();
        }

        // and we're done...
        return result;
    }


    /**
     * Returns {@code true} if this address is a private address that is not routable over the public Internet.
     *
     * @return {@code true} if this address is a private address that is not routable over the public Internet.
     */
    public abstract boolean isPrivate();


    /**
     * Returns {@code true} if this address is normally routable over the public Internet.  Addresses that are not private, not link local, not loopback, not for documentation,
     * not broadcast, and are not reserved for any other reason are considered public.  Note that some addresses not considered public by these criteria are actually routable
     * over the public Internet under certain circumstances.  The intent of this method is to return true for addresses that always publicly routable.
     *
     * @return {@code true} if this address is normally routable over the public Internet.
     */
    public boolean isPublic() {
        return !( isPrivate() || isBroadcast() || isLinkLocal() || isLoopback() || isDocumentation() || isReserved() );
    }


    /**
     * Returns {@code true} if this address is a unicast address.  All addresses that are not explicitly multicast are unicast.
     *
     * @return {@code true} if this address is a unicast address.
     */
    public boolean isUnicast() {
        return !isMulticast();
    }


    /**
     * Return {@code true} if this address is a multicast address.  Packets with a multicast address as the destination address may be received by multiple hosts.
     *
     * @return {@code true} if this address is a multicast address.
     */
    public abstract boolean isMulticast();


    /**
     * Return {@code true} if this address is a loopback address.
     *
     * @return {@code true} if this address is a loopback address.
     */
    public abstract boolean isLoopback();


    /**
     * Return {@code true} if this address is a link-local address.
     *
     * @return {@code true} if this address is a link-local address.
     */
    public abstract boolean isLinkLocal();


    /**
     * Return {@code true} if this address has been reserved for documentation.
     *
     * @return {@code true} if this address has been reserved for documentation.
     */
    public abstract boolean isDocumentation();


    /**
     * Return {@code true} if this address is a broadcast address.
     *
     * @return {@code true} if this address is a broadcast address.
     */
    public abstract boolean isBroadcast();


    /**
     * Returns {@code true} if this address is reserved for some purpose not otherwise detected.
     *
     * @return {@code true} if this address is reserved for some purpose not otherwise detected.
     */
    public abstract boolean isReserved();


    /**
     * Returns {@code true} if this address is the wildcard address.
     *
     * @return {@code true} if this address is the wildcard address.
     */
    public abstract boolean isWildcard();


    /**
     * Return a new instance of {@link InetAddress} that contains the IP address in this instance, and no host name.
     *
     * @return A new instance of {@link InetAddress} that contains the IP address in this instance, and no host name.
     */
    public abstract InetAddress toInetAddress();


    /**
     * Returns the number of bits in this address.
     *
     * @return The number of bits in this address.
     */
    public int size() {
        return address.size() << 3;
    }


    /**
     * Compares this {@link IPAddress} with the given {@link IPAddress} for order.  Returns a
     * negative integer, zero, or a positive integer as this address is less
     * than, equal to, or greater than the given address.  If the two addresses being compared
     * have a different number of bytes (i.e., IPv4 vs. IPv6), the one with fewer bytes is
     * considered to be smaller.
     *
     * @param _other the address to be compared with this one.
     * @return a negative integer, zero, or a positive integer as this address
     * is less than, equal to, or greater than the given address.
     */
    @Override
    public int compareTo( final IPAddress _other ) {

        Checks.required( _other );

        // if we are comparing two different address types, the one with fewer bytes is smaller than the other...
        if( address.size() != _other.address.size() )
            return (address.size() < _other.address.size()) ? -1 : 1;

        // they're the same size, so we do a byte-by-byte compare, as positive integers...
        for( int i = 0; i < address.size(); i++ ) {

            // if the bytes at this index are equal, we just try the next one...
            if( a( i ) == _other.a( i ) )
                continue;

            // they're not the same, so we have a result...
            return (a( i ) < _other.a( i )) ? -1 : 1;
        }

        // if we get here, then all the bytes were equal -- and so are the two addresses...
        return 0;
    }


    /**
     * Returns {@code true} if this instance is equal to the given instance.
     *
     * @param _o The object to check for equality.
     * @return {@code true} if this instance is equal to the given instance.
     */
    @Override
    public boolean equals( final Object _o ) {

        if( this == _o ) return true;
        if( !(_o instanceof IPAddress ipAddress) ) return false;
        return address.equals( ipAddress.address );
    }


    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code for this instance.
     */
    @Override
    public int hashCode() {
        return Objects.hash( address );
    }


    /**
     * Convenience method that returns the positive value of the byte at the given index within the address bytes.
     *
     * @param _index The index of the byte to retrieve.
     * @return The positive value of the byte at the given index within the address bytes.
     */
    protected int a( final int _index ) {
        return address.get( _index );
    }


    /**
     * Returns a new {@link IPAddress} instance (either an {@link IPv4Address} or an {@link IPv6Address}) that contains the IP address in the given {@link InetAddress} instance.
     *
     * @param _address The {@link InetAddress} instance to extract an IP address from.
     * @return A new instance of {@link IPAddress} containing the IP address in the given {@link InetAddress} instance.
     */
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
