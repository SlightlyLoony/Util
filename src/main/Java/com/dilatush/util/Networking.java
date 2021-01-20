package com.dilatush.util;

import java.net.*;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for methods related to networking.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Networking {


    /**
     * Returns a string representing the given {@link InetAddress}.  This replaces the {@link InetAddress#toString()} method, which returns
     * {@code hostname/IP address} if the host name is resolved, or {@code /IP address} if it is not resolved.  This method returns only the IP
     * address.
     *
     * @param _address The {@link InetAddress} to get a string for.
     * @return the string representing the given {@link InetAddress}
     */
    public static String toString( final InetAddress _address ) {

        // if we have no argument, return an empty string...
        if( isNull( _address ) )
            return "";

        // get the string produced by InetAddress, split on the "/"...
        String[] parts = _address.toString().split( "/" );

        // if we don't have two parts, just return what InetAddress gives us, because we don't know what's going on...
        if( parts.length != 2 )
            return _address.toString();

        // the second part is what we want; just return it...
        return parts[1];
    }


    /**
     * Returns a string representing the given {@link SocketAddress}.  {@link SocketAddress} is an abstract class, and for concrete subclasses
     * <i>other</i> than {@link InetSocketAddress}, this method just returns the result of {@link SocketAddress#toString()}.  However, for the
     * concrete subclass {@link InetSocketAddress} this replaces the {@link SocketAddress#toString()} method, which returns
     * {@code hostname/IP address:port} if the host name is resolved, or {@code /IP address:port} if it is not resolved.  This method always returns
     * {@code IP address,port}, which is unambiguous with IPv6 addresses (which may include colons).
     *
     * @param _socketAddress The {@link SocketAddress} to get a string for.
     * @return the string representing the given {@link SocketAddress}
     */
    public static String toString( final SocketAddress _socketAddress ) {

        // if we got a null, return an empty string...
        if( isNull( _socketAddress ) )
            return "";

        // if we've got an InetSocketAddress, we've got special things to do...
        if( _socketAddress instanceof InetSocketAddress ) {

            // make the string we really want...
            InetSocketAddress isa = (InetSocketAddress) _socketAddress;
            return toString( isa.getAddress() ) + "," + isa.getPort();
        }

        // otherwise, just return the toString() results...
        return _socketAddress.toString();
    }
}
