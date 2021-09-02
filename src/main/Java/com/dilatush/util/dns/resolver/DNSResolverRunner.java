package com.dilatush.util.dns.resolver;

import com.dilatush.util.ExecutorService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.currentTimeMillis;
import static java.nio.channels.SelectionKey.OP_READ;

class DNSResolverRunner {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    public  static       ExecutorService  alternateExecutor;
    public  static       Long             alternateTimeoutCheckIntervalMillis;

    private static final long             defaultTimeoutCheckIntervalMillis = 50;

    private        final long             timeoutCheckIntervalMillis;
    private        final ExecutorService  executor;
    private        final Selector         selector;
    private        final Thread           ioRunner;
    private        final Timeouts         timeouts;

    private              long             nextTimeoutCheck;


    /**
     * Creates a new instance of this class.  The new instance will start a daemon thread that does the bulk of the work of this class, which is to handle the low-level (UDP and
     * TCP) I/O for the DNS resolver.  By default, received data and timeout handlers are called through a single-threaded {@link ExecutorService} instance (with a daemon thread
     * and a queue of 100).  However, an alternate {@link ExecutorService} can be used by setting {@link #alternateExecutor} prior to calling {@link DNSResolver#create} for the
     * first time in any given process.
     *
     * @throws IOException if the selector can't be opened for some reason.
     */
    protected DNSResolverRunner() throws IOException {

        // get our timeouts manager...
        timeouts = new Timeouts();

        // open the selector we're going to use for all our I/O...
        selector = Selector.open();

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


    protected void register( final DNSResolver _resolver, final DatagramChannel _udp, final SocketChannel _tcp ) throws ClosedChannelException {

        _udp.register( selector, OP_READ, _resolver );
        _tcp.register( selector, OP_READ, _resolver );
    }


    protected void send( final DNSQuery _query ) {

        // add the data to our queue of data to send...
        _query.resolver.sendData.offerFirst( _query.queryData );

        // increment our count of data blocks to send...
        int count = _query.resolver.sendsOutstanding.incrementAndGet();

        // set our write interest if necessary...
        if( count == 1 ) {
            if( _query.transport == Transport.UDP ) {

                try {
                    _query.resolver.udpChannel.register( selector, SelectionKey.OP_WRITE | OP_READ, _query );
                } catch( ClosedChannelException _e ) {
                    // TODO: handle closed channel by sending a not ok outcome
                }
            }
        }
    }


    /**
     * The main I/O loop for {@link DNSResolver}s.
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

                    if( key.isValid() && key.isWritable() ) {

                        DNSQuery query = (DNSQuery) key.attachment();  // TODO: more safely here...
                        if( query.transport == Transport.UDP ) {
                            query.resolver.udpChannel.write( query.resolver.sendData.pollLast() );  // TODO: more safely here...
                            int count = query.resolver.sendsOutstanding.decrementAndGet();
                            if( count == 0 ) {
                                try {
                                    query.resolver.udpChannel.register( selector, OP_READ, query );
                                } catch( ClosedChannelException _e ) {
                                    // TODO: return a not ok outcome...
                                }
                            }
                        }
                    }

                    if( key.isValid() && key.isReadable() ) {
                        DNSQuery query = (DNSQuery) key.attachment();  // TODO: more safely here...
                        if( query.transport == Transport.UDP ) {
                            ByteBuffer readBuffer = ByteBuffer.allocate( 512 );
                            int read = query.resolver.udpChannel.read( readBuffer );  // TODO: more safely here...
                            readBuffer.hashCode();
                        }

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
