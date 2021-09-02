package com.dilatush.util.dns.resolver;

import com.dilatush.util.ExecutorService;

import java.io.IOException;
import java.nio.channels.*;

import static java.nio.channels.SelectionKey.OP_READ;

class DNSResolverRunner {

    public static ExecutorService alternateExecutor;

    private final ExecutorService executor;
    private final Selector        selector;
    private final Thread          ioRunner;


    /**
     * Creates a new instance of this class.  The new instance will start a daemon thread that does the bulk of the work of this class, which is to handle the low-level (UDP and
     * TCP) I/O for the DNS resolver.  By default, received data and timeout handlers are called through a single-threaded {@link ExecutorService} instance (with a daemon thread
     * and a queue of 100).  However, an alternate {@link ExecutorService} can be used by setting {@link #alternateExecutor} prior to calling {@link DNSResolver#create} for the
     * first time in any given process.
     *
     * @throws IOException if the selector can't be opened for some reason.
     */
    protected DNSResolverRunner() throws IOException {

        // open the selector we're going to use for all our I/O...
        selector = Selector.open();

        // use the alternate executor if it was supplied; otherwise, use a default executor...
        executor = (alternateExecutor != null) ? alternateExecutor : new ExecutorService();

        // create and start our I/O thread...
        ioRunner = new Thread( this::ioLoop );
        ioRunner.setDaemon( true );
        ioRunner.setName( "IO Runner" );
        ioRunner.start();
    }


    protected void register( final DNSResolver _resolver, final DatagramChannel _udp, final SocketChannel _tcp ) throws ClosedChannelException {

        _udp.register( selector, OP_READ | SelectionKey.OP_WRITE, _resolver );
    }


    /**
     * The main I/O loop for {@link DNSResolver}s.
     */
    private void ioLoop() {

        // we're gonna loop basically forever...
        while( !ioRunner.isInterrupted() ) {

            // w
        }
    }
}
