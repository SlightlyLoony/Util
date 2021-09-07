package com.dilatush.util.dns.agent;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DNSChannel {

    protected static final Outcome.Forge<?> outcome = new Outcome.Forge<>();


    public final DNSServerAgent resolver;
    public final SelectableChannel channel;

    protected final AtomicInteger unresolvedQueries;
    protected final Deque<ByteBuffer> sendData;


    protected DNSChannel( final DNSServerAgent _resolver, final SelectableChannel _channel ) {

        resolver = _resolver;
        channel = _channel;
        unresolvedQueries = new AtomicInteger();
        sendData = new ArrayDeque<>();
    }


    protected abstract Outcome<?> send( final ByteBuffer _data );

    protected abstract void write( );
    protected abstract void read();
}
