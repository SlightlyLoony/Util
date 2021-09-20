package com.dilatush.util.dns.examples;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.agent.DNSQuery;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;

import java.util.concurrent.Semaphore;

import static com.dilatush.util.General.breakpoint;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

/**
 * Create a very simple DNS resolver that can use Google's recursive DNS server to resolve IP addresses.
 */
@SuppressWarnings( "unused" )
public class IterativeQuery {

    private static Semaphore waiter = new Semaphore( 0 );

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

        breakpoint();
    }

    private static void handler( final Outcome<DNSQuery.QueryResult> _outcome ) {

        waiter.release();
        breakpoint();
    }
}
