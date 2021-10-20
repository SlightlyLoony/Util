package com.dilatush.util.ip;

import com.dilatush.util.Checks;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public interface IPAddress {

    byte[] getAddress();

    String toString();

    InetAddress toInetAddress();

    static IPAddress fromInetAddress( final InetAddress _address ) {

        Checks.required( _address );

        if( _address instanceof Inet4Address ) {

        }

        else if( _address instanceof Inet6Address ) {

        }

        // this should be impossible...
        else
            throw new IllegalStateException( "InetAddress that is not Inet4Address or Inet6Address" );
    }
}
