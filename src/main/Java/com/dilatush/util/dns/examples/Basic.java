package com.dilatush.util.dns.examples;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolverAPI;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Create a very simple DNS resolver that can use Google's recursive DNS server to resolve IP addresses.
 */
public class Basic {

    public static void main( final String[] _args ) throws InterruptedException {

        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );

        // create a DNS resolver that knows about Google's recursive DNS server...
        DNSResolver.Builder builder = new DNSResolver.Builder();
        builder.addDNSServer( new InetSocketAddress( "8.8.8.8", 53 ), 2000, 0, "Google" );
        Outcome<DNSResolver> ro = builder.getDNSResolver();
        if( ro.notOk() ) {
            System.out.println( "Could not build resolver: " + ro.msg() );
            return;
        }
        DNSResolver resolver = ro.info();
        DNSResolverAPI api = new DNSResolverAPI( resolver );

        // now get the IP address of some host names
        List<Inet4Address> r1 = api.resolveIPv4( "www.state.gov" );
        List<Inet4Address> r2 = api.resolveIPv4( "www.cnn.com" );
        List<Inet4Address> r3 = api.resolveIPv4( "www.paradiseweather.info" );
        List<Inet4Address> r4 = api.resolveIPv4( "www.aa.com" );
        List<Inet4Address> r5 = api.resolveIPv4( "www.paris.info" );
        List<Inet4Address> r6 = api.resolveIPv4( "www.hamburger.com" );

        "breakpoint".hashCode();
    }
}
