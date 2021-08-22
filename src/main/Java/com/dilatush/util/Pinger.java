package com.dilatush.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.Misc.IS_LINUX;
import static com.dilatush.util.Misc.IS_OSX;
import static com.dilatush.util.Streams.toUTF8String;
import static com.dilatush.util.Strings.isEmpty;
import static java.util.logging.Level.FINE;

/**
 * <p>Instances of this class execute a "ping" command in a local process and return success (with round trip time) or failure (if no response was
 * received).  A single ping is transmitted, and pinger will wait up to one second for a response.</p>
 * <p>Pinger is asynchronous internally, but the convenience method {@link #pingSync(String)} is provided to emulate synchronous (blocking) behavior.
 * </p>
 * <p>Pinger uses a {@link ScheduledExecutor} instance (either a default instance or a supplied instance) to schedule checks on the progress of the
 * ping command, rather than blocking until it is finished.  These checks are performed at 1ms, 2ms, 4ms, 8ms, and so on (up to 1024ms) after the
 * ping is started.  This allows fast pings to be detected quickly while limiting the checks to a maximum of 10 even for the slowest pings.</p>
 * <p>Note that this class has been implemented and tested only on the Macintosh OSX and Linux operating systems; trying to use it under any other
 * operating system will cause an error.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class Pinger {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final static String  BASIC_PING_REGEX   = "^.*?(\\d+) packets transmitted, (\\d+) packets received,.*$";
    private final static String  RTT_PING_REGEX     = "^.*?(\\d+\\.?\\d*)/.*$";
    private final static Pattern BASIC_PING_PATTERN = Pattern.compile( BASIC_PING_REGEX, Pattern.DOTALL );
    private final static Pattern RTT_PING_PATTERN   = Pattern.compile( RTT_PING_REGEX,   Pattern.DOTALL );

    // null unless the no-args constructor is ever called, in which case it will refer to a scheduler with a single daemon thread...
    private static volatile ScheduledExecutor defaultScheduler;

    // the scheduler to use with this instance (either one supplied when instantiated, or the default scheduler)...
    private final ScheduledExecutor scheduler;


    /**
     * Creates a new instance of this class using the default scheduler, which has a single daemon thread shared by all instances of Pinger that use
     * the default scheduler.
     */
    public Pinger() {

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
    public Pinger( final ScheduledExecutor _scheduler ) {
        scheduler = _scheduler;
    }


    /**
     * Executes a "ping" command asynchronously in a local process.  The given IP address string may contain either an IPv4 address (in standard
     * dotted-decimal form) or an IPv6 address (in the forms defined in RFC 2732 or RFC 2373).  The callback can be any method that accepts an
     * instance of {@link Outcome<PingResult>}.  The callback method is called if the ping fails for some reason (in which case the outcome is failure
     * and there will be a diagnostic message and perhaps an exception), or when it completes.  If a response was received for the ping, then
     * {@link PingResult#success} will be {@code true}, and {@link PingResult#roundTripSeconds} will contain the round trip time in seconds.  If no
     * response was received for the ping, then {@link PingResult#success} will be {@code false} and {@link PingResult#roundTripSeconds} will contain
     * zero.
     *
     * @param _address The IPv4 or IPv6 address to ping.
     * @param _callback The method to be called when the ping completes.
     */
    public void ping( final String _address, final Consumer<Outcome<PingResult>> _callback ) {

        // sanity checks...
        if( _callback == null )
            throw new IllegalArgumentException( "No callback supplied for ping" );
        if( isEmpty( _address ) )
            throw new IllegalArgumentException( "No IP address supplied for ping" );
        InetAddress address;
        try {
            address = InetAddress.getByName( _address );
        }
        catch( UnknownHostException _e ) {
            throw new IllegalArgumentException( "Invalid IP address supplied for ping: " + _address );
        }

        // container for elements of the ping command...
        List<String> pingCommand = new ArrayList<>();

        // our command is "ping", unless we're in OSX, and we have an IPv6 address, in which case it's "ping6"...
        pingCommand.add( (IS_OSX && (address instanceof Inet6Address)) ? "ping6" : "ping" );

        // this part is the same for all supported OSs...
        pingCommand.add( "-c" );        // transmit a single ping...
        pingCommand.add( "1" );
        pingCommand.add( "-n" );        // do not try to resolve host names...
        pingCommand.add( "-q" );        // run quietly (no ping-by-ping results)...

        if( IS_OSX ) {
            pingCommand.add( "-t" );    // timeout of one second on OSX...
            pingCommand.add( "1" );
        }
        else if( IS_LINUX ) {
            pingCommand.add( "-W" );    // timeout of one second on Linux...
            pingCommand.add( "1" );
        }
        else
            throw new IllegalStateException( "Pinger is only implemented for Linux and OS X" );

        // and all supported OSs want the IP address at the end...
        pingCommand.add( _address );

        // create an instance to actually run the ping...
        new Runner( pingCommand, _callback ).run();
    }


    /**
     * Executes a "ping" command synchronously in a local process.  The given IP address string may contain either an IPv4 address (in standard
     * dotted-decimal form) or an IPv6 address (in the forms defined in RFC 2732 or RFC 2373).  This method returns if the ping fails for some reason
     * (in which case the outcome is failure and there will be a diagnostic message and perhaps an exception), or when it completes.  If a response
     * was received for the ping, then {@link PingResult#success} will be {@code true}, and {@link PingResult#roundTripSeconds} will contain the round
     * trip time in seconds.  If no response was received for the ping, then {@link PingResult#success} will be {@code false} and
     * {@link PingResult#roundTripSeconds} will contain zero.
     *
     * @param _address The IPv4 or IPv6 address to ping.
     */
    public Outcome<PingResult> pingSync( final String _address ) throws InterruptedException {

        // create an instance that will collect the result of the ping and release a permit on a semaphore when the ping finishes...
        Waiter waiter = new Waiter();

        // start our ping...
        ping( _address, waiter::done );

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


    /**
     * Instances of this helper class run a ping, wait for its completion, and report the results.
     */
    private class Runner implements Runnable {

        // the ping command to be executed, as string elements of a command line...
        private final List<String> command;

        // the callback method that will receive the results of the ping...
        private final Consumer<Outcome<PingResult>> callback;

        // the process running the ping command...
        private Process pinger;

        // the number of milliseconds to wait before the next check to see if ping has completed...
        private int waitedMS;


        /**
         * Creates a new instance of this class that will run the given ping command and report the results to the given callback method.
         *
         * @param _command  The ping command to be executed, as string elements of a command line
         * @param _callback The callback method that will receive the results of the ping
         */
        private Runner( final List<String> _command, final Consumer<Outcome<PingResult>> _callback ) {
            command  = _command;
            callback = _callback;
            waitedMS = 0;
            pinger   = null;
        }


        /**
         * Called initially by {@link #ping(String,Consumer)} to send the ping, and subsequently by the scheduler to check for completion.
         */
        public void run() {

            // if we have no pinger process, that means we've never run before, and we haven't yet sent a ping - so fix that...
            if( pinger == null ) {

                try {

                    // start the ping going in a local process...
                    pinger = new ProcessBuilder( command ).start();

                    // schedule a check in a millisecond...
                    scheduler.schedule( this, Duration.ofMillis( 1 ) );
                }
                catch( IOException _e ) {

                    // we had a problem executing ping, so report our abject failure...
                    callback.accept( new Outcome<>( false, "Failed to transmit ping", _e, null ) );
                }

                // no matter what happened above, it's time to leave...
                return;
            }

            // if our pinger is still working away...
            if( pinger.isAlive() ) {

                // we need to schedule another check and leave...
                waitedMS =  ( waitedMS == 0 ) ? 1 : waitedMS << 1;  // double the time we last waited...
                scheduler.schedule( this, Duration.ofMillis( waitedMS ) );
                return;
            }

            // the pinger is finished, so it's time to get our results...
            // do we have a bogus exit code?
            if( (pinger.exitValue() > 2) || (pinger.exitValue() < 0) ) {

                // report our somewhat puzzling failure and leave...
                callback.accept( new Outcome<>( false, "Invalid exit code from ping: " + pinger.exitValue(), null, null ) );
                return;
            }

            try {
                // ping gave us a valid exit code, so get the ping results and analyze them...
                String output = toUTF8String( new BufferedInputStream( pinger.getInputStream() ) );

                Matcher mat = BASIC_PING_PATTERN.matcher( output );
                if( mat.matches() ) {

                    // get the ping results...
                    int packetsTransmitted  = Integer.parseInt(   mat.group( 1 ) );
                    int packetsReceived     = Integer.parseInt(   mat.group( 2 ) );

                    // if we transmitted no packets, we've got one kind of problem...
                    if( packetsTransmitted == 0 ) {
                        callback.accept( new Outcome<>( false, "No ping was transmitted", null, null ) );
                    }

                    // if we transmitted one, but received zero, then our pinged host didn't respond...
                    else if( packetsReceived == 0 ) {
                        callback.accept( new Outcome<>( true, null, null, new PingResult( false, 0 ) ) );
                    }

                    // otherwise, our pinged host did respond, and we've got a round trip time...
                    else {
                        mat = RTT_PING_PATTERN.matcher( output );
                        if( mat.matches() ) {
                            double roundTripSeconds = Double.parseDouble( mat.group( 1 ) ) / 1000d;
                            callback.accept( new Outcome<>( true, null, null, new PingResult( true, roundTripSeconds ) ) );
                        }
                        else {
                            callback.accept( new Outcome<>( false, "Can't analyze ping's output:\n" + output, null, null ) );
                        }
                    }
                }

                // if we couldn't analyze ping's output, something is badly wrong...
                else {
                    callback.accept( new Outcome<>( false, "Can't analyze ping's output:\n" + output, null, null ) );
                }
            }

            // this happens if we can't read the input stream...
            catch( IOException _e ) {
                callback.accept( new Outcome<>( false, "Can't read ping's output", _e, null ) );
            }
        }
    }


    /**
     * Container for ping results:
     * <ul>
     *     <li>{@code success} is {@code true} if a response to the ping was received</li>
     *     <li>{@code roundTripSeconds} is set to the time it took for the ping's round trip if {@code success} is {@code true}; otherwise it is
     *     set to zero</li>
     * </ul>
     */
    public record PingResult(
            boolean success,
            double roundTripSeconds
    ){}
}
