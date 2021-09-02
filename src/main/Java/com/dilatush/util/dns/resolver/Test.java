package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSMessage;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;

public class Test {


    public static void main( final String[] _args ) throws InterruptedException, UnknownHostException {


        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );

        InetSocketAddress address = new InetSocketAddress( InetAddress.getByName( "10.2.5.200" ), 53 );
        Outcome<DNSResolver> ro1 = DNSResolver.create( address );
        if( ro1.ok() ) {

            DNSResolver r1 = ro1.info();
            r1.query( "www.cnn.com",  Test::handler, 500);
        }

        sleep(1000);
    }


    private static void handler( final Outcome<DNSMessage> _outcome )  {
        System.out.println( _outcome );
    }
}
