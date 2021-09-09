package com.dilatush.util.dns;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.agent.DNSQuery;
import com.dilatush.util.dns.agent.DNSResolution;
import com.dilatush.util.dns.agent.DNSServerAgent;
import com.dilatush.util.dns.agent.DNSTransport;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.dns.DNSServerSelectionStrategy.*;
import static com.dilatush.util.dns.agent.DNSQuery.*;
import static com.dilatush.util.dns.agent.DNSTransport.*;
import static java.lang.Thread.sleep;

public class Test {

    private static final List<Outcome<QueryResult>> results = new ArrayList<>();

    private static Logger LOGGER;

    public static void main( final String[] _args ) throws InterruptedException, UnknownHostException {


        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );
        LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

        LOGGER.info( "Test start..." );

        DNSResolver.Builder builder = new DNSResolver.Builder();
//        builder.addDNSServer( new InetSocketAddress( InetAddress.getByName( "10.2.5.200" ),     53 ), 1000, 0, "Beast"      );
        builder.addDNSServer( new InetSocketAddress( InetAddress.getByName( "8.8.8.8" ),        53 ), 1000, 0, "Google"     );
//        builder.addDNSServer( new InetSocketAddress( InetAddress.getByName( "208.67.220.220" ), 53 ), 1000, 0, "OpenDNS"    );
//        builder.addDNSServer( new InetSocketAddress( InetAddress.getByName( "1.1.1.1" ),        53 ), 1000, 0, "CloudFlare" );

        Outcome<DNSResolver> ro = builder.getDNSResolver();
        if( ro.notOk() ) {
            LOGGER.log( Level.SEVERE, ro.msg(), ro.cause() );
            return;
        }

        DNSResolver resolver = ro.info();

        resolver.queryIPv4( "www.cnn.com",          Test::handler, UDP, SPEED, null );
        resolver.queryIPv4( "www.foxnews.com",      Test::handler, UDP, SPEED, null );
        resolver.queryIPv4( "paradiseweather.info", Test::handler, UDP, SPEED, null );
        resolver.queryIPv4( "beast.dilatush.com",   Test::handler, UDP, SPEED, null );
        resolver.queryIPv4( "www.state.gov",        Test::handler, UDP, SPEED, null );
        resolver.queryIPv4( "www.usda.gov",         Test::handler, UDP, SPEED, null );
        resolver.queryIPv4( "www.fda.gov",          Test::handler, UDP, SPEED, null );
        resolver.queryIPv4( "www.state.com",        Test::handler, UDP, SPEED, null );



        sleep(3000);
        "".hashCode();

    }


    private static void handler( final Outcome<QueryResult> _outcome ) {
        results.add( _outcome );
    }
}
