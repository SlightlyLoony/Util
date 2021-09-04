package com.dilatush.util.dns.resolver;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSQueryTimeout extends Timeout {

    private final Runnable handler;

    public DNSQueryTimeout( final long _timeoutMillis, final Runnable _handler ) {
        super( _timeoutMillis );
        handler = _handler;
    }


    @Override
    protected void onTimeout() {
        handler.run();
    }
}
