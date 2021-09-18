package com.dilatush.util.dns;

import com.dilatush.util.Checks;
import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;
import com.dilatush.util.dns.rr.A;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import static com.dilatush.util.dns.DNSServerSelectionStrategy.SPEED;
import static com.dilatush.util.dns.agent.DNSQuery.QueryResult;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

/**
 * Instances of this class wrap an instance of {@link DNSResolver} to provide a simpler API.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSResolverAPI {

    private final Logger LOGGER = General.getLogger();

    public final DNSResolver resolver;


    public DNSResolverAPI( final DNSResolver _resolver ) {
        Checks.required( _resolver );
        resolver = _resolver;
    }


    public List<Inet4Address> resolveIPv4( final String _fqdn ) {

        Checks.required( _fqdn );
        Outcome<DNSDomainName> dno = DNSDomainName.fromString( _fqdn );
        if( dno.notOk() ) {
            LOGGER.log( WARNING, "Invalid domain name: " + dno.msg() );
            return null;
        }
        SyncHandler handler = new SyncHandler();
        DNSDomainName dn = dno.info();
        DNSQuestion question = new DNSQuestion( dn, DNSRRType.A );
        resolver.query( question, handler::handler, UDP, SPEED, null );
        handler.waitForCompletion();

        if( handler.qr == null )
            return null;

        if( handler.qr.notOk() ) {
            LOGGER.log( FINE, "Query failed: " + handler.qr.msg() );
            return null;
        }
        List<Inet4Address> result = new ArrayList<>();
        handler.qr.info().response().answers.forEach( (rr)->{
            if( rr instanceof A )
                result.add( ((A)rr).address );
        } );
        return result;
    }


    private static class SyncHandler {

        private Outcome<QueryResult> qr;
        private final Semaphore waiter = new Semaphore( 0 );

        private void handler( final Outcome<QueryResult> _qr ) {
            qr = _qr;
            waiter.release();
        }

        private void waitForCompletion() {
            try {
                waiter.acquire();
            } catch( InterruptedException _e ) {
                // naught to do...
            }
        }
    }
}
