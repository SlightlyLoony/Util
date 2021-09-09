package com.dilatush.util.dns;

public class DNSException extends Exception {

    public DNSException( final String message ) {
        super( message );
    }

    public DNSException( final String message, final Throwable cause ) {
        super( message, cause );
    }
}
