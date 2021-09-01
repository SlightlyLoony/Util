package com.dilatush.util.dns.resolver;

import java.io.IOException;
import java.nio.channels.*;

import static java.nio.channels.SelectionKey.OP_READ;

class DNSResolverRunner {

    protected final Selector selector;

    protected DNSResolverRunner() throws IOException {

        selector = Selector.open();
    }


    protected void register( final DNSResolver _resolver, final DatagramChannel _udp, final SocketChannel _tcp ) throws ClosedChannelException {

        _udp.register( selector, OP_READ | SelectionKey.OP_WRITE, _resolver );
    }
}
