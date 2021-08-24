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

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );
    private static final int DEFAULT_TIMEOUT_MS = 2000;

    // null unless the no-args constructor is ever called, in which case it will refer to a scheduler with a single daemon thread...
    private static volatile ScheduledExecutor defaultScheduler;

    // the scheduler to use with this instance (either one supplied when instantiated, or the default scheduler)...
    private final ScheduledExecutor scheduler;


    /**
     * Creates a new instance of this class using the default scheduler, which has a single daemon thread shared by all instances of
     * TCPPinger that use the default scheduler.
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


    /**
     * "Pings" the given host and port by attempting to open a TCP connection to it.  This is an asynchronous method; calling the method starts the
     * "ping", and the callback method is called when it completes or there is an error.  The given IP address string may contain either an IPv4
     * address (in standard dotted-decimal form) or an IPv6 address (in the forms defined in RFC 2732 or RFC 2373).  The callback can be any method
     * that accepts an instance of {@link Outcome Outcome&lt;PingResult&gt;}.  The callback method is called if the ping fails for some reason (in
     * which case the outcome is failure and there will be a diagnostic message and perhaps an exception), when it times out (no response received
     * within the given timeout milliseconds), or when a response is received.  If a response was received for the "ping", then
     * {@link PingResult#success} will be {@code true}, and {@link PingResult#responseSeconds} will contain the response
     * time in seconds.  If no response was received for the "ping", then {@link PingResult#success} will be {@code false} and
     * {@link PingResult#responseSeconds} will contain zero.
     *
     * @param _address The IPv4 or IPv6 address to attempt to open a TCP connection on.
     * @param _port The TCP port to attempt to open a TCP connection on.
     * @param _timeoutMS The maximum time to wait for a response, in milliseconds.
     * @param _callback The method to be called when the ping completes.
     */
    public void ping( final String _address, final int _port, final int _timeoutMS, final Consumer<Outcome<PingResult>> _callback ) {

        // sanity checks...
        if( _callback == null )
            throw new IllegalArgumentException( "No callback supplied for TCP ping" );
        if( isEmpty( _address ) )
            throw new IllegalArgumentException( "No IP address supplied for TCP ping" );
        if( (_port <= 0) || (_port > 65535) )
            throw new IllegalArgumentException( "Invalid port supplied for TCP ping: " + _port );
        if( _timeoutMS < 0 )
            throw new IllegalArgumentException( "Invalid timeout: " + _timeoutMS );
        InetAddress address;
        try {
            address = InetAddress.getByName( _address );
        }
        catch( UnknownHostException _e ) {
            throw new IllegalArgumentException( "Invalid IP address supplied for TCP ping: " + _address );
        }

        // attempt to connect and wait for the result...
        new Runner( new InetSocketAddress( address, _port ), _timeoutMS, _callback ).run();
    }


    /**
     * "Pings" the given host and port by attempting to open a TCP connection to it.  This is an asynchronous method; calling the method starts the
     * "ping", and the callback method is called when it completes or there is an error.  The given IP address string may contain either an IPv4
     * address (in standard dotted-decimal form) or an IPv6 address (in the forms defined in RFC 2732 or RFC 2373).  The callback can be any method
     * that accepts an instance of {@link Outcome Outcome&lt;PingResult&gt;}.  The callback method is called if the ping fails for some reason (in
     * which case the outcome is failure and there will be a diagnostic message and perhaps an exception), when it times out (no response received
     * within two seconds), or when a response is received.  If a response was received for the "ping", then
     * {@link PingResult#success} will be {@code true}, and {@link PingResult#responseSeconds} will contain the response
     * time in seconds.  If no response was received for the "ping", then {@link PingResult#success} will be {@code false} and
     * {@link PingResult#responseSeconds} will contain zero.
     *
     * @param _address The IPv4 or IPv6 address to attempt to open a TCP connection on.
     * @param _port The TCP port to attempt to open a TCP connection on.
     * @param _callback The method to be called when the ping completes.
     */
    public void ping( final String _address, final int _port, final Consumer<Outcome<PingResult>> _callback ) {
        ping( _address, _port, DEFAULT_TIMEOUT_MS, _callback );
    }


    /**
     * "Pings" the given host and port by attempting to open a TCP connection to it.  This is a synchronous method; calling the method starts the
     * "ping" and blocks until it is complete.  The given IP address string may contain either an IPv4
     * address (in standard dotted-decimal form) or an IPv6 address (in the forms defined in RFC 2732 or RFC 2373).  The callback can be any method
     * that accepts an instance of {@link Outcome Outcome&lt;PingResult&gt;}.  The callback method is called if the ping fails for some reason (in
     * which case the outcome is failure and there will be a diagnostic message and perhaps an exception), when it times out (no response received
     * within the given timeout milliseconds), or when a response is received.  If a response was received for the "ping", then
     * {@link PingResult#success} will be {@code true}, and {@link PingResult#responseSeconds} will contain the response
     * time in seconds.  If no response was received for the "ping", then {@link PingResult#success} will be {@code false} and
     * {@link PingResult#responseSeconds} will contain zero.
     *
     * @param _address The IPv4 or IPv6 address to attempt to open a TCP connection on.
     * @param _port The TCP port to attempt to open a TCP connection on.
     * @param _timeoutMS The maximum time to wait for a response, in milliseconds.
     * @return The {@link Outcome Outcome&lt;PingResult&gt;>} containing the result of the ping.
     */
    public Outcome<PingResult> pingSync( final String _address, final int _port, final int _timeoutMS ) throws InterruptedException {

        // create an instance that will collect the result of the ping and release a permit on a semaphore when the ping finishes...
        Waiter waiter = new Waiter();

        // start our ping...
        ping( _address, _port, _timeoutMS, waiter::done );

        // block until the ping has finished...
        waiter.semaphore.acquire();

        // return with the results...
        return waiter.result;
    }


    /**
     * "Pings" the given host and port by attempting to open a TCP connection to it.  This is a synchronous method; calling the method starts the
     * "ping" and blocks until it is complete.  The given IP address string may contain either an IPv4
     * address (in standard dotted-decimal form) or an IPv6 address (in the forms defined in RFC 2732 or RFC 2373).  The callback can be any method
     * that accepts an instance of {@link Outcome Outcome&lt;PingResult&gt;}.  The callback method is called if the ping fails for some reason (in
     * which case the outcome is failure and there will be a diagnostic message and perhaps an exception), when it times out (no response received
     * within two seconds), or when a response is received.  If a response was received for the "ping", then
     * {@link PingResult#success} will be {@code true}, and {@link PingResult#responseSeconds} will contain the response
     * time in seconds.  If no response was received for the "ping", then {@link PingResult#success} will be {@code false} and
     * {@link PingResult#responseSeconds} will contain zero.
     *
     * @param _address The IPv4 or IPv6 address to attempt to open a TCP connection on.
     * @param _port The TCP port to attempt to open a TCP connection on.
     * @return The {@link Outcome Outcome&lt;PingResult&gt;>} containing the result of the ping.
     */
    public Outcome<PingResult> pingSync( final String _address, final int _port ) throws InterruptedException {
        return pingSync( _address, _port, DEFAULT_TIMEOUT_MS );
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


    /**
     * Instances of this helper class attempt a TCP connection, wait for its completion, and report the results.
     */
    private class Runner implements Runnable {

        private final InetSocketAddress socketAddress;
        private final int timeoutMS;
        private final Consumer<Outcome<PingResult>> callback;

        // the number of milliseconds to wait before the next check to see if ping has completed...
        private int waitedMS;

        private SocketChannel channel;


        private Runner( final InetSocketAddress _socketAddress, final int _timeoutMS, final Consumer<Outcome<PingResult>> _callback ) {
            socketAddress = _socketAddress;
            timeoutMS = _timeoutMS;
            callback = _callback;
        }


        @Override
        public void run() {

            // if we don't have a channel yet, then we need to create one and start a connection attempt...
            if( channel == null ) {
                try {
                    channel = SocketChannel.open();
                    channel.configureBlocking( false );
                    channel.connect( socketAddress );
                }
                catch( IOException _e ) {
                    callback.accept( new Outcome<>( false, "Problem creating a channel or trying to connect", _e, null ) );
                }
            }

            try {

                // if we've finished connecting, then close the channel and leave with success and a response time...
                if( channel.finishConnect() ) {
                    close( channel );
                    callback.accept(
                            new Outcome<>( true, null, null, new PingResult( true, waitedMS / 1000d ) )
                    );
                }

                // if we've already waited more than the timeout milliseconds, it's time to fail...
                else if( waitedMS > timeoutMS ) {
                    close( channel );
                    callback.accept( new Outcome<>( true, null, null, new PingResult( false, 0 ) ) );
                }

                // otherwise, we need to schedule another check for completion...
                else {
                    int nextWait = nextWaitMS( waitedMS );
                    waitedMS += nextWait;
                    scheduler.schedule( this, Duration.ofMillis( nextWait ) );
                }

            }

            // we got some kind of exception, which MIGHT be notification of a connection refusal...
            catch( IOException _e ) {

                // no matter what, we need to close the channel...
                close( channel );

                // if this is a "connection refused" exception, treat it just like a connection finished...
                if( (_e instanceof ConnectException) && (_e.getMessage().contains( "refused" )) ) {
                    callback.accept( new Outcome<>( true, null, null, new PingResult( true, waitedMS / 1000d ) ) );
                }

                // otherwise, it was actually an error and we've failed...
                else {
                    callback.accept(new Outcome<>(false, "Problem trying to finish connecting", _e, null));
                }
            }
        }
    }


    /**
     * Compute the amount of time to wait before the next check for completion.
     *
     * @param _waitedMS the number of milliseconds we've waited so far
     * @return the number of milliseconds to wait before the next check for completion
     */
    private int nextWaitMS( final int _waitedMS ) {
        if( _waitedMS < 10 ) return 1;
        if( _waitedMS < 500 ) return 10;
        return 50;
    }


    /**
     * Closes the given {@link SocketChannel}, ignoring any exceptions.
     *
     * @param _channel the {@link SocketChannel} to close.
     */
    private void close( final SocketChannel _channel ) {
        try { _channel.close(); } catch( IOException _e ) { /* naught to do... */ }
    }


    /**
     * Container for ping results:
     * <ul>
     *     <li>{@code success} is {@code true} if any response to the open connection attempt was received</li>
     *     <li>{@code responseSeconds} is set to the time it took for the response to be received if {@code success} is {@code true}; otherwise it is
     *     set to zero</li>
     * </ul>
     */
    public record PingResult(
            boolean success,
            double responseSeconds
    ) {}
}
