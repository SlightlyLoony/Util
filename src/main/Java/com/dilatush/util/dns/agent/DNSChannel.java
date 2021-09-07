package com.dilatush.util.dns.agent;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class DNSChannel {

    protected static final Outcome.Forge<?> outcome = new Outcome.Forge<>();


    protected final DNSServerAgent    agent;
    protected final DNSNIO            nio;
    protected final Deque<ByteBuffer> sendData;


    protected DNSChannel( final DNSServerAgent _agent, final DNSNIO _nio ) {

        agent = _agent;
        nio = _nio;
        sendData = new ArrayDeque<>();
    }


    protected abstract Outcome<?> send( final ByteBuffer _data );

    protected abstract void register( final Selector _selector, final int _operations, final Object _attachment ) throws ClosedChannelException;

    protected abstract void write( );
    protected abstract void read();
}
