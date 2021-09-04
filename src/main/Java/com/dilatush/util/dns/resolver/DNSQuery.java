package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSMessage;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Instances of this class contain the elements of a DNS query.
 */
public class DNSQuery {

    private static final Outcome.Forge<DNSQuery> queryOutcome = new Outcome.Forge<>();


    public final long                         timeoutMillis;

    public final DNSQueryTimeout              timeout;

    public final DNSMessage                   queryMessage;

    public final DNSMessage                   responseMessage;

    public final ByteBuffer                   queryData;

    private final Consumer<Outcome<DNSQuery>> handler;


    private DNSQuery( final DNSMessage _queryMessage, final ByteBuffer _queryData, final Consumer<Outcome<DNSQuery>> _handler,
                     final long _timeoutMillis, final DNSMessage _response ) {

        handler         = _handler;
        timeoutMillis   = _timeoutMillis;
        timeout         = new DNSQueryTimeout( timeoutMillis, this::onTimeout );
        queryMessage    = _queryMessage;
        queryData       = _queryData;
        responseMessage = _response;
    }



    public DNSQuery( final DNSMessage _queryMessage, final ByteBuffer _queryData, final Consumer<Outcome<DNSQuery>> _handler, final long _timeoutMillis ) {
        this( _queryMessage, _queryData, _handler, _timeoutMillis, null );
    }

    protected DNSQuery addResponse( final DNSMessage _response ) {
        return new DNSQuery( queryMessage, queryData, handler, timeoutMillis, _response );
    }


    protected void onCompletion() {
        handler.accept( queryOutcome.ok( this ) );
    }


    private void onTimeout() {
        handler.accept( queryOutcome.notOk( "Timeout", new TimeoutException(), this ) );
    }
}
