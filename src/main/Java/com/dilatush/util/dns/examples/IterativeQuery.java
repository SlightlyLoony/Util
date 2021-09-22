package com.dilatush.util.dns.examples;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolverAPI;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.dilatush.util.General.breakpoint;

/**
 * Create a very simple DNS resolver that uses iterative resolution.
 */
@SuppressWarnings( "unused" )
public class IterativeQuery {

    private static Semaphore                waiter  = new Semaphore( 0 );
    private static List<List<Inet4Address>> results = new ArrayList<>();

    public static void main( final String[] _args ) throws InterruptedException {

        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );

        // create a DNS resolver that doesn't know about any other resolvers...
        DNSResolver.Builder builder = new DNSResolver.Builder();
        builder.setExecutor( new ExecutorService( 10, 100 ) );
        Outcome<DNSResolver> ro = builder.getDNSResolver();
        if( ro.notOk() ) {
            System.out.println( "Could not build resolver: " + ro.msg() );
            return;
        }
        DNSResolver resolver = ro.info();

        // wrap it with an API...
        DNSResolverAPI api = new DNSResolverAPI( resolver );

        // resolve a domain name twice in a row (testing cache)...
        long startTime = System.currentTimeMillis();
        api.resolveIPv4Addresses( IterativeQuery::handler, "www.hp.com" );  // we're ignoring the return value, which should always be "ok"...
        waiter.acquire();
        System.out.println( "First time: " + (System.currentTimeMillis() - startTime) );
        startTime = System.currentTimeMillis();
        api.resolveIPv4Addresses( IterativeQuery::handler, "www.hp.com" );  // we're ignoring the return value, which should always be "ok"...
        waiter.acquire();
        System.out.println( "Second time: " + (System.currentTimeMillis() - startTime) );

        // now do a bunch of concurrent resolutions...
        String[] domains = new String[] {
                "www.cnn.com",
                "www.hp.com",
                "www.servicenow.com",
                "www.paradiseweather.info",
                "news.google.com",
                "www.qq.com",
                "www.burger.com",
                "www.hamburger.com",
                "www.hp.co.uk"
        };
        Iterator<String> it = Arrays.stream( domains ).iterator();
        while( it.hasNext() ) {
            api.resolveIPv4Addresses( IterativeQuery::handler, it.next() );
        }
        waiter.acquire( domains.length );
        results.forEach( (ipa) -> System.out.println( ipa.toString()) );

        breakpoint();
    }

    private static void handler( final Outcome<List<Inet4Address>> _outcome ) {
        if( _outcome.info() == null )
            System.out.println( _outcome.msg() );
        results.add( _outcome.info() );
        waiter.release();
        breakpoint();
    }
}
