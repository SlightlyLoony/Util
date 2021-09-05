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

        // OpenDNS: 208.67.220.220  (secondary)
        // Beast: 10.2.5.200
        InetSocketAddress address = new InetSocketAddress( InetAddress.getByName( "10.2.5.200" ), 53 );
        Outcome<DNSResolver> ro1 = DNSResolver.create( address );
        if( ro1.ok() ) {

            DNSResolver r1 = ro1.info();
//            r1.queryIPv4( "www.cnn.com",  Test::handler, 500);
//            r1.queryIPv4( "www.foxnews.com",  Test::handler, 500);
//            r1.queryIPv4( "paradiseweather.info",  Test::handler, 500);
//            r1.queryIPv4( "beast.dilatush.com", Test::handler, 100 );
            r1.queryAny( "bogus.com", Test::handler, 1000 );


            sleep(3000);
            results.hashCode();
        }
        results.hashCode();
    }


    private static void handler( final Outcome<DNSQuery> _outcome )  {
        results.add( _outcome );
    }
}
