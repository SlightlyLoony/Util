package com.dilatush.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Static container class for functions related to the Internet.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class Internet {


    /**
     * Returns <code>true</code> if the given host is valid.  If the host is a dotted-form IP address (like "10.1.45.220"), then the IP address is
     * simply checked for validity.  Otherwise, the host is treated as a domain name and an attempt is made to resolve it to an IP address by making
     * a DNS call.  If the name resolution is successful, returns <code>true</code>.  Note that the execution of this function can take some time,
     * especially if the DNS server is accessible only through a slow network connection.
     *
     * @param _host The host or IP address to check.
     * @return <code>true</code> if the given host or IP address is valid
     */
    @SuppressWarnings( "unused" )
    public static boolean validHost( final String _host ) {

        // if some dummy gave us nothing to operate on, return invalid...
        if( isEmpty( _host ) )
            return false;

        // otherwise, try to resolve it...
        try {
            // see if we can resolve the address...
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName( _host );
        }
        catch( UnknownHostException _e ) {
            return false;
        }

        // if we make it here, then the address was successfully resolved...
        return true;
    }
}
