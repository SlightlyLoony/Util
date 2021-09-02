package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSMessage;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.function.Consumer;

/**
 * Instances of this class the elements of a DNS query.  Instances are created before {@link DNSResolver} sends each query.  Each query has
 * a unique ID (within a given {@link DNSResolver} instance.  This ID, a 16-bit integer, is stored within the query message.  Just after it is
 * created, each instance of this class is mapped by its ID in the {@link DNSResolver} instance; this allows the response {@link DNSMessage}s to be
 * matched up with the query that produced them.  After the mapping, each instance is passed to the {@link DNSResolverRunner} to have the query sent.
 */
public class DNSQuery {

    public final Transport transport;

    public final Timeout timeout;

    public final Consumer<Outcome<DNSMessage>> onDone;

    public final DNSMessage queryMessage;

    public final ByteBuffer queryData;

    public final DNSResolver resolver;

    
    public DNSQuery( final Transport _transport, final Timeout _timeout, final Consumer<Outcome<DNSMessage>> _onDone,
                     final DNSMessage _queryMessage, final ByteBuffer _queryData, final DNSResolver _resolver ) {

        transport    = _transport;
        timeout      = _timeout;
        onDone       = _onDone;
        queryMessage = _queryMessage;
        queryData    = _queryData;
        resolver     = _resolver;
    }
}
