package com.dilatush.util.ip;

import com.dilatush.util.Checks;

import java.util.Objects;

/**
 * Instances of this class represent an IP CIDR network, comprised of the first IP address in the network and the number of bits of network prefix.  Methods are provided to
 * test whether an IP address is contained in the network, to return the first and last IP address in the network, etc.  Instances of this class are immutable and threadsafe.
 */
@SuppressWarnings( "unused" )
final public class IPNetwork {

    private static final int[] BIT_MASK = new int[] { 0, 0x80, 0xc0, 0xe0, 0xf0, 0xf8, 0xfc, 0xfe, 0xff };
    private static final int[] OR_MASK  = new int[] { 0, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff };

    /** The base address for this IP network. */
    public final IPAddress baseAddress;

    /** The number of CIDR network prefix bits for this IP network */
    public final int prefixBits;


    /**
     * Create a new instance of {@link IPNetwork} with the given base {@link IPAddress} and number of prefix bits.  The number of prefix bits must be in the range [0..n], where
     * 'n' is the number of bits in the base IP address.
     *
     * @param _baseAddress The base {@link IPAddress} for the new {@link IPNetwork}.
     * @param _prefixBits The number of CIDR network prefix bits.
     */
    public IPNetwork( final IPAddress _baseAddress, final int _prefixBits ) {

        Checks.required( _baseAddress );
        if( (_prefixBits < 0) || (_prefixBits > _baseAddress.size()) )
            throw new IllegalArgumentException( "Number of prefix bits must be in [0.." + _baseAddress.size() + "], was: " + _prefixBits );

        baseAddress = _baseAddress;
        prefixBits  = _prefixBits;
    }


    /**
     * Returns {@code true} if this IP network contains the given {@link IPAddress}.  The IP address is considered contained if it is the same type of IP address as the network's
     * base address (e.g., {@link IPv4Address} or {@link IPv6Address}), and the IP address's network prefix matches the base address.
     *
     * @param _address The {@link IPAddress} to test.
     * @return {@code true} if this IP network contains the given {@link IPAddress}.
     */
    public boolean contains( final IPAddress _address ) {

        Checks.required( _address );

        // if the IP address is a different type than the base address, it is by definition not contained...
        if( baseAddress.getClass() != _address.getClass() )
            return false;

        // if the prefixes match, then the address is contained...
        int bitsRemaining = prefixBits;
        int i = 0;
        while( bitsRemaining > 0 ) {

            // get the mask we're going to use to compare this byte...
            int mask = BIT_MASK[ Math.min(8, bitsRemaining) ];

            // if the masked bytes are different, the address is not contained...
            if( (mask & baseAddress.a(i)) != (mask & _address.a(i) ))
                return false;

            // update our state...
            bitsRemaining -= Math.min(8, bitsRemaining);
            i++;
        }

        // if we make it here, then the entire prefix is equal, and the address is contained...
        return true;
    }


    /**
     * Returns the first {@link IPAddress} that is contained in this network.
     *
     * @return The first {@link IPAddress} that is contained in this network.
     */
    public IPAddress first() {
        return baseAddress;
    }


    /**
     * Returns the last {@link IPAddress} that is contained in this network.
     *
     * @return The last {@link IPAddress} that is contained in this network.
     */
    public IPAddress last() {

        // start with the network prefix...
        byte[] addr = baseAddress.getAddress();

        // set all the suffix bits to '1'...
        int bitsRemaining = baseAddress.size() - prefixBits;   // the number of bits that must be set to '1'...
        int i = addr.length - 1;                               // we start with the least significant byte...
        while( bitsRemaining > 0 ) {

            // set the bits we need to...
            int or = OR_MASK[ Math.min(8, bitsRemaining) ];
            addr[i] |= or;

            // update our state...
            bitsRemaining -= Math.min(8, bitsRemaining);
            i--;
        }

        // return with our answer...
        return (addr.length == 4) ? IPv4Address.fromBytes( addr ).info() : IPv6Address.fromBytes( addr ).info();
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
        if( _o == null || getClass() != _o.getClass() ) return false;
        IPNetwork ipNetwork = (IPNetwork) _o;
        return prefixBits == ipNetwork.prefixBits && baseAddress.equals( ipNetwork.baseAddress );
    }


    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code for this instance.
     */
    @Override
    public int hashCode() {

        return Objects.hash( baseAddress, prefixBits );
    }
}
