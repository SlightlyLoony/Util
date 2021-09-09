package com.dilatush.util.dns.rr;

import com.dilatush.util.dns.DNSException;

public class DNSTimeoutException extends DNSException {


    public DNSTimeoutException( final String message ) {

        super( message );
    }


    public DNSTimeoutException( final String message, final Throwable cause ) {

        super( message, cause );
    }
}
