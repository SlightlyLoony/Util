package com.dilatush.util.dns;

import com.dilatush.util.Checks;
import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.message.DNSRRType;

import java.net.Inet4Address;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.dns.DNSUtil.extractIPv4Addresses;
import static com.dilatush.util.dns.agent.DNSQuery.QueryResult;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

/**
 * Instances of this class wrap an instance of {@link DNSResolver} to provide a simpler API.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSResolverAPI {

    private static final Logger LOGGER = General.getLogger();
    private static final Outcome.Forge<?> outcome = new Outcome.Forge<>();
    private static final Outcome.Forge<List<Inet4Address>> ipv4Outcome = new Outcome.Forge<>();

    public final DNSResolver resolver;


    public DNSResolverAPI( final DNSResolver _resolver ) {
        Checks.required( _resolver );
        resolver = _resolver;
    }


    public Outcome<?> resolveIPv4Addresses( final Consumer<Outcome<List<Inet4Address>>> _handler, final String _fqdn  ) {

        Checks.required( _fqdn, _handler );

        Outcome<DNSQuestion> qo = DNSUtil.getQuestion( _fqdn, DNSRRType.A );
        if( qo.notOk() )
            return outcome.notOk( qo.msg(), qo.cause() );
        DNSQuestion question = qo.info();
        IPv4Handler handler = new IPv4Handler( _handler );
        return resolver.query( question, handler::handler, UDP, DNSServerSelection.speed() );
    }


    public Outcome<List<Inet4Address>> resolveIPv4Addresses( final String _fqdn ) {

        Checks.required( _fqdn );

        Outcome<DNSQuestion> qo = DNSUtil.getQuestion( _fqdn, DNSRRType.A );
        if( qo.notOk() )
            return ipv4Outcome.notOk( qo.msg(), qo.cause() );
        DNSQuestion question = qo.info();

        SyncHandler handler = new SyncHandler();
        resolver.query( question, handler::handler, UDP, DNSServerSelection.speed() );
        handler.waitForCompletion();

        if( handler.qr.notOk() )
            return ipv4Outcome.notOk( handler.qr.msg(), handler.qr.cause() );
        return ipv4Outcome.ok(extractIPv4Addresses( handler.qr.info().response().answers ) );
    }


    private static class IPv4Handler {

        private final Consumer<Outcome<List<Inet4Address>>> ipv4Handler;

        private IPv4Handler( final Consumer<Outcome<List<Inet4Address>>> _ipv4Handler ) {
            ipv4Handler = _ipv4Handler;
        }

        private void handler( final Outcome<QueryResult> _qr ) {

            if( _qr.notOk() ) ipv4Handler.accept( ipv4Outcome.notOk( _qr.msg(), _qr.cause() ) );
            ipv4Handler.accept( ipv4Outcome.ok( extractIPv4Addresses( _qr.info().response().answers )) );
        }
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
