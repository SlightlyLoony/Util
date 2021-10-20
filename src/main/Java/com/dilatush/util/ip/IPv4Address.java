package com.dilatush.util.ip;

import com.dilatush.util.Outcome;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPv4Address extends IPAddress {

    private static final Outcome.Forge<IPv4Address> outcomeIP = new Outcome.Forge<>();

    private final byte[] address;  // the four bytes of IPv4 address, MSB first...


    private IPv4Address( final byte[] _address ) {
        address = _address;
    }


    static public Outcome<IPv4Address> fromBytes( final byte[] _bytes ) {

        if( _bytes == null )
            return outcomeIP.notOk( "No bytes argument supplied" );
        else if( _bytes.length != 4 )
            return outcomeIP.notOk( "Should be 4 bytes for IPv4 address, was " + _bytes.length );
        return outcomeIP.ok( new IPv4Address( _bytes ) );
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
        for( byte b : address ) {

            // if this isn't the first byte, then we need a dot separator...
            if( sb.length() != 0 )
                sb.append( "." );

            // mask the byte so that it doesn't turn into a negative number when cast to an int...
            sb.append( b & 0xff );
        }

        // and we're done...
        return sb.toString();
    }


    @Override
    public byte[] getAddress() {
        return address;
    }


    @Override
    public InetAddress toInetAddress() {

        try {
            return InetAddress.getByAddress( address );
        }

        // it should be impossible to get here, as the address is guaranteed to be a valid 4 bytes long...
        catch( UnknownHostException _e ) {
            throw new IllegalStateException( "IPv4 address that is not 4 bytes long" );
        }
    }
}
