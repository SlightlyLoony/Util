package com.dilatush.util.dns.resolver;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.channels.SelectionKey.*;

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


    protected synchronized Outcome<?> send( final DNSQuery _query ) {

        boolean wasAdded = sendData.offerFirst( _query.queryData );
        if( !wasAdded )
            return outcome.notOk( "Send data queue full" );

        // if we just added the first data, set write interest on...
        if( sendData.size() == 1 ) {
            try {
                DNSResolver.runner.register( this, channel, OP_WRITE | OP_READ );
                return outcome.ok();
            }
            catch( ClosedChannelException _e ) {
                return outcome.notOk( "Problem registering write interest", _e );
            }
        }

        return outcome.ok();
    }

    protected abstract void write( );
    protected abstract void read();
}
