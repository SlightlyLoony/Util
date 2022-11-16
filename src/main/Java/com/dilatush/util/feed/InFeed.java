package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.io.Closeable;
import java.nio.ByteBuffer;

public interface InFeed extends Closeable {


    public abstract void read( final ByteBuffer _buffer, final ReadCompleteHandler _handler );

    public abstract void read( final ByteBuffer _buffer, final int _minBytes, final ReadCompleteHandler _handler );

    public abstract Outcome<ByteBuffer> read( final ByteBuffer _readBuffer );

    public abstract Outcome<ByteBuffer> read( final ByteBuffer _readBuffer, final int _minBytes );

    public abstract void close();
}
