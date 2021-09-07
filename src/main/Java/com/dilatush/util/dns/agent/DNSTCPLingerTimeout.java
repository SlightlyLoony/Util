package com.dilatush.util.dns.agent;

public class DNSTCPLingerTimeout extends AbstractTimeout {

    private final Runnable handler;

    public DNSTCPLingerTimeout( final long _timeoutMillis, final Runnable _handler ) {
        super( _timeoutMillis );
        handler = _handler;
    }


    @Override
    protected void onTimeout() {
        handler.run();
    }
}
