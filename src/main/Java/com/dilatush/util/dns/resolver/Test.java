package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class Test {

    private static final List<Outcome<DNSQuery>> results = new ArrayList<>();

    public static void main( final String[] _args ) throws InterruptedException, UnknownHostException {


        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );

        InetSocketAddress address = new InetSocketAddress( InetAddress.getByName( "8.8.8.8" ), 53 );
        Outcome<DNSResolver> ro1 = DNSResolver.create( address );
        if( ro1.ok() ) {

            DNSResolver r1 = ro1.info();
            r1.query( "www.cnn.com",  Test::handler, 500);
            r1.query( "www.foxnews.com",  Test::handler, 500);
            r1.query( "paradiseweather.info",  Test::handler, 500);

            sleep(1000);
            results.hashCode();
        }
        results.hashCode();
    }


    private static void handler( final Outcome<DNSQuery> _outcome )  {
        results.add( _outcome );
    }
}
