package com.dilatush.util.ip;

import com.dilatush.util.Checks;
import com.dilatush.util.Outcome;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Instances of this class represent IP version 4 addresses.  Instances of this class are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
final public class IPv4Address extends IPAddress {

    /** The wildcard IPv4 address: "0.0.0.0". */
    public static final IPv4Address WILDCARD    = new IPv4Address( new byte[] {0,0,0,0} );

    /** The unspecified IPv4 address: "0.0.0.0" */
    public static final IPv4Address UNSPECIFIED = WILDCARD;

    /** The broadcast IPv4 address: "255.255.255.255". */
    public static final IPv4Address BROADCAST   = new IPv4Address( new byte[] {-1,-1,-1,-1} );

    /** The loopback IPv4 address: "127.0.0.1". */
    public static final IPv4Address LOOPBACK    = new IPv4Address( new byte[] {127,0,0,1} );

    private static final Outcome.Forge<IPv4Address> outcomeIP = new Outcome.Forge<>();


    /**
     * Creates a new instance of this class with the given 4 address bytes.  Note that this constructor is private, and that the argument is not validated.
     *
     * @param _address The 4 address bytes for this instance.
     */
    private IPv4Address( final byte[] _address ) {
        super( _address );
    }


    /**
     * Returns {@code true} if this address is a private address that is not routable over the public Internet.
     *
     * @return {@code true} if this address is a private address that is not routable over the public Internet.
     */
    @Override
    public boolean isPrivate() {
        return PRIVATE1.contains( this ) || PRIVATE2.contains( this ) || PRIVATE3.contains( this );
    }
    private static final IPNetwork PRIVATE1  = new IPNetwork( IPv4Address.fromString( "10.0.0.0"    ).info(),  8 );
    private static final IPNetwork PRIVATE2  = new IPNetwork( IPv4Address.fromString( "172.16.0.0"  ).info(), 12 );
    private static final IPNetwork PRIVATE3  = new IPNetwork( IPv4Address.fromString( "192.168.0.0" ).info(), 16 );


    /**
     * Return {@code true} if this address is a multicast address.  Packets with a multicast address as the destination address may be received by multiple hosts.
     *
     * @return {@code true} if this address is a multicast address.
     */
    @Override
    public boolean isMulticast() {
        return MULTICAST.contains( this );
    }
    private static final IPNetwork MULTICAST  = new IPNetwork( IPv4Address.fromString( "224.0.0.0" ).info(),  4 );


    /**
     * Return {@code true} if this address is a loopback address.
     *
     * @return {@code true} if this address is a loopback address.
     */
    @Override
    public boolean isLoopback() {
        return LOOPBACK_NETWORK.contains( this );
    }
    private static final IPNetwork LOOPBACK_NETWORK  = new IPNetwork( IPv4Address.fromString( "127.0.0.0" ).info(),  8 );


    /**
     * Return {@code true} if this address is a link-local address.
     *
     * @return {@code true} if this address is a link-local address.
     */
    @Override
    public boolean isLinkLocal() {
        return LINK_LOCAL.contains( this );
    }
    private static final IPNetwork LINK_LOCAL  = new IPNetwork( IPv4Address.fromString( "169.254.0.0" ).info(),  16 );


    /**
     * Returns {@code true} if this address is the wildcard address.
     *
     * @return {@code true} if this address is the wildcard address.
     */
    @Override
    public boolean isWildcard() {
        return equals( WILDCARD );
    }

    /**
     * Return {@code true} if this address has been reserved for documentation.
     *
     * @return {@code true} if this address has been reserved for documentation.
     */
    @Override
    public boolean isDocumentation() {
        return  DOCUMENTATION1.contains( this ) || DOCUMENTATION2.contains( this ) || DOCUMENTATION3.contains( this ) || DOCUMENTATION4.contains( this );
    }
    private static final IPNetwork DOCUMENTATION1  = new IPNetwork( IPv4Address.fromString( "192.0.2.0"    ).info(),  24 );
    private static final IPNetwork DOCUMENTATION2  = new IPNetwork( IPv4Address.fromString( "198.51.100.0" ).info(),  24 );
    private static final IPNetwork DOCUMENTATION3  = new IPNetwork( IPv4Address.fromString( "203.0.113.0"  ).info(),  24 );
    private static final IPNetwork DOCUMENTATION4  = new IPNetwork( IPv4Address.fromString( "233.252.0.0"  ).info(),  24 );


