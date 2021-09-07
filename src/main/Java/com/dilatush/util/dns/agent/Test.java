package com.dilatush.util.dns.agent;

import com.dilatush.util.Outcome;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class Test {

    private static final List<Outcome<DNSQuery>> results1 = new ArrayList<>();
    private static final List<Outcome<DNSQuery>> results2 = new ArrayList<>();
    private static final List<Outcome<DNSQuery>> results3 = new ArrayList<>();

    private static Logger LOGGER;

    public static void main( final String[] _args ) throws InterruptedException, UnknownHostException {


        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );
        LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

        LOGGER.info( "Test start..." );

        //DNSResolverRunner.alternateExecutor = new ExecutorService( 10, 100 );

        // OpenDNS: 208.67.220.220  (secondary)
        // Beast: 10.2.5.200
        InetSocketAddress address = new InetSocketAddress( InetAddress.getByName( "10.2.5.200" ), 53 );
        Outcome<DNSServerAgent> ro1 = DNSServerAgent.create( address );
        address = new InetSocketAddress( InetAddress.getByName( "1.1.1.1" ), 53 );
        Outcome<DNSServerAgent> ro2 = DNSServerAgent.create( address );
        address = new InetSocketAddress( InetAddress.getByName( "10.2.5.200" ), 53 );
        Outcome<DNSServerAgent> ro3 = DNSServerAgent.create( address, DNSTransport.UDP, DNSResolution.ITERATIVE );
        if( ro1.ok() && ro2.ok() && ro3.ok() ) {

            DNSServerAgent r1 = ro1.info();
            DNSServerAgent r2 = ro2.info();
            DNSServerAgent r3 = ro3.info();
//            r2.queryIPv4( "www.cnn.com",  Test::handler2, 500);
//            r2.queryIPv4( "www.foxnews.com",  Test::handler2, 500);
//            r2.queryIPv4( "paradiseweather.info",  Test::handler2, 500);
//            r2.queryIPv4( "beast.dilatush.com", Test::handler2, 500 );
//            r1.queryAny( "state.gov", Test::handler1, 2000 );
//            r1.queryAny( "usda.gov", Test::handler1, 2000 );
//            r1.queryAny( "fda.gov", Test::handler1, 2000 );
            r3.queryIPv4( "www.state.com", Test::handler3, 1000 );


            sleep(10000);
            results1.hashCode();
        }
        results1.hashCode();
    }

// TODO: troubleshoot handler not being called, but query actually completed and is still in resolver map...

    private static void handler1( final Outcome<DNSQuery> _outcome )  {
        adder( results1, _outcome );
    }


    private static void handler2( final Outcome<DNSQuery> _outcome )  {
        adder( results2, _outcome );
    }


    private static void handler3( final Outcome<DNSQuery> _outcome )  {
        adder( results3, _outcome );
    }

    private static void adder( final List<Outcome<DNSQuery>> _list, final Outcome<DNSQuery> _outcome ) {
        if( _outcome.ok() ) {
            LOGGER.info( "OK: " + _outcome.info() );
        } else {
            LOGGER.info( "Not OK: " + _outcome.msg() );
        }
        _list.add( _outcome );
    }
}
