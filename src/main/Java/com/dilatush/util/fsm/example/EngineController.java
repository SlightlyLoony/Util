package com.dilatush.util.fsm.example;

import com.dilatush.util.Threads;
import com.dilatush.util.fsm.*;
import com.dilatush.util.fsm.events.FSMEvent;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.fsm.example.Engine.Mode.OFF;
import static com.dilatush.util.fsm.example.Engine.Mode.ON;

/**
 * Implements an example usage of the {@link FSM} finite state machine: a state-machine controlled controller for the propane engine on a home backup
 * generator.  While this isn't a real-world example, it is actually quite close to what you would find on a propane-powered Generac backup
 * generator.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class EngineController {

    private static final long   MAX_STOPPING_TIME_MS    = 10_000;
    private static final long   MAX_STARTER_TIME_MS     = 15_000;
    private static final long   MAX_STABILIZING_TIME_MS = 30_000;
    private static final long   STARTER_COOLING_TIME_MS = 15_000;
    private static final long   MIN_STABLE_TIME_MS      = 10_000;
    private static final int    MAX_START_ATTEMPTS      = 3;
    private static final double RPM_TOLERANCE_PERCENT   = 0.5;  // RPM must be +/- this percentage to generate
    private static final double MIN_GEN_RPM             = 1800 - RPM_TOLERANCE_PERCENT / 100 * 1800;
    private static final double MAX_GEN_RPM             = 1800 + RPM_TOLERANCE_PERCENT / 100 * 1800;
    private static final double STARTED_THRESHOLD_RPM   = 1000;

    private final Engine           engine;
    private final FSM<State,Event> fsm;
    private final Consumer<Report> eventListener;

    // this is an example of keeping FSM state in a field of the class containing the FSM...
    private int engineStartAttempts;


    /**
     * Create a new instance of this class with the given {@link Engine} and event listener.
     *
     * @param _engine The engine being controlled.
     * @param _eventListener The listener receiving reports (events) from this controller.
     */
    public EngineController( final Engine _engine, final Consumer<Report> _eventListener ) {

        // fail fast if we're missing arguments...
        if( isNull( _engine, _eventListener ) )
            throw new IllegalArgumentException( "Missing the engine or event listener" );

        engine = _engine;
        eventListener = _eventListener;

        fsm = createFSM();

        // set up a scheduled executor to read the engine RPMs about 3 times a second...
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor( new Threads.DaemonThreadFactory( "EngineSim" ) );
        executor.scheduleAtFixedRate( this::readRPM, 0, 331, TimeUnit.MILLISECONDS );
    }


    /**
     * The FSM states for this engine controller.
     */
    private enum State {
        STOPPED,      // engine is stopped
        STARTING,     // engine is starting
        STABILIZING,  // engine is started, stabilizing to 1800 RPM
        RUNNING,      // engine is running at 1800 RPM
        COOLING,      // engine starter motor is cooling down
        STOPPING,     // engine is stopping
        FAILED        // engine has failed
    }


    /**
     * The FSM events for this engine controller.
     */
    private enum Event {
        START,                // command: start the engine
        STOP,                 // command: stop the engine
        FIXED,                // command: engine fixed
        STOPPING_TIMEOUT,     // stopping took too long
        STABILIZING_TIMEOUT,  // stabilizing took too long
        RPM,                  // raw RPM reading
        RPM_0,                // the RPMs have reached zero
        CANNOT_START,         // engine failed to start after several tries
        MAX_STARTER_TIME,     // the starter motor has reached maximum cranking time; it's hot
        COOLED,               // the starter motor has cooled
        RPM_OUT_OF_RANGE,     // the RPMs are out of the target range
        RPM_IN_RANGE,         // the RPMs are in the target range
        STABLE                // the RPMs have been in the target range long enough
    }


    /**
     * The reports (events) sent to the event listener.
     */
    public enum Report {
        RUNNING,
        OVERLOADED,
        FAILED,
        STOPPED
    }


    /**
     * An example of an FSM global context, used here to store a cancellable timeout.
     */
    private static class GlobalContext {
        private FSMEvent<Event> timeout;  // so we don't exceed 15 seconds of cranking
    }


    /**
     * An example of an FSM state context, used here to store a cancellable timeout.
     */
    private static class StoppingContext {
        private FSMEvent<Event> timeout;   // in case the engine takes too long to stop
    }


    /**
     * Create and return the engine controller FSM.
     *
     * @return the FSM created
     */
    private FSM<State,Event> createFSM() {

        // create our FSM specification with the initial state and an example event...
        FSMSpec<State, Event> spec = new FSMSpec<>( State.STOPPED, Event.STOP );

        // we want all the good and fancy stuff...
        spec.enableBufferedEvents();
        spec.enableEventScheduling();

        // set an example of an FSM global context...
        spec.setFSMContext( new GlobalContext() );

        // set an example of an FSM state context...
        spec.setStateContext( State.STOPPING, new StoppingContext() );

        // set a couple examples of an FSM event action...
        spec.setEventAction( Event.RPM_IN_RANGE, this::onEvent );
        spec.setEventAction( Event.RPM_OUT_OF_RANGE, this::onEvent );

        // set an example of an on-entry state action...
        spec.setStateOnEntryAction( State.STOPPING, this::onEntryStopping );

        // add all the FSM state transitions for our FSM...
        spec.addTransition( State.STOPPED,        Event.START,               this::actionStart,      State.STARTING    );
        spec.addTransition( State.STARTING,       Event.STOP,                null,                   State.STOPPING    );
        spec.addTransition( State.STARTING,       Event.MAX_STARTER_TIME,    this::actionCool,       State.COOLING     );
        spec.addTransition( State.STARTING,       Event.RPM_OUT_OF_RANGE,    this::actionStabilize,  State.STABILIZING );
        spec.addTransition( State.COOLING,        Event.STOP,                null,                   State.STOPPING    );
        spec.addTransition( State.STABILIZING,    Event.STOP,                null,                   State.STOPPING    );
        spec.addTransition( State.STABILIZING,    Event.RPM_IN_RANGE,        this::actionInRange,    State.STABILIZING );
        spec.addTransition( State.STABILIZING,    Event.RPM_OUT_OF_RANGE,    this::actionOutOfRange, State.STABILIZING );
        spec.addTransition( State.STABILIZING,    Event.STABILIZING_TIMEOUT, this::actionUnstable,   State.FAILED      );
        spec.addTransition( State.RUNNING,        Event.STOP,                null,                   State.STOPPING    );
        spec.addTransition( State.STABILIZING,    Event.STABLE,              this::actionRunning,    State.RUNNING     );
        spec.addTransition( State.RUNNING,        Event.RPM_OUT_OF_RANGE,    this::actionOverload,   State.STOPPING    );
        spec.addTransition( State.COOLING,        Event.CANNOT_START,        this::actionNoStart,    State.FAILED      );
        spec.addTransition( State.COOLING,        Event.COOLED,              this::actionCooled,     State.STARTING    );
        spec.addTransition( State.STOPPING,       Event.RPM_0,               this::actionStopped,    State.STOPPED     );
        spec.addTransition( State.STOPPING,       Event.STOPPING_TIMEOUT,    this::actionFailed,     State.FAILED      );
        spec.addTransition( State.FAILED,         Event.FIXED,               null,                   State.STOPPED     );

        // add an example of an FSM event transform...
        spec.addEventTransform( Event.RPM, this::rawRPM );

        // we're done with the spec, so use it to create the actual FSM and return it...
        return new FSM<>( spec );
    }


    /**
     * This example of an event action just prints out the event and state.
     *
     * @param _event The FSM event.
     * @param _state The FSM state that the event occurred in.
     */
    private void onEvent( final FSMEvent<Event> _event, final FSMState<State,Event> _state ) {
        out( "On event " + _event.event + " while in state " + _state.state );
    }


    /**
     * This example of an FSM "event transform" transforms a raw event containing the engine RPM into one of three discrete FSM events, or returns
     * a {@code null} when the RPMs are in a range that can't trigger any transitions.
     *
     * @param _event The FSM event being transformed, in this case, always an RPM event (with RPMs as the data).
     * @param _fsm The FSM associated with this transformation.
     * @return the transformed event, or {@code null} if none
     */
    private FSMEvent<Event> rawRPM( final FSMEvent<Event> _event, final FSM<State, Event> _fsm  ) {

        // we know the data is a double...
        double rpm = (double) _event.getData();

        // if the RPMs are zero, transform to the RPM_0 event...
        if( rpm == 0)
            return fsm.event( Event.RPM_0 );

        // if the RPMs are less than our "engine started" threshold, just return a null...
        if( rpm < STARTED_THRESHOLD_RPM )
            return null;

        // otherwise, return either RPM_IN_RANGE or RPM_OUT_OF_RANGE events ...
        return ( (rpm >= MIN_GEN_RPM) && (rpm <= MAX_GEN_RPM) )
                ? fsm.event( Event.RPM_IN_RANGE )
                : fsm.event( Event.RPM_OUT_OF_RANGE );
    }


    // on entry to STOPPING...
    private void onEntryStopping( final FSMState<State,Event> _state ) {
        out( "engine stopping" );
        engineOff();
        StoppingContext context = (StoppingContext) _state.context;
        context.timeout = fsm.scheduleEvent( Event.STOPPING_TIMEOUT, Duration.ofMillis( MAX_STOPPING_TIME_MS ) );
        GlobalContext globalContext = (GlobalContext) _state.fsmContext;
        globalContext.timeout.cancel();
    }


    // on STOPPED, START -> STARTING...
    private void actionStart( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "engine starting" );
        GlobalContext context = (GlobalContext) _transition.fsmContext;
        engineStartAttempts = 0;
        context.timeout = fsm.scheduleEvent( Event.MAX_STARTER_TIME, Duration.ofMillis( MAX_STARTER_TIME_MS ) );
        engine.power( ON );
        engine.fuel( ON );
        engine.starter( ON );
    }


    // on COOLING, COOLED -> STARTING...
    private void actionCooled( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "starter cooled" );
        GlobalContext context = (GlobalContext) _transition.fsmContext;
        context.timeout = fsm.scheduleEvent( Event.MAX_STARTER_TIME, Duration.ofMillis( MAX_STARTER_TIME_MS ) );
        engine.starter( ON );
    }


    // on STOPPING, RPM_0 -> STOPPED...
    private void actionStopped( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "engine stopped" );
        StoppingContext context = (StoppingContext) _transition.fromState.context;
        context.timeout.cancel();
        eventListener.accept( Report.STOPPED );
    }


    // on STOPPING, STOPPING_TIMEOUT -> FAILED...
    private void actionFailed( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "engine failed" );
        eventListener.accept( Report.FAILED );
    }


    // on RUNNING, RPM_OUT_OF_RANGE -> STOPPING...
    private void actionOverload( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "engine overloaded" );
        engineOff();
        eventListener.accept( Report.OVERLOADED );
    }


    // on STABILIZING, STABLE -> RUNNING...
    private void actionRunning( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "engine running" );
        eventListener.accept( Report.RUNNING);
    }


    // on COOLING, CANNOT_START -> FAILED...
    private void actionNoStart( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "couldn't start engine" );
        engineOff();
        actionFailed( _transition, _event );
    }


    // on STABILIZING, STABILIZING_TIMEOUT -> FAILED...
    private void actionUnstable( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "couldn't stabilize engine RPM" );
        engineOff();
        actionFailed( _transition, _event );
    }


    // one STARTING, RPM_OUT_OF_RANGE -> STABILIZING...
    private void actionStabilize( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "stabilizing engine RPM" );
        engine.starter( OFF );
        GlobalContext fmContext = (GlobalContext) _transition.fsmContext;
        fmContext.timeout.cancel();
        _transition.setTimeout( Event.STABILIZING_TIMEOUT, Duration.ofMillis( MAX_STABILIZING_TIME_MS ) );
    }


    // on STABILIZING, RPM_IN_RANGE -> STABILIZING...
    private void actionInRange( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        _transition.toState.setProperty( "StableTime", fsm.scheduleEvent( Event.STABLE, Duration.ofMillis( MIN_STABLE_TIME_MS ) ) );
    }


    // on STABILIZING, RPM_OUT_OF_RANGE -> STABILIZING
    private void actionOutOfRange( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        FSMEvent<?> stableTime = (FSMEvent<?>) _transition.toState.getProperty( "StableTime" );
        if( stableTime != null)
            stableTime.cancel();
    }


    // on STARTING, MAX_STARTER_TIME -> COOLING
    private void actionCool( final FSMTransition<State,Event> _transition, FSMEvent<Event> _event ) {
        out( "cooling engine starter" );
        engine.starter( OFF );
        engineStartAttempts++;
        boolean maxed = engineStartAttempts >= MAX_START_ATTEMPTS;
        fsm.scheduleEvent( maxed ? Event.CANNOT_START : Event.COOLED, Duration.ofMillis( STARTER_COOLING_TIME_MS ) );
    }


    // turn everything in the engine off...
    private void engineOff() {
        engine.starter( OFF );
        engine.fuel( OFF );
        engine.power( OFF );
    }


    /**
     * Start the engine.
     */
    public void start() {
        fsm.onEvent( Event.START );
        out( "START command" );
    }


    /**
     * Stop the engine.
     */
    public void stop() {
        fsm.onEvent( Event.STOP );
        out( "STOP command" );
    }


    /**
     * Assert that the engine has been fixed (after a failure).
     */
    public void fixed() {
        fsm.onEvent( Event.FIXED );
        out( "FIXED command" );
    }


    /**
     * Read the RPMs on the engine and send it to the FSM.
     */
    private void readRPM() {
        fsm.onEvent( Event.RPM, engine.tachometer()  );
    }


    private final DateTimeFormatter logStampFormatter = DateTimeFormatter.ofPattern( "HH:mm:ss.SSS " );

    private void out( final String _msg ) {
        System.out.println( logStampFormatter.format( ZonedDateTime.now() ) + "   Controller: " +  _msg );
    }
}
