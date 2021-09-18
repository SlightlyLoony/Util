package com.dilatush.util.dns.examples;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolverAPI;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.List;

import static com.dilatush.util.General.breakpoint;

/**
 * Create a very simple DNS resolver that can use Google's recursive DNS server to resolve IP addresses.
 */
@SuppressWarnings( "unused" )
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
        // this also "warms up" the VM...
        List<Inet4Address> r1a = api.resolveIPv4( "www.state.gov" );
        List<Inet4Address> r2a = api.resolveIPv4( "www.cnn.com" );
        List<Inet4Address> r3a = api.resolveIPv4( "www.paradiseweather.info" );
        List<Inet4Address> r4a = api.resolveIPv4( "www.aa.com" );
        List<Inet4Address> r5a = api.resolveIPv4( "www.paris.info" );
        List<Inet4Address> r6a = api.resolveIPv4( "www.hamburger.com" );

        // now we clear the cache and time a new run...
        resolver.clear();
        long startTime = System.currentTimeMillis();

        List<Inet4Address> r1b = api.resolveIPv4( "www.state.gov" );
        List<Inet4Address> r2b = api.resolveIPv4( "www.cnn.com" );
        List<Inet4Address> r3b = api.resolveIPv4( "www.paradiseweather.info" );
        List<Inet4Address> r4b = api.resolveIPv4( "www.aa.com" );
        List<Inet4Address> r5b = api.resolveIPv4( "www.paris.info" );
        List<Inet4Address> r6b = api.resolveIPv4( "www.hamburger.com" );

        System.out.println( "Uncached resolution time (ms): " + (System.currentTimeMillis() - startTime));

        // now we run the same queries again; all should be cached (unless we had some VERY short TTLs!)...
        startTime = System.currentTimeMillis();

        List<Inet4Address> r1c = api.resolveIPv4( "www.state.gov" );
        List<Inet4Address> r2c = api.resolveIPv4( "www.cnn.com" );
        List<Inet4Address> r3c = api.resolveIPv4( "www.paradiseweather.info" );
        List<Inet4Address> r4c = api.resolveIPv4( "www.aa.com" );
        List<Inet4Address> r5c = api.resolveIPv4( "www.paris.info" );
        List<Inet4Address> r6c = api.resolveIPv4( "www.hamburger.com" );

        System.out.println( "Cached resolution time (ms): " + (System.currentTimeMillis() - startTime));

        breakpoint();
    }
}
