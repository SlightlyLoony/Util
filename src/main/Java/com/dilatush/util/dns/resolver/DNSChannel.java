package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public abstract class DNSChannel {

    protected static final Outcome.Forge<?> outcome = new Outcome.Forge<>();


    public final DNSResolver resolver;
    public final SelectableChannel channel;

    protected final AtomicInteger unresolvedQueries;
    protected final Deque<ByteBuffer> sendData;


    protected DNSChannel( final DNSResolver _resolver, final SelectableChannel _channel ) {

        resolver = _resolver;
        channel = _channel;
        unresolvedQueries = new AtomicInteger();
        sendData = new ArrayDeque<>();
    }


    protected abstract Outcome<?> send( final ByteBuffer _data );

    protected abstract void write( );
    protected abstract void read();
}
