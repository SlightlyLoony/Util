package com.dilatush.util.fsm.test;

import com.dilatush.util.fsm.*;

import java.time.Duration;
import java.util.logging.Logger;

import static com.dilatush.util.fsm.test.TestFSM.HeaterEvent.*;
import static com.dilatush.util.fsm.test.TestFSM.HeaterState.*;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestFSM {

    private static Logger LOGGER;

    public static void main( final String[] _args ) throws InterruptedException {

        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );
        LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getSimpleName() );

        LOGGER.info( () -> "Test starting" );

        FSMSpec<HeaterState,HeaterEvent> spec = new FSMSpec<>( OFF, FUELED );

        spec.addTransition( OFF,         FEELS_COLD,    TestFSM::heaterOn,         ON          );
        spec.addTransition( OFF,         FEELS_COLD,    TestFSM::heaterOn,         ON          );
        spec.addTransition( OFF,         TEMP_LOW,      TestFSM::heaterOn,         ON          );
        spec.addTransition( ON,          FEELS_HOT,     TestFSM::heaterOff,        OFF         );
        spec.addTransition( ON,          TEMP_HIGH,     TestFSM::heaterOff,        OFF         );
        spec.addTransition( ON,          BROKE,         TestFSM::heaterBroke,      BROKEN      );
        spec.addTransition( ON,          FUEL_RUNS_OUT, TestFSM::heaterOutOfFuel,  OUT_OF_FUEL );
        spec.addTransition( BROKEN,      FIXED,         TestFSM::heaterFixed,      ON          );
        spec.addTransition( OUT_OF_FUEL, FUELED,        TestFSM::heaterFueled,     ON          );

        spec.enableBufferedEvents();
        spec.enableEventScheduling();

        FSM<HeaterState, HeaterEvent> fsm = new FSM<>( spec );

        fsm.onEvent( FEELS_COLD );
        fsm.onEvent( BROKE );
        fsm.onEvent( FUEL_RUNS_OUT );
        fsm.onEvent( FIXED );
        fsm.onEvent( FEELS_HOT );
        fsm.onEvent( TEMP_LOW );
        fsm.onEvent( FUEL_RUNS_OUT );
        fsm.onEvent( FEELS_COLD );
        fsm.onEvent( FUELED );
        fsm.onEvent( FEELS_HOT );
        fsm.onEvent( TEMP_HIGH );


        Thread.sleep( 2000 );
    }


    private static void heaterOn( FSMActionContext<HeaterState,HeaterEvent> _context ) {
        _context.fsm.scheduleEvent( TEMP_LOW, Duration.ofMillis( 500 ) );
        System.out.println( "Heater on" );
    }


    private static void heaterOff( FSMActionContext<HeaterState,HeaterEvent> _context ) {
        _context.fsm.scheduleEvent( FEELS_HOT, Duration.ofMillis( 1000 ) );
        System.out.println( "Heater off" );
    }


    private static void heaterBroke( FSMActionContext<HeaterState,HeaterEvent> _context ) {
        System.out.println( "Heater broke, is off" );
    }


    private static void heaterOutOfFuel( FSMActionContext<HeaterState,HeaterEvent> _context ) {
        System.out.println( "Heater out of fuel, is off" );
    }


    private static void heaterFixed( FSMActionContext<HeaterState,HeaterEvent> _context ) {
        System.out.println( "Heater fixed, is on" );
    }


    private static void heaterFueled( FSMActionContext<HeaterState,HeaterEvent> _context ) {
        System.out.println( "Heater fueled, is on" );
    }


    public enum HeaterEvent {
        FEELS_COLD,
        FEELS_HOT,
        TEMP_LOW,
        TEMP_HIGH,
        BROKE,
        FIXED,
        FUEL_RUNS_OUT,
        FUELED
    }

    public enum HeaterState {
        ON,
        OFF,
        BROKEN,
        OUT_OF_FUEL
    }
}