    /**
     * Return {@code true} if this address is a broadcast address.
     *
     * @return {@code true} if this address is a broadcast address.
     */
    @Override
    public boolean isBroadcast() {
        return equals( BROADCAST );  // 255.255.255.255
    }


    /**
     * Returns {@code true} if this address is reserved for some purpose not otherwise detected.
     *
     * @return {@code true} if this address is reserved for some purpose not otherwise detected.
     */
    @Override
    public boolean isReserved() {

        return CGNAT.contains( this ) || OLD_E.contains( this ) || RESERVED1.contains( this ) || PRIVATE_MULTICAST.contains( this ) || BENCHMARKING.contains( this );
    }
    private static final IPNetwork CGNAT             = new IPNetwork( IPv4Address.fromString( "100.64.0.0"  ).info(),  10 );
    private static final IPNetwork OLD_E             = new IPNetwork( IPv4Address.fromString( "240.0.0.0"   ).info(),   4 );
    private static final IPNetwork RESERVED1         = new IPNetwork( IPv4Address.fromString( "192.88.99.0" ).info(),  24 );
    private static final IPNetwork PRIVATE_MULTICAST = new IPNetwork( IPv4Address.fromString( "224.0.0.0"   ).info(),  24 );
    private static final IPNetwork BENCHMARKING      = new IPNetwork( IPv4Address.fromString( "198.18.0.0"  ).info(),  15 );


    /**
     * Attempts to create a new instance of {@link IPv4Address} from the given array of bytes, which must have a length of 4.  If successful, an ok with the new instance is
     * returned.  Otherwise, a not ok with an explanatory message is returned.
     *
     * @param _bytes The four bytes of address to create a new {@link IPv4Address} instance with.
     * @return An {@link Outcome Outcome&lt;IPv4Address&gt;} instance with the result.
     */
    static public Outcome<IPv4Address> fromBytes( final byte[] _bytes ) {

        // noinspection all
        Checks.required( (Object) _bytes );

        if( _bytes.length != 4 )
            return outcomeIP.notOk( "Should be 4 bytes for IPv4 address, was " + _bytes.length );
        return outcomeIP.ok( new IPv4Address( Arrays.copyOf( _bytes, _bytes.length ) ) );
    }


    /**
     * Returns a string representation of this instance, in standard "dotted decimal" format: four numbers in [0..255], separated by three "dots" (periods).  For example, the
     * IPv4 address represented by the four hex bytes 1a, 20, ff, 1 would return the string "26.32.255.1".
     *
     * @return A string representation of this instance, in standard "dotted decimal" format.
     */
    public String toString() {

        // allow for the longest possible result...
        StringBuilder sb = new StringBuilder( 15 );

        // each byte is one of the decimal numbers...
        for( int b : address ) {

            // if this isn't the first byte, then we need a dot separator...
            if( sb.length() != 0 )
                sb.append( "." );

            // the quad's value...
            sb.append( b );
        }

        // and we're done...
        return sb.toString();
    }


    /**
     * Return a new instance of {@link Inet4Address} that contains the IP address in this instance, and no host name.
     *
     * @return A new instance of {@link Inet4Address} that contains the IP address in this instance, and no host name.
     */
    @Override
    public InetAddress toInetAddress() {

        try {
            InetAddress ia = InetAddress.getByAddress( getAddress() );
            if( !(ia instanceof Inet4Address) )
                throw new IllegalStateException( "Converting IP4Address to Inet4Address resulted in " + ia.getClass().getSimpleName() );
            return ia;
        }

        // it should be impossible to get here, as the address is guaranteed to be a valid 4 bytes long...
        catch( UnknownHostException _e ) {
            throw new IllegalStateException( "IPv4 address that is not 4 bytes long" );
        }
    }
}
