package com.dilatush.util.dns.agent;

import com.dilatush.util.ExecutorService;
import com.dilatush.util.dns.DNSException;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.currentTimeMillis;
import static java.nio.channels.SelectionKey.OP_READ;

public class DNSNIO {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    public  static       ExecutorService  alternateExecutor;
    public  static       Long             alternateTimeoutCheckIntervalMillis;

    private static final long             defaultTimeoutCheckIntervalMillis = 50;

    private        final long             timeoutCheckIntervalMillis;
    protected      final ExecutorService  executor;
    private        final Selector         selector;
    private        final Thread           ioRunner;
    private        final Timeouts         timeouts;

    private              long             nextTimeoutCheck;


    /**
     * Creates a new instance of this class.  The new instance will start a daemon thread that does the bulk of the work of this class, which is to handle the low-level (UDP and
     * TCP) I/O for the DNS resolver.  By default, received data and timeout handlers are called through a single-threaded {@link ExecutorService} instance (with a daemon thread
     * and a queue of 100).
     *
     * @throws DNSException if the selector can't be opened for some reason.
     */
    public DNSNIO() throws DNSException {

        // get our timeouts manager...
        timeouts = new Timeouts();

        // open the selector we're going to use for all our I/O...
        try {
            selector = Selector.open();
        }
        catch( IOException _e ) {
            throw new DNSException( "Problem opening selector", _e );
        }

        // use the alternate executor if it was supplied; otherwise, use a default executor...
        executor = (alternateExecutor != null) ? alternateExecutor : new ExecutorService();

        // use the alternate timeout check interval if it was supplied; otherwise, use the default...
        timeoutCheckIntervalMillis = (alternateTimeoutCheckIntervalMillis != null) ? alternateTimeoutCheckIntervalMillis : defaultTimeoutCheckIntervalMillis;

        // create and start our I/O thread...
        ioRunner = new Thread( this::ioLoop );
        ioRunner.setDaemon( true );
        ioRunner.setName( "IO Runner" );
        ioRunner.start();
    }


    protected void register( final DNSChannel _dnsChannel, final SelectableChannel _channel, final int _ops ) throws ClosedChannelException {
        _channel.register( selector, _ops, _dnsChannel );
    }


    protected void addTimeout( final AbstractTimeout _timeout ) {
        timeouts.add( _timeout );
    }


    /**
     * The main I/O loop for {@link DNSServerAgent}s.
     */
    private void ioLoop() {

        // we're going to loop here basically forever...
        while( !ioRunner.isInterrupted() ) {

            // any exceptions in this code are a serious problem; we just log and make no attempt to recover...
            try {
                // select and get any keys...
                int numKeys = selector.select( timeoutCheckIntervalMillis );
                Set<SelectionKey> keys = selector.selectedKeys();

                // handle any keys we got...
                Iterator<SelectionKey> keyIterator = keys.iterator();
                while( keyIterator.hasNext() ) {

                    SelectionKey key = keyIterator.next();
                    DNSTCPChannel tcp  = (key.attachment() instanceof DNSTCPChannel) ? (DNSTCPChannel) key.attachment() : null;
                    DNSChannel channel = (key.attachment() instanceof DNSChannel)    ? (DNSChannel)    key.attachment() : null;

                    if( key.isValid() && key.isWritable() ) {
                        if( channel != null) channel.write();
                    }

                    if( key.isValid() && key.isReadable() ) {
                        if( channel != null ) channel.read();
                    }

                    if( key.isValid() && key.isConnectable() ) {
                        if( tcp != null ) tcp.tcpChannel.finishConnect();
                    }

                    // get rid the key we just processed...
                    keyIterator.remove();
                }

                // see if it's time for us to check the timeouts...
                if( currentTimeMillis() >= nextTimeoutCheck ) {

                    // check 'em...
                    timeouts.check();

                    // figure out when we should check again...
                    nextTimeoutCheck = currentTimeMillis() + timeoutCheckIntervalMillis;
                }
            }

            // getting here means something majorly wrong happened; log and let the loop die...
            catch( IOException _e ) {

                LOGGER.log( Level.SEVERE, "Unhandled exception in NIO selector loop", _e );
                break;
            }
        }
    }
}