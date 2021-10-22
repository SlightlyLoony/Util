package com.dilatush.util.ip;

import com.dilatush.util.Checks;
import com.dilatush.util.Outcome;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Instances of this class represent a host name as a standard DNS formatted string, and zero or more IP addresses for that host.  Instances of this class are immutable and
 * threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class IPHost {

    /** The hostname (which must follow standard DNS format, as defined in RFC 1034). */
    public final String          hostname;

    /** Zero or more IP addresses (either IPv4 or IPv6) for this host. */
    public final List<IPAddress> ipAddresses;

    private static final Outcome.Forge<IPHost> outcomeIPHost = new Outcome.Forge<>();

    private static final Pattern DOMAIN_NAME_CHECK
            = Pattern.compile( "((?:(?:[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61})?[a-zA-Z0-9]\\.)*?(?:(?:[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61})?[a-zA-Z0-9])?)\\.?$" );


    /**
     * Creates a new instance of {@link IPHost} with the given hostname and list of IP addresses.  Note that this constructor is private, and that it's arguments are not
     * validated.
     *
     * @param _hostname The hostname for this instance.
     * @param _ipAddresses The list of IP addresses for this instance.
     */
    private IPHost( final String _hostname, final List<IPAddress> _ipAddresses ) {
        hostname    = _hostname;
        ipAddresses = Collections.unmodifiableList( _ipAddresses );
    }


    /**
     * Create a new instance of {@link IPHost} that is exactly the same as this instance, except that the given address is added.
     *
     * @param _ipAddress The IP address to add.
     * @return The new {@link IPHost}.
     */
    public IPHost add( final IPAddress _ipAddress ) {

        Checks.required( _ipAddress );

        List<IPAddress> ips = new ArrayList<>( ipAddresses );
        ips.add( _ipAddress );
        return new IPHost( hostname, ips );
    }


    public InetAddress getInetAddress() {
        return InetAddress.
    }


    /**
     * Returns a list (possibly empty) of only the IPv4 addresses in this instance.
     *
     * @return A list (possibly empty) of only the IPv4 addresses in this instance.
     */
    public List<IPv4Address> getIPv4Addresses() {
        return ipAddresses
                .stream()
                .filter( (ipa) -> ipa instanceof IPv4Address )
                .map( (ipa) -> (IPv4Address)ipa )
                .collect( Collectors.toList());
    }


    /**
     * Returns a list (possibly empty) of only the IPv6 addresses in this instance.
     *
     * @return A list (possibly empty) of only the IPv6 addresses in this instance.
     */
    public List<IPv6Address> getIPv6Addresses() {
        return ipAddresses
                .stream()
                .filter( (ipa) -> ipa instanceof IPv6Address )
                .map( (ipa) -> (IPv6Address)ipa )
                .collect( Collectors.toList());
    }


    /**
     * Attempts to create a new instance of {@link IPHost}, with the given hostname and IP addresses.  If there is a problem, returns not ok with an explanatory message.
     * Otherwise, returns ok with the new {@link IPHost} instance.
     *
     * @param _hostname The hostname for this instance.
     * @param _ipAddresses The IP addresses (which may be an empty list) for this instance.
     * @return The {@link Outcome Outcome&lt;IPHost&gt;} with the result.
     */
    public static Outcome<IPHost> create( final String _hostname, final List<IPAddress> _ipAddresses ) {

        Checks.required( _hostname, _ipAddresses );

        // validate the hostname, except for overall length, and normalize (remove any trailing period)...
        Matcher mat = DOMAIN_NAME_CHECK.matcher( _hostname );
        if( !mat.matches() )
            return outcomeIPHost.notOk( "Invalid hostname: " + _hostname );

        // get the validated, normalized host name, and make sure it's not too long...
        String hostname = mat.group( 1 );
        if( hostname.length() >= 255 )
            return outcomeIPHost.notOk( "Hostname is too long (limit is 254 characters): " + _hostname );

        // all is ok, so construct and return our shiny new IPHost instance...
        return outcomeIPHost.ok( new IPHost( hostname, _ipAddresses ) );
    }


    /**
     * Attempts to create a new instance of {@link IPHost}, with the given hostname and no IP addresses.  If there is a problem, returns not ok with an explanatory message.
     * Otherwise, returns ok with the new {@link IPHost} instance.
     *
     * @param _hostname The hostname for this instance.
     * @return The {@link Outcome Outcome&lt;IPHost&gt;} with the result.
     */
    public static Outcome<IPHost> create( final String _hostname ) {
        return create( _hostname, new ArrayList<>(0) );
    }


    /**
     * Attempts to create a new instance of {@link IPHost}, with the given hostname and IP address.  If there is a problem, returns not ok with an explanatory message.
     * Otherwise, returns ok with the new {@link IPHost} instance.
     *
     * @param _hostname The hostname for this instance.
     * @param _ipAddress The IP address for this instance.
     * @return The {@link Outcome Outcome&lt;IPHost&gt;} with the result.
     */
    public static Outcome<IPHost> create( final String _hostname, final IPAddress _ipAddress ) {

        Checks.required( _ipAddress );
        List<IPAddress> address = new ArrayList<>( 1 );
        address.add( _ipAddress );
        return create( _hostname, address );
    }


    @Override
    public boolean equals( final Object _o ) {
        if( this == _o ) return true;
        if( _o == null || getClass() != _o.getClass() ) return false;
        IPHost ipHost = (IPHost) _o;
        return hostname.equals( ipHost.hostname ) && ipAddresses.equals( ipHost.ipAddresses );
    }


    @Override
    public int hashCode() {
        return Objects.hash( hostname, ipAddresses );
    }
}
