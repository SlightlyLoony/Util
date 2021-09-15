package com.dilatush.util.dns.examples;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.agent.DNSQuery;
import com.dilatush.util.dns.rr.DNSResourceRecord;

import java.net.InetSocketAddress;
import java.util.concurrent.Semaphore;

import static com.dilatush.util.dns.DNSServerSelectionStrategy.NAMED;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

/**
 * Create a very simple DNS resolver that can use Google's recursive DNS server to resolve IP addresses.
 */
public class Basic {

    private static final Semaphore waiter = new Semaphore( 0 );

    public static void main( final String[] _args ) throws InterruptedException {

        // create a DNS resolver that knows about Google's recursive DNS server...
        DNSResolver.Builder builder = new DNSResolver.Builder();
        builder.addDNSServer( new InetSocketAddress( "8.8.8.8", 53 ), 2000, 0, "Google" );
        Outcome<DNSResolver> ro = builder.getDNSResolver();
        if( ro.notOk() ) {
            System.out.println( "Could not build resolver: " + ro.msg() );
            return;
        }
        DNSResolver resolver = ro.info();

        // now get the IP address of some host names
        resolver.queryIPv4( "www.state.gov", Basic::handler, UDP, NAMED, "Google" );
        resolver.queryIPv4( "www.cnn.com", Basic::handler, UDP, NAMED, "Google" );
        resolver.queryIPv4( "www.paradiseweather.info", Basic::handler, UDP, NAMED, "Google" );
        resolver.queryIPv4( "www.aa.com", Basic::handler, UDP, NAMED, "Google" );
        resolver.queryIPv4( "www.paris.info", Basic::handler, UDP, NAMED, "Google" );
        resolver.queryIPv4( "www.hamburger.com", Basic::handler, UDP, NAMED, "Google" );

        // wait for completion...
        waiter.acquire(6);
    }

    private static void handler( final Outcome<DNSQuery.QueryResult> _result ) {

        if( _result.notOk() ) {
            System.out.println( "Query failed: " + _result.msg() );
            return;
        }
        System.out.println( "Query successful" );

        for( DNSResourceRecord rr : _result.info().response().answers ) {
            System.out.println( rr );
        }

        System.out.println( "Query log:" );
        for( DNSQuery.QueryLogEntry entry : _result.info().log() ) {
            System.out.println( entry );
        }
        System.out.println( "" );
        waiter.release();
    }
}
