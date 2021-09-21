package com.dilatush.util.dns.examples;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.dilatush.util.General.breakpoint;
import static com.dilatush.util.dns.agent.DNSQuery.QueryResult;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

/**
 * Create a very simple DNS resolver that uses iterative resolution.
 */
@SuppressWarnings( "unused" )
public class IterativeQuery {

    private static Semaphore waiter = new Semaphore( 0 );
    private static List<QueryResult> results = new ArrayList<>();

    public static void main( final String[] _args ) throws InterruptedException {

        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );

        // create a DNS resolver that doesn't know about any other resolvers...
        DNSResolver.Builder builder = new DNSResolver.Builder();
        Outcome<DNSResolver> ro = builder.getDNSResolver();
        if( ro.notOk() ) {
            System.out.println( "Could not build resolver: " + ro.msg() );
            return;
        }
        DNSResolver resolver = ro.info();

        DNSDomainName dn = DNSDomainName.fromString( "www.paradiseweather.info" ).info();
        DNSQuestion question = new DNSQuestion( dn, DNSRRType.A );
        long startTime = System.currentTimeMillis();
        resolver.query( question, IterativeQuery::handler, UDP );
        waiter.acquire();
        System.out.println( "First time: " + (System.currentTimeMillis() - startTime) );
        startTime = System.currentTimeMillis();
        resolver.query( question, IterativeQuery::handler, UDP );
        waiter.acquire();
        System.out.println( "Second time: " + (System.currentTimeMillis() - startTime) );

        String[] domains = new String[] { "www.cnn.com", "www.hp.com", "www.servicenow.com", "www.paradiseweather.info",
                "news.google.com", "www.qq.com", "www.burger.com", "www.hamburger.com", "www.hp.co.uk" };
        Iterator<String> it = Arrays.stream( domains ).iterator();
        while( it.hasNext() ) {
            dn = DNSDomainName.fromString( it.next() ).info();
            question = new DNSQuestion( dn, DNSRRType.A );
            resolver.query( question, IterativeQuery::handler, UDP );
        }
        waiter.acquire( domains.length );

        breakpoint();
    }

    private static void handler( final Outcome<QueryResult> _outcome ) {
        if( _outcome.info() == null )
            System.out.println( _outcome.msg() );
        results.add( _outcome.info() );
        waiter.release();
        breakpoint();
    }
}
