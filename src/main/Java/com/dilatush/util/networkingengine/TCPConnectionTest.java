package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPAddress;
import com.dilatush.util.ip.IPv4Address;

import java.util.function.Function;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.General.isNull;

/**
 * Instances of this class attempt to connect to a TCP server at a particular IP address and port.
 */
@SuppressWarnings( "unused" )
public class TCPConnectionTest {

    final static private Logger LOGGER = getLogger();

    private static final Outcome.Forge<Boolean>   FORGE_BOOLEAN  = new Outcome.Forge<>();
    private static final Outcome.Forge<AttemptResult> FORGE_ATTEMPT_RESULT = new Outcome.Forge<>();

    private final NetworkingEngine engine;


    public TCPConnectionTest( final NetworkingEngine _engine ) {

        if( isNull( _engine ) ) throw new IllegalArgumentException( "_engine may not be null" );
        engine = _engine;
    }


    /**
     * Attempts to establish a TCP connection to the given IP address and port.  This method will try once, waiting for up to the given timeout period.
     *
     * @param _timeoutMs The timeout in milliseconds to use for the connection attempt.
     * @param _ip The IP address for the connection attempt.
     * @param _port The TCP port for the connection attempt.
     * @return The outcome of the attempt.  If ok, returns {@code SUCCESS} for a successful attempt and {@code TIMEOUT} if the attempt timed out.
     * If not ok, returns {@code FAIL} and there is an explanatory message and possibly the exception that caused the problem.
     */
    public Outcome<AttemptResult> isConnectable( final int _timeoutMs, final IPAddress _ip, final int _port ) {

        // get our pipe...
        var tcpOutcome = TCPOutboundPipe.getNewInstance( engine, IPv4Address.WILDCARD );

        // if we had a problem creating the pipe, we have serious problems - time to bail out...
        if( tcpOutcome.notOk() ) return FORGE_ATTEMPT_RESULT.notOk( tcpOutcome.msg(), tcpOutcome.cause(), AttemptResult.FAIL );

        // get our new pipe...
        var tcp = tcpOutcome.info();

        // make the connection attempt...
        var connectOutcome = tcp.connect( _ip, _port, _timeoutMs );

        // no matter what happened, close the connection...
        tcp.close();

        // if we failed to connect, could be a timeout or a failure...
        if( connectOutcome.notOk() ) {

            LOGGER.finest( "Connect not ok: " + connectOutcome.msg() );

            // if it was a timeout, return ok with a timeout...
            var lcMsg = connectOutcome.msg().toLowerCase();
            if( lcMsg.contains( "timeout" ) || lcMsg.contains( "timed out" ))
                return FORGE_ATTEMPT_RESULT.ok( AttemptResult.TIMEOUT );

            // otherwise we have an actual failure of some kind...
            return FORGE_ATTEMPT_RESULT.notOk( connectOutcome.msg(), connectOutcome.cause(), AttemptResult.FAIL );
        }

        // if we get here, then we succeeded, so return success...
        return FORGE_ATTEMPT_RESULT.ok( AttemptResult.SUCCESS );
    }


    /**
     * Attempts to establish a TCP connection to the given IP address and port.  This method will try up to the given maximum number of attempts, with the given
     * timeout for the first attempt, and the timeout adjusted by the given function on each subsequent attempt,
     *
     * @param _timeoutMs The initial timeout in milliseconds to use for the connection attempt; must be in [1..10000].
     * @param _timeoutAdjust The function to adjust the timeout for each subsequent attempt; output is forced into [1..10000].
     * @param _maxAttempts The maximum number of connection attempts to make; must be in [1..10].
     * @param _ip The IP address for the connection attempt.
     * @param _port The TCP port for the connection attempt.
     * @return The outcome of the attempt.  If ok, returns {@code true} for a successful attempt and {@code false} if the attempt timed out.
     * If not ok, then there is an explanatory message and possibly the exception that caused the problem.
     */
    public Outcome<Boolean> isConnectable( final int _timeoutMs, final Function<Integer,Integer> _timeoutAdjust,
                                            final int _maxAttempts, final IPAddress _ip, final int _port ) {

        // sanity checks...
        if( (_timeoutMs < 1) || (_timeoutMs > 10000) ) throw new IllegalArgumentException( "_timeoutMs must be in [1..10000]" );
        if( (_maxAttempts < 1) || (_maxAttempts > 10) ) throw new IllegalArgumentException( "_maxAttempts must be in [1..10]" );

        var timeoutMs = _timeoutMs;

        // try up to maximum number of times...
        for( int i = 0; i < _maxAttempts; i++ ) {

            // make the attempt...
            var attempt = isConnectable( timeoutMs, _ip, _port );

            // if we succeed (i.e, we connected) then return success...
            if( attempt.info() == AttemptResult.SUCCESS )
                return FORGE_BOOLEAN.ok( true );

            // if we had a failure, then bail out...
            if( attempt.info() == AttemptResult.FAIL )
                return FORGE_BOOLEAN.notOk( attempt );

            // otherwise we must have had a timeout...
            if( attempt.info() != AttemptResult.TIMEOUT )
                throw new IllegalStateException( "Impossible info in attempt: " + attempt.info() );

            // so transform our timeout value...
            timeoutMs = _timeoutAdjust.apply( timeoutMs );
            if( timeoutMs < 1 ) timeoutMs = 1;
            if( timeoutMs > 10000 ) timeoutMs = 10000;
        }

        // we're out of tries, so return failure...
        return FORGE_BOOLEAN.ok( false );
    }

    public enum AttemptResult { SUCCESS, FAIL, TIMEOUT }
}
