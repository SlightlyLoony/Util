package com.dilatush.util.ip;

import com.dilatush.util.Outcome;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import static com.dilatush.util.General.breakpoint;

public class IPv6Address extends IPAddress {

    private static final Outcome.Forge<IPv6Address> outcomeIP = new Outcome.Forge<>();

    private static final byte[] IPv4_COMPATIBLE_IPv6_PREFIX = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           0,           0 };
    private static final byte[] IPv4_MAPPED_IPv6_PREFIX     = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff };

    private final byte[] address;  // the sixteen bytes of IPv6 address, MSB first...


    private IPv6Address( final byte[] _address ) {
        address = _address;
    }


    /**
     * Returns a string representation of this instance, in the canonical form defined in RFC 5952, including the special handling of IPv6 addresses with embedded IPv4 addresses.
     *
     * @return A string representation of this instance.
     */
    public String toString() {

        // do we have an IPv4 address embedded in the 32 LSBs?
        boolean embeddedIPv4 =
                Arrays.equals( address, 0, IPv4_COMPATIBLE_IPv6_PREFIX.length, IPv4_COMPATIBLE_IPv6_PREFIX, 0, IPv4_COMPATIBLE_IPv6_PREFIX.length ) ||
                Arrays.equals( address, 0, IPv4_MAPPED_IPv6_PREFIX.length,     IPv4_MAPPED_IPv6_PREFIX,     0, IPv4_MAPPED_IPv6_PREFIX.length );

        // find the longest run of zero bytes that will be encoded as hexadecimal 16 bit groups...
        int zeroes = 0;          // the number of zero bytes in the current run of zero bytes...
        int runStart = -1;       // the offset of the start of the current run of zero bytes (always an even index, start of a 16 bit group)...
        int bestRunZeroes = 0;   // the longest run of zero bytes (always an even number of bytes, so an even number of 16 bit groups)...
        int bestRunStart = -1;   // the offset of the
        for( int i = 0; i < (embeddedIPv4 ? 12 : 16); i++ ) {

            // if this byte is a zero...
            if( address[i] == 0 ) {

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
                String hex = Integer.toHexString( ((address[i] & 0xff) << 8) | (address[i+1] & 0xff) );
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
                sb.append( 0xff & address[i] );
            }
        }

        // finally, we're done...
        return sb.toString();
    }


    @Override
    public byte[] getAddress() {
        return address;
    }


    static public Outcome<IPv6Address> fromBytes( final byte[] _bytes ) {

        if( _bytes == null )
            return outcomeIP.notOk( "No bytes argument supplied" );
        else if( _bytes.length != 16 )
            return outcomeIP.notOk( "Should be 16 bytes for IPv6 address, was " + _bytes.length );
        return outcomeIP.ok( new IPv6Address( _bytes ) );
    }


    @Override
    public InetAddress toInetAddress() {

        try {
            return InetAddress.getByAddress( address );
        }

        // it should be impossible to get here, as the address is guaranteed to be a valid 16 bytes long...
        catch( UnknownHostException _e ) {
            throw new IllegalStateException( "IPv6 address that is not 16 bytes long" );
        }
    }


    public static void main( final String[] _args ) {

        String test = "gss::dfd::lkd";

        String[] parts = test.split( "::", -1 );

        breakpoint();
    }
}
