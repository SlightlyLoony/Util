package com.dilatush.util.fsm.example;

import com.dilatush.util.Threads;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleUnaryOperator;

import static com.dilatush.util.fsm.example.Engine.Mode.OFF;
import static com.dilatush.util.fsm.example.Engine.Mode.ON;


/**
 * Simulated engine for testing.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EngineSim implements Engine {

    private enum State {IDLE,STARTING,RUNNING,BROKEN}

    private Mode                power    = OFF;
    private Mode                fuel     = OFF;
    private Mode                starter  = OFF;

    private State               state    = State.IDLE;
    private double              rpm      = 0;
    private DoubleUnaryOperator scenario = null;
    private int                 ticker   = 0;     // state for scenarios...
    private int                 runtime  = 0;

    private final Random        random   = new Random( System.currentTimeMillis() );


    public EngineSim() {

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor( new Threads.DaemonThreadFactory( "EngineSim" ) );
        executor.scheduleAtFixedRate( this::tick, 0, 250, TimeUnit.MILLISECONDS );
    }


    /**
     * Runs every 250 milliseconds update our simulation...
     */
    private void tick() {

        // update our scenario ticker...
        ticker++;

        // update the scenario we're playing...
        if( (state != State.BROKEN) && (scenario != null) ) {
            rpm = scenario.applyAsDouble( rpm );
        }

        // update our starter motor runtime...
        if( starter == ON ) {
            runtime++;
            if( runtime > (15 * 4) ) {
                state = State.BROKEN;
                rpm = 0;
            }
        }
        else
            runtime = 0;

        if( (ticker & 3) == 0 )
            out( rpm );
    }


    private void stateCheck() {

        switch( state ) {

            case BROKEN:
                rpm = 0;
                break;

            case IDLE:

                // if the fuel is on, the power is on, and the starter is on or the RPMs are over 50, we'll start up...
                if( (power == ON) && (fuel == ON) && ((starter == ON) || (rpm >= 50)) ) {
                    state = State.STARTING;
                    ticker = 0;
                    scenario = startingScenarios[random.nextInt( startingScenarios.length )];
                    out( "starting" );
                }
                break;

            case STARTING:

                // if the power is off, or the fuel is off, or the starter is off and RPMs are under 50 we're stopping and switching to idle...
                if( (power == OFF) || (fuel == OFF) || ((starter == OFF) && (rpm < 1000)) ) {
                    state = State.IDLE;
                    ticker = 0;
                    scenario = this::stopRPM;
                    out( "stopping" );
                }

                // if the RPMs are 1000 or over, we're running...
                else if( rpm >= 1000) {
                    state = State.RUNNING;
                    ticker = 0;
                    scenario = runningScenarios[random.nextInt( runningScenarios.length )];
                    out( "running" );
                }
                break;

            case RUNNING:

                // if the power is off, the fuel is off, or the RPMs are under 50, we're stopping and switching to idle...
                if( (power == OFF) || (fuel == OFF) || (rpm < 50) ) {
                    state = State.IDLE;
                    ticker = 0;
                    scenario = this::stopRPM;
                    out( "stopping" );
                }

                break;
        }
    }

    /**
     * Turn the power to the engine on or off.
     *
     * @param _mode The desired power state.
     */
    @Override
    public void power( final Mode _mode ) {
        out( "power " + _mode );
        power = _mode;
        stateCheck();
    }


    /**
     * Turn the fuel valve on (open) or off (closed).
     *
     * @param _mode The desired fuel valve state.
     */
    @Override
    public void fuel( final Mode _mode ) {
        out( "fuel " + _mode );
        fuel = _mode;
        stateCheck();
    }


    /**
     * Turn the starter motor on or off.  Note that the starter motor should not be turned on for more than 15 seconds at a time, and should be
     * allowed to cool down for 15 seconds before being turned on again.
     *
     * @param _mode The desired starter motor state.
     */
    @Override
    public void starter( final Mode _mode ) {
        out( "starter " + _mode );
        starter = _mode;
        stateCheck();
    }


    /**
     * Returns the current tachometer reading in revolutions per minute (RPM).  To generate AC at 60 Hz, the engine must be turning at 1800 RPM, plus
     * or minus 0.5%.
     *
     * @return the current tachometer reading
     */
    @Override
    public double tachometer() {
        return rpm;
    }


    DoubleUnaryOperator[] startingScenarios = { this::startRamp, this::startFail, this::startExp };


    // exponential ramp...
    private double startRamp( final double _rpm ) {
        double r = Math.max( _rpm, 50 );
        return r + (1100 - r) / 12;
    }


    // failed start...
    private double startFail( final double _rpm ) {
        double r = Math.max( _rpm, 50 );
        return r + (300 - r) / 12;
    }


    // exponential ramp with a little random variation...
    private double startExp( final double _rpm ) {
        double r = Math.max( _rpm, 50 );
        return r + (1100 - r) / 12 + random.nextDouble() * (1000 - r) / 25;
    }


    DoubleUnaryOperator[] runningScenarios = { this::runLinear, this::runFail, this::runShoot };


    // exponential ramp...
    private double runLinear( final double _rpm ) {
        return _rpm + (1800 - _rpm) / 12;
    }


    // linear ramp than never makes it to 1800 RPM...
    private double runFail( final double _rpm ) {
        return _rpm + (1700 - _rpm) / 12;
    }


    // linear with overshoot, undershoot, overshoot, and then on target...
    private double runShoot( final double _rpm ) {

        // compute our target coefficient...
        double target = 1800 + 10000 * Math.sin( ticker * Math.PI / 10 ) / (5 * ticker);
        return _rpm + (target - _rpm) / 10;
    }


    // ramp the RPMs down when stopping, with a little random variation...
    private double stopRPM( final double _rpm ) {
        return Math.max( 0, _rpm - (25 + random.nextInt(100)) );
    }


    private final DateTimeFormatter ldtf = DateTimeFormatter.ofPattern( "HH:mm:ss.SSS " );

    private void out( final String _msg ) {
        System.out.println( ldtf.format( ZonedDateTime.now() )  + "      Engine: " + _msg );
    }


    private void out( final double _rpm ) {
        System.out.println( ldtf.format( ZonedDateTime.now() )  + "         RPM " + Math.round( _rpm ) );
    }
}
