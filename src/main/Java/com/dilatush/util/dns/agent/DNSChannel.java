package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.dilatush.util.General.isNull;

public abstract class DNSChannel {

    protected static final Outcome.Forge<?> outcome = new Outcome.Forge<>();


    protected final DNSServerAgent    agent;
    protected final DNSNIO            nio;
    protected final ExecutorService   executor;
    protected final Deque<ByteBuffer> sendData;
    protected final InetSocketAddress serverAddress;


    protected DNSChannel( final DNSServerAgent _agent, final DNSNIO _nio, final ExecutorService _executor, final InetSocketAddress _serverAddress ) {

        if( isNull( _agent, _nio, _executor, _serverAddress ) )
            throw new IllegalArgumentException( "Required argument(s) are missing" );

        agent         = _agent;
        nio           = _nio;
        executor      = _executor;
        serverAddress = _serverAddress;
        sendData      = new ArrayDeque<>();
    }


    protected abstract Outcome<?> send( final DNSMessage _msg );

    protected abstract void register( final Selector _selector, final int _operations, final Object _attachment ) throws ClosedChannelException;

    protected abstract void write();
    protected abstract void read();
    protected abstract void close();
}
