package com.dilatush.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.Strings.isEmpty;
import static java.lang.Thread.sleep;
import static java.util.logging.Level.FINE;

/**
 * <p>Instances of this class "ping" a host or device with an open TCP port (or one that refuses a connection).  If there's a response (including
 * refusal to connect), then the ping was successful and the response time is returned.  If there's no response at all (not even a refusal), then
 * the ping was unsuccessful.  A successful TCP ping indicates good network connectivity to the host or device.</p>
 * <p>TCPPinger is asynchronous internally, but the convenience method {@link #pingSync(String,int)} is provided to emulate synchronous (blocking)
 * behavior.</p>
 * <p>TCPPinger uses a {@link ScheduledExecutor} instance (either a default instance or a supplied instance) to schedule checks on the progress of the
 * TCP connection, rather than blocking until it is finished.  These checks are performed every millisecond up to 10 milliseconds, then every 10
 * milliseconds up to 500 milliseconds, then every 50 milliseconds up to 2 seconds.  This provides higher resolutions for faster responses, but still
 * keeps the worst-case number of checks to a reasonable number.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class TCPPinger {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // null unless the no-args constructor is ever called, in which case it will refer to a scheduler with a single daemon thread...
    private static volatile ScheduledExecutor defaultScheduler;

    // the scheduler to use with this instance (either one supplied when instantiated, or the default scheduler)...
    private final ScheduledExecutor scheduler;


    /**
     * Creates a new instance of this class using the default scheduler, which has a single daemon thread shared by all instances of Pinger that use
     * the default scheduler.
     */
    public TCPPinger() {

        // non-blocking check to see if we don't already have a default scheduler...
        if( defaultScheduler == null ) {

            // no default scheduler, so synchronize to make sure the default scheduler is created in exactly one thread...
            synchronized( Pinger.class ) {

                // if no other thread has created a default scheduler since our thread invoked this method, then it's up to us...
                if( defaultScheduler == null ) {

                    // create a default scheduler with a single daemon thread...
                    defaultScheduler = new ScheduledExecutor();
                    LOGGER.log( FINE, "Created default scheduler" );
                }
            }
        }

        // now we have a default scheduler for sure, so use it...
        scheduler = defaultScheduler;
    }


    /**
     * Creates a new instance of this class using the given {@link ScheduledExecutor} instance instead of the default scheduler.
     *
     * @param _scheduler  The {@link ScheduledExecutor} instance to use for asynchronous operations.
     */
    public TCPPinger( final ScheduledExecutor _scheduler ) {
        scheduler = _scheduler;
    }


    public void ping( final String _address, final int _port, final Consumer<Outcome<PingResult>> _callback ) {

        // sanity checks...
        if( _callback == null )
            throw new IllegalArgumentException( "No callback supplied for TCP ping" );
        if( isEmpty( _address ) )
            throw new IllegalArgumentException( "No IP address supplied for TCP ping" );
        if( (_port <= 0) || (_port > 65535) )
            throw new IllegalArgumentException( "Invalid port supplied for TCP ping: " + _port );
        InetAddress address;
        try {
            address = InetAddress.getByName( _address );
        }
        catch( UnknownHostException _e ) {
            throw new IllegalArgumentException( "Invalid IP address supplied for TCP ping: " + _address );
        }

        InetSocketAddress socketAddress = new InetSocketAddress( address, _port );

        new Runner( socketAddress, _callback ).run();
    }


    /**
     * Executes a "ping" command synchronously in a local process.  The given IP address string may contain either an IPv4 address (in standard
     * dotted-decimal form) or an IPv6 address (in the forms defined in RFC 2732 or RFC 2373).  This method returns if the ping fails for some reason
     * (in which case the outcome is failure and there will be a diagnostic message and perhaps an exception), or when it completes.  If a response
     * was received for the ping, then {@link PingResult#success} will be {@code true}, and {@link PingResult#responseSeconds} will contain the round
     * trip time in seconds.  If no response was received for the ping, then {@link PingResult#success} will be {@code false} and
     * {@link PingResult#responseSeconds} will contain zero.
     *
     * @param _address The IPv4 or IPv6 address to ping.
     */
    public Outcome<PingResult> pingSync( final String _address, final int _port ) throws InterruptedException {

        // create an instance that will collect the result of the ping and release a permit on a semaphore when the ping finishes...
        Waiter waiter = new Waiter();

        // start our ping...
        ping( _address, _port, waiter::done );

        // block until the ping has finished...
        waiter.semaphore.acquire();

        // return with the results...
        return waiter.result;
    }


    /**
     * A helper class (for emulating synchronous behavior) that does two things:
     * <ul>
     *     <li>Asynchronously collects the results of a ping.</li>
     *     <li>Instantiates a semaphore with no permits, synchronously releases a permit when a ping completes.</li>
     * </ul>
     */
    private static class Waiter {

        // the results of a ping...
        private Outcome<PingResult> result;

        // a semaphore that can be blocked on
        private final Semaphore semaphore = new Semaphore( 0 );


        /**
         * Called when a ping has complete.
         *
         * @param _result The result of a ping.
         */
        private void done( final Outcome<PingResult> _result ) {

            // squirrel away the ping results so that pingSync() can retrieve them...
            result = _result;

            // release a permit so that pingSync() can unblock...
            semaphore.release();
        }
    }


    private class Runner implements Runnable {

        private final InetSocketAddress socketAddress;
        private final Consumer<Outcome<PingResult>> callback;

        // the number of milliseconds to wait before the next check to see if ping has completed...
        private int waitedMS;

        private SocketChannel channel;


        private Runner( final InetSocketAddress _socketAddress, final Consumer<Outcome<PingResult>> _callback ) {
            socketAddress = _socketAddress;
            callback = _callback;
        }


        @Override
        public void run() {

            if( channel == null ) {
                try {
                    channel = SocketChannel.open();
                    channel.configureBlocking( false );
                    channel.connect( socketAddress );
                    scheduler.schedule( this, Duration.ofMillis( 1 ) );
                }
                catch( IOException _e ) {
                    callback.accept( new Outcome<>( false, "Problem trying to connect", _e, null ) );
                }

                return;
            }

            boolean finishedConnecting;
            try {
                finishedConnecting = channel.finishConnect();
            }
            catch( IOException _e ) {

                if( (_e instanceof ConnectException) && (_e.getMessage().contains( "refused" )) ) {

                    try {
                        channel.close();
                    }
                    catch( IOException _f ) {
                        // naught to do...
                    }
                    callback.accept( new Outcome<>( true, null, null, new PingResult( true, waitedMS / 1000d ) ) );
                    return;
                }
                callback.accept( new Outcome<>( false, "Problem trying to finish connecting", _e, null ) );
                return;
            }

            if( !finishedConnecting ) {

                // we need to schedule another check and leave...
                int nextWait;
                if( waitedMS < 10 ) nextWait = 1;
                else if( waitedMS < 500 ) nextWait = 10;
                else if( waitedMS < 2000 ) nextWait = 50;
                else {
                    try {
                        channel.close();
                    }
                    catch( IOException _e ) {
                        // naught to do...
                    }
                    callback.accept( new Outcome<>( true, null, null, new PingResult( false, 0 ) ) );
                    return;
                }
                waitedMS += nextWait;
                scheduler.schedule( this, Duration.ofMillis( nextWait ) );
                return;
            }

            try {
                channel.close();
            }
            catch( IOException _e ) {
                // naught to do...
            }
            callback.accept( new Outcome<>( true, null, null, new PingResult( true, waitedMS / 1000d ) ) );
        }
    }


    public record PingResult(
            boolean success,
            double responseSeconds
    ) {}


    public static void main( String[] _args ) throws InterruptedException {

        TCPPinger pinger = new TCPPinger();
        pinger.ping( "paradiseweather.info", 80, TCPPinger::callback );
        System.out.println( pinger.pingSync( "paradiseweather.info", 80 ) );
        sleep( 3000 );
    }

    private static void callback( Outcome<PingResult> _result ) {
        System.out.println( _result.toString() );
    }
}
