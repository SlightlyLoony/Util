package com.dilatush.util.ip;

import com.dilatush.util.Checks;
import com.dilatush.util.Outcome;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Instances of this class represent IP version 6 addresses.  Instances of this class are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
final public class IPv6Address extends IPAddress {

    /** The wildcard IPv6 address "0:0:0:0:0:0:0:0" (or "::"). */
    public static final IPv6Address WILDCARD    = new IPv6Address( new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} );

    /** The unspecified IPv6 address "0:0:0:0:0:0:0:0" (or "::"). */
    public static final IPv6Address UNSPECIFIED = WILDCARD;

    /** The loopback IPv6 address "::1". */
    public static final IPv6Address LOOPBACK    = new IPv6Address( new byte[] {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1} );

    private static final Outcome.Forge<IPv6Address> outcomeIP = new Outcome.Forge<>();

    private static final byte[] IPv4_COMPATIBLE_IPv6_PREFIX = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           0,           0 };
    private static final byte[] IPv4_MAPPED_IPv6_PREFIX     = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff };


    /**
     * Creates a new instance of this class with the given address, which must be 128 bits (16 bytes) long.  Note that this constructor is private, and that the argument is not
     * validated.
     *
     * @param _address The 128 bit (16 byte) address.
     */
    private IPv6Address( final byte[] _address ) {
        super( _address );
    }


    /**
     * Returns {@code true} if this address is a private address that is not routable over the public Internet.
     *
     * @return {@code true} if this address is a private address that is not routable over the public Internet.
     */
    @Override
    public boolean isPrivate() {
        return PRIVATE.contains( this );
    }
    private static final IPNetwork PRIVATE = new IPNetwork( IPv6Address.fromString( "fc00::" ).info(), 7 );


    /**
     * Return {@code true} if this address is a multicast address.  Packets with a multicast address as the destination address may be received by multiple hosts.
     *
     * @return {@code true} if this address is a multicast address.
     */
    @Override
    public boolean isMulticast() {
        return MULTICAST.contains( this );
    }
    private static final IPNetwork MULTICAST = new IPNetwork( IPv6Address.fromString( "ff00::" ).info(), 8 );


    /**
     * Return {@code true} if this address is a loopback address.
     *
     * @return {@code true} if this address is a loopback address.
     */
    @Override
    public boolean isLoopback() {
        return equals( LOOPBACK );
    }


    /**
     * Return {@code true} if this address is a link-local address.
     *
     * @return {@code true} if this address is a link-local address.
     */
    @Override
    public boolean isLinkLocal() {

        return LINK_LOCAL.contains( this );
    }
    private static final IPNetwork LINK_LOCAL = new IPNetwork( IPv6Address.fromString( "fe80::" ).info(), 10 );


    /**
     * Return {@code true} if this address has been reserved for documentation.
     *
     * @return {@code true} if this address has been reserved for documentation.
     */
    @Override
    public boolean isDocumentation() {
        return DOCUMENTATION.contains( this );
    }
    private static final IPNetwork DOCUMENTATION = new IPNetwork( IPv6Address.fromString( "2001:db8::" ).info(), 32 );


    /**
     * Return {@code true} if this address is a broadcast address.
     *
     * @return {@code true} if this address is a broadcast address.
     */
    @Override
    public boolean isBroadcast() {
        return false;
    }


    /**
     * Returns {@code true} if this address is reserved for some purpose not otherwise detected.
     *
     * @return {@code true} if this address is reserved for some purpose not otherwise detected.
     */
    @Override
    public boolean isReserved() {

        return     IPv4_MAPPED.contains( this )
                || IPv4_IPv6.contains( this )
                || DISCARD_ONLY.contains( this )
                || BENCHMARKING.contains( this )
                || RESERVED1.contains( this );
    }
    private static final IPNetwork IPv4_MAPPED  = new IPNetwork( IPv6Address.fromString( "::ffff:0:0"  ).info(), 96 );
    private static final IPNetwork IPv4_IPv6    = new IPNetwork( IPv6Address.fromString( "64:ff9b:1::" ).info(), 48 );
    private static final IPNetwork DISCARD_ONLY = new IPNetwork( IPv6Address.fromString( "::ffff:0:0"  ).info(), 96 );
    private static final IPNetwork BENCHMARKING = new IPNetwork( IPv6Address.fromString( "2001:2::"    ).info(), 48 );
    private static final IPNetwork RESERVED1    = new IPNetwork( IPv6Address.fromString( "2001::"      ).info(), 23 );



    /**
     * Returns a string representation of this instance, in the canonical form defined in RFC 5952, including the special handling of IPv6 addresses with embedded IPv4 addresses.
     *
     * @return A string representation of this instance.
     */
    public String toString() {

        // do we have an IPv4 address embedded in the 32 LSBs?
        boolean embeddedIPv4 = IPv4_MAPPED.contains( this ) || IPv4_IPv6.contains( this );

        // find the longest run of zero bytes that will be encoded as hexadecimal 16 bit groups...
        int zeroes = 0;          // the number of zero bytes in the current run of zero bytes...
        int runStart = -1;       // the offset of the start of the current run of zero bytes (always an even index, start of a 16 bit group)...
        int bestRunZeroes = 0;   // the longest run of zero bytes (always an even number of bytes, so an even number of 16 bit groups)...
        int bestRunStart = -1;   // the offset of the
        for( int i = 0; i < (embeddedIPv4 ? 12 : 16); i++ ) {

            // if this byte is a zero...
            if( a(i) == 0 ) {

                // if we're already in a run of zeroes...
                if( runStart >= 0 ) {

                    // just bump our count...
                    zeroes++;
                }

                // if we're not already in a run, and this is an even index (start of a 16 bit group), then start one...
                else if( (i & 1) == 0 ) {

                    runStart = i;
                    zeroes++;
                }
            }

            // if this byte is NOT a zero...
            else {

                // if it's terminating a run of zeroes...
                if( runStart >= 0 ) {

                    // make sure we have an even number of zeroes, by forcing the LSB to zero...
                    zeroes &= ~1;

                    // if this is the biggest run, and it's at least two groups long, save it...
                    if( (zeroes >= 4) && (zeroes > bestRunZeroes) ) {
                        bestRunStart = runStart;
                        bestRunZeroes = zeroes;
                    }

                    // reset for the next run of zeroes...
                    zeroes = 0;
                    runStart = -1;
                }
            }
        }

        // if we ended the loop above still in a run of zeroes, handle it...
        if( runStart >= 0 ) {

            // make sure we have an even number of zeroes, by forcing the LSB to zero...
            zeroes &= ~1;

            // if this is the biggest run, and it's at least two groups long, save it...
            if( (zeroes >= 4) && (zeroes > bestRunZeroes) ) {
                bestRunStart = runStart;
                bestRunZeroes = zeroes;
            }
        }

        // when we get here, if we've identified a group of zeroes to be represented by "::", then bestRunStart is >= 0...

        // run through the address, emitting groups of two bytes in hex, separated by colons, and a double colon for the longest run of zeroes, if we've identified one...
        StringBuilder sb = new StringBuilder( 80 );  // allow for the longest possible result string...
        for( int i = 0; i < (embeddedIPv4 ? 12 : 16); i += 2 ) {

            // if we're at the start of our longest zero run, emit the first of the double colon...
            if( i == bestRunStart ) {
                sb.append( ':' );
                i += (bestRunZeroes - 2);  // bump the index to get past the zeroes...
            }

            // otherwise, emit a hex group...
            else {

                // emit a colon if we need to...
                if( sb.length() > 0 )
                    sb.append( ':' );

                // emit our 16 bit hex group...
                String hex = Integer.toHexString( (a(i) << 8) | a(i+1) );
                sb.append( hex );
            }
        }

        // if we've got an embedded IPv4 address, emit it in dotted decimal format...
        if( embeddedIPv4 ) {

            // the embedded IPv4 address is always in the last four bytes...
            for( int i = 12; i < 16; i++ ) {

                // emit the appropriate separator...
                sb.append( (i > 12) ? '.' : ':' );

                // emit the decimal byte...
                sb.append( a(i) );
            }
        }

        // finally, we're done...
        return sb.toString();
    }


    /**
     * Attempts to create a new instance of {@link IPv6Address} from the given array of bytes, which must have a length of 16.  If successful, an ok with the new instance is
     * returned.  Otherwise, a not ok with an explanatory message is returned.
     *
     * @param _bytes The sixteen bytes of address to create a new {@link IPv6Address} instance with.
     * @return An {@link Outcome Outcome&lt;IPv6Address&gt;} instance with the result.
     */
    public static Outcome<IPv6Address> fromBytes( final byte[] _bytes ) {

        // noinspection all
        Checks.required( (Object) _bytes );

        if( _bytes.length != 16 )
            return outcomeIP.notOk( "Should be 16 bytes for IPv6 address, was " + _bytes.length );
        return outcomeIP.ok( new IPv6Address( Arrays.copyOf( _bytes, _bytes.length ) ) );
    }


    /**
     * Return a new instance of {@link Inet6Address} that contains the IP address in this instance, and no host name.
     *
     * @return A new instance of {@link Inet6Address} that contains the IP address in this instance, and no host name.
     */
    @Override
    public InetAddress toInetAddress() {

        try {
            InetAddress ia = InetAddress.getByAddress( getAddress() );
            if( !(ia instanceof Inet6Address) )
                throw new IllegalStateException( "Converting IP6Address to Inet6Address resulted in " + ia.getClass().getSimpleName() );
            return ia;
        }

        // it should be impossible to get here, as the address is guaranteed to be a valid 16 bytes long...
        catch( UnknownHostException _e ) {
            throw new IllegalStateException( "IPv6 address that is not 16 bytes long" );
        }
    }
}
