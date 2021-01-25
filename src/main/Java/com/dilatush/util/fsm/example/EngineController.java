package com.dilatush.util.fsm.example;

import com.dilatush.util.Threads;
import com.dilatush.util.fsm.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.dilatush.util.fsm.example.Engine.Mode.OFF;
import static com.dilatush.util.fsm.example.Engine.Mode.ON;

/**
 * Implements a state-machine controlled controller for the propane engine on a backup generator.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class EngineController {

    private static final long   MAX_STOPPING_TIME_MS    = 10_000;
    private static final long   MAX_STARTER_TIME_MS     = 15_000;
    private static final long   MAX_STABILIZING_TIME_MS = 30_000;
    private static final long   STARTER_COOLING_TIME_MS = 15_000;
    private static final long   MIN_STABLE_TIME_MS      = 5_000;
    private static final int    MAX_START_ATTEMPTS      = 3;
    private static final double RPM_TOLERANCE_PERCENT   = 0.5;  // RPM must be +/- this percentage to generate
    private static final double MIN_GEN_RPM             = 1800 - RPM_TOLERANCE_PERCENT / 100 * 1800;
    private static final double MAX_GEN_RPM             = 1800 + RPM_TOLERANCE_PERCENT / 100 * 1800;
    private static final double STARTED_THRESHOLD_RPM   = 1000;

    private final Engine           engine;
    private final FSM<State,Event> fsm;
    private final Consumer<Report> reportConsumer;


    public EngineController( final Engine _engine, final Consumer<Report> _reportConsumer ) {

        engine = _engine;
        reportConsumer = _reportConsumer;

        fsm = createFSM();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor( new Threads.DaemonThreadFactory( "EngineSim" ) );
        executor.scheduleAtFixedRate( this::readRPM, 0, 331, TimeUnit.MILLISECONDS );
    }


    public void start() {
        fsm.onEvent( Event.START );
        out( "Controller: START command" );
    }


    public void stop() {
        fsm.onEvent( Event.STOP );
        out( "Controller: STOP command" );
    }


    public void fixed() {
        fsm.onEvent( Event.FIXED );
        out( "Controller: FIXED command" );
    }


    // read the RPMs on the engine and send it to the FSM...
    private void readRPM() {
        fsm.onEvent( new FSMEvent<>( Event.RPM, engine.tachometer() ) );
    }


    private enum State {
        STOPPED,      // engine is stopped
        STARTING,     // engine is starting
        STABILIZING,  // engine is started, stabilizing to 1800 RPM
        RUNNING,      // engine is running at 1800 RPM
        COOLING,      // engine starter motor is cooling down
        STOPPING,     // engine is stopping
        FAILED        // engine has failed
    }


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


    public enum Report {
        RUNNING,
        OVERLOADED,
        FAILED,
        STOPPED
    }


    /**
     * Create the engine controller FSM.
     *
     * @return the FSM created
     */
    private FSM<State,Event> createFSM() {

        FSMSpec<State,Event> spec = new FSMSpec<>( State.STOPPED, Event.STOP );

        spec.enableBufferedEvents();
        spec.enableEventScheduling();

        spec.setStateContext( State.STARTING,    new StartingContext()     );
        spec.setStateContext( State.STOPPING,    new StoppingContext()     );
        spec.setStateContext( State.COOLING,     new CoolingContext()      );
        spec.setStateContext( State.STABILIZING, new StabilizingContext()  );

        spec.addTransition( State.STOPPED,        Event.START,               this::actionStart,      State.STARTING    );
        spec.addTransition( State.STARTING,       Event.STOP,                this::actionStop,       State.STOPPING    );
        spec.addTransition( State.STARTING,       Event.STOP,                this::actionStop,       State.STOPPING    );
        spec.addTransition( State.STARTING,       Event.MAX_STARTER_TIME,    this::actionCool,       State.COOLING     );
        spec.addTransition( State.STARTING,       Event.RPM_OUT_OF_RANGE,    this::actionStabilize,  State.STABILIZING );
        spec.addTransition( State.COOLING,        Event.STOP,                this::actionStop,       State.STOPPING    );
        spec.addTransition( State.STABILIZING,    Event.STOP,                this::actionStop,       State.STOPPING    );
        spec.addTransition( State.STABILIZING,    Event.RPM_IN_RANGE,        this::actionInRange,    State.STABILIZING );
        spec.addTransition( State.STABILIZING,    Event.RPM_OUT_OF_RANGE,    this::actionOutOfRange, State.STABILIZING );
        spec.addTransition( State.STABILIZING,    Event.STABILIZING_TIMEOUT, this::actionUnstable,   State.FAILED      );
        spec.addTransition( State.RUNNING,        Event.STOP,                this::actionStop,       State.STOPPING    );
        spec.addTransition( State.STABILIZING,    Event.STABLE,              this::actionRunning,    State.RUNNING     );
        spec.addTransition( State.RUNNING,        Event.RPM_OUT_OF_RANGE,    this::actionOverload,   State.STOPPING    );
        spec.addTransition( State.COOLING,        Event.CANNOT_START,        this::actionNoStart,    State.FAILED      );
        spec.addTransition( State.COOLING,        Event.COOLED,              this::actionCooled,     State.STARTING    );
        spec.addTransition( State.STOPPING,       Event.RPM_0,               this::actionStopped,    State.STOPPED     );
        spec.addTransition( State.STOPPING,       Event.STOPPING_TIMEOUT,    this::actionFailed,     State.FAILED      );
        spec.addTransition( State.FAILED,         Event.FIXED,               null,                   State.STOPPED     );

        spec.addEventTransform( Event.RPM, this::rawRPM );

        return new FSM<>( spec );
    }


    private static class StartingContext extends FSMStateContext {
        private int attempts;                        // how many attempts we've made to start the engine
        private FSMCancellableEvent<Event> timeout;  // so we don't exceed 15 seconds of cranking
    }


    private static class StoppingContext extends FSMStateContext {
        private FSMCancellableEvent<Event> timeout;   // in case the engine takes too long to stop
    }


    private static class CoolingContext extends FSMStateContext {
    }


    private static class StabilizingContext extends FSMStateContext {
        private FSMCancellableEvent<Event> timeout;      // so we don't wait too long to stabilize
        private FSMCancellableEvent<Event> stableTime;   // keep track of how long we've been stable
    }


    // analyze the raw tachometer reading and emit the appropriate state machine event
    private FSMEvent<Event> rawRPM( final FSMEvent<Event> _event, final FSM<State, Event> _fsm,
                                    final Object _fsmContext, final FSMStateContext _stateContext  ) {

        double rpm = (double) _event.data;

        if( rpm == 0)
            return new FSMEvent<>( Event.RPM_0 );
        if( rpm < STARTED_THRESHOLD_RPM )
            return null;
        if( (rpm >= MIN_GEN_RPM) && (rpm <= MAX_GEN_RPM) )
            return new FSMEvent<>( Event.RPM_IN_RANGE );
        return new FSMEvent<>( Event.RPM_OUT_OF_RANGE );
    }


    private void actionStart( final FSMActionContext<State,Event> _context ) {

        // clear our attempts counter and start our cranking timer...
        StartingContext context = (StartingContext) _context.toStateContext;
        context.attempts = 0;
        context.timeout = fsm.scheduleEvent( Event.MAX_STARTER_TIME, Duration.ofMillis( MAX_STARTER_TIME_MS ) );

        // tell the engine to get its butt in gear...
        engine.power( ON );
        engine.fuel( ON );
        engine.starter( ON );

        out( "Controller: engine starting" );
    }


    private void actionCooled( final FSMActionContext<State,Event> _context ) {

        // start our cranking timer...
        StartingContext context = (StartingContext) _context.toStateContext;
        context.timeout = fsm.scheduleEvent( Event.MAX_STARTER_TIME, Duration.ofMillis( MAX_STARTER_TIME_MS ) );

        // turn the starter on...
        engine.starter( ON );

        out( "Controller: starter cooled" );
    }


    private void actionStop( final FSMActionContext<State,Event> _context ) {

        engineOff();

        // start our stopping timeout...
        StoppingContext context = (StoppingContext) _context.toStateContext;
        context.timeout = fsm.scheduleEvent( Event.STOPPING_TIMEOUT, Duration.ofMillis( MAX_STOPPING_TIME_MS ) );

        out( "Controller: engine stopping" );
    }


    private void actionStopped( final FSMActionContext<State,Event> _context ) {

        // cancel our stopping timeout...
        StoppingContext context = (StoppingContext) _context.toStateContext;
        context.timeout.cancel();

        reportConsumer.accept( Report.STOPPED );
        out( "Controller: engine stopped" );
    }


    private void actionFailed( final FSMActionContext<State,Event> _context ) {
        reportConsumer.accept( Report.FAILED );

        out( "Controller: engine failed" );
    }


    private void actionOverload( final FSMActionContext<State,Event> _context ) {
        actionStop( _context );
        reportConsumer.accept( Report.OVERLOADED );

        out( "Controller: engine overloaded" );
    }


    private void actionRunning( final FSMActionContext<State,Event> _context ) {

        // cancel our timeout...
        StabilizingContext context = (StabilizingContext) _context.fromStateContext;
        context.timeout.cancel();

        reportConsumer.accept( Report.RUNNING);

        out( "Controller: engine running" );
    }


    private void actionNoStart( final FSMActionContext<State,Event> _context ) {

        out( "Controller: couldn't start engine" );
        engineOff();
        actionFailed( _context );
    }


    private void actionUnstable( final FSMActionContext<State,Event> _context ) {

        out( "Controller: couldn't stabilize engine RPM" );
        engineOff();
        actionFailed( _context );
    }


    private void engineOff() {
        engine.starter( OFF );
        engine.fuel( OFF );
        engine.power( OFF );
    }


    private void actionStabilize( final FSMActionContext<State,Event> _context ) {

        // turn off the starter motor...
        engine.starter( OFF );

        // clear the starter timeout...
        StartingContext fmContext = (StartingContext) _context.fromStateContext;
        fmContext.timeout.cancel();

        // start our timeout...
        StabilizingContext toContext = (StabilizingContext) _context.toStateContext;
        toContext.timeout = fsm.scheduleEvent( Event.STABILIZING_TIMEOUT, Duration.ofMillis( MAX_STABILIZING_TIME_MS ) );

        out( "Controller: stabilizing engine RPM" );
    }


    private void actionInRange( final FSMActionContext<State,Event> _context ) {

        // start our timer...
        StabilizingContext context = (StabilizingContext) _context.toStateContext;
        context.stableTime = fsm.scheduleEvent( Event.STABLE, Duration.ofMillis( MIN_STABLE_TIME_MS ) );
    }


    private void actionOutOfRange( final FSMActionContext<State,Event> _context ) {

        // cancel our timer...
        StabilizingContext context = (StabilizingContext) _context.toStateContext;
        if( context.stableTime != null)
            context.stableTime.cancel();
    }


    private void actionCool( final FSMActionContext<State,Event> _context ) {

        // tell the engine starter to shut off...
        engine.starter( OFF );

        // update the number of attempts we've made...
        StartingContext fmContext = (StartingContext) _context.fromStateContext;
        fmContext.attempts++;
        boolean maxed = fmContext.attempts >= MAX_START_ATTEMPTS;

        // start our cooling timer to either try again or fail, depending on how many attempts we've made...
        fsm.scheduleEvent( maxed ? Event.CANNOT_START : Event.COOLED, Duration.ofMillis( STARTER_COOLING_TIME_MS ) );

        out( "Controller: cooling engine starter" );
    }


    private final DateTimeFormatter ldtf = DateTimeFormatter.ofPattern( "HH:mm:ss.SSS " );

    private void out( final String _msg ) {
        System.out.println( ldtf.format( ZonedDateTime.now() ) + _msg );
    }
}
