package com.dilatush.util.ip;

import com.dilatush.util.Checks;

public class IPv4Address implements IPAddress {

    private byte[] address;  // the four bytes of IPv4 address, MSB first...


    private IPv4Address( final byte[] _address ) {

        Checks.required( (Object) _address );
        Checks.isTrue( _address.length == 4, "IPv4 must be four bytes long" );

        address = _address;
    }
}
