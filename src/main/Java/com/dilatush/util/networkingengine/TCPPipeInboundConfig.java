package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record TCPPipeInboundConfig( SocketChannel channel, Consumer<Outcome<ByteBuffer>> onReadReadyHandler, Runnable onWriteReadyHandler,
                                    BiConsumer<String,Exception> onErrorHandler ) {}
