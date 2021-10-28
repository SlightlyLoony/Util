package com.dilatush.util.fsm.example;

import com.dilatush.util.fsm.FSM;
import com.dilatush.util.fsm.FSMSpec;
import com.dilatush.util.fsm.FSMTransition;
import com.dilatush.util.fsm.events.FSMEvent;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implements a state-machine controlled controller for the generator interface.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class GeneratorController {

    private final EngineController engineController;
    private final Generator        generator;
    private final FSM<State,Event> fsm;


    /**
     * Create a new instance of this class with the given generator and engine.
     *
     * @param _generator The generator to be controlled.
     * @param _engine The engine to be controlled.
     */
    public GeneratorController( final Generator _generator, final Engine _engine ) {

        engineController = new EngineController( _engine, this::engineListener );
        generator = _generator;
        fsm = createFSM();
        generator.setListener( this::generatorListener );
    }


    /**
     * The FSM states for the Generator Controller's FSM.
     */
    private enum State {
        OFF,    // generator is off; will not back up the grid
        RUN,    // generator engine is running, but will not back up the grid (exercise)
        WAIT,   // generator engine is off, grid has the load, waiting for the grid to go down
        GEN,    // generator engine is running, and has the load
        OVER,   // generator has overloaded, and engine is not running
        FAIL    // generator has failed
    }


    /**
     * The FSM events for the Generator Controller's FSM.
     */
    private enum Event {
        ON,    // user pressed on button
        OFF,   // user pressed off button
        AUTO,  // user pressed auto button
        FIX,   // technician pressed fixed button
        UP,    // grid went up
        DOWN,  // grid went down
        FAIL,  // generator failed
        RUN,   // generator is running
        OVER   // generator overloaded
    }


    /**
     * Create and return the FSM for use in this class.  This FSM is a particularly simple one, with no state in contexts or properties, and no
     * FSM event transforms - just states and event-triggered state transitions.
     *
     * @return the FSM created
     */
    private FSM<State,Event> createFSM() {

        FSMSpec<State,Event> spec = new FSMSpec<>( State.OFF, Event.OFF );

        spec.addTransition( State.OFF,     Event.ON,     this::onAction,      State.RUN    );
        spec.addTransition( State.OFF,     Event.AUTO,   null,                State.WAIT   );
        spec.addTransition( State.RUN,     Event.OFF,    this::offAction,     State.OFF    );
        spec.addTransition( State.RUN,     Event.FAIL,   this::failAction,    State.FAIL   );
        spec.addTransition( State.WAIT,    Event.ON,     this::onAction,      State.RUN    );
        spec.addTransition( State.WAIT,    Event.OFF,    null,                State.OFF    );
        spec.addTransition( State.WAIT,    Event.DOWN,   this::genAction,     State.GEN    );
        spec.addTransition( State.GEN,     Event.RUN,    this::atsOnAction,   State.GEN    );
        spec.addTransition( State.GEN,     Event.UP,     this::atsOffAction,  State.WAIT   );
        spec.addTransition( State.GEN,     Event.OFF,    this::atsOffAction,  State.OFF    );
        spec.addTransition( State.GEN,     Event.FAIL,   this::failAction,    State.FAIL   );
        spec.addTransition( State.GEN,     Event.OVER,   this::overAction,    State.OVER   );
        spec.addTransition( State.OVER,    Event.OFF,    null,                State.OFF    );
        spec.addTransition( State.FAIL,    Event.FIX,    this::fixAction,     State.OFF    );

        return new FSM<>( spec );
    }


    // on FAIL, FIX -> OFF...
    private void fixAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "fixed" );
        engineController.fixed();
        generator.failureIndicator( Generator.Mode.OFF );
    }


    // on GEN, OVER -> OVER...
    private void overAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "overload" );
        engineController.stop();
        generator.ats( Generator.Mode.OFF );
        generator.runningIndicator( Generator.Mode.OFF );
        generator.generatingIndicator( Generator.Mode.OFF );
    }


    // on GEN, UP  -> WAIT...
    // on GEN, OFF -> OFF...
    private void atsOffAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "ATS off, engine stop" );
        generator.ats( Generator.Mode.OFF );
        generator.generatingIndicator( Generator.Mode.OFF );
        generator.runningIndicator( Generator.Mode.OFF );
        engineController.stop();
    }


    // on GEN, RUN -> GEN...
    private void atsOnAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "ATS on" );
        generator.ats( Generator.Mode.ON );
        generator.generatingIndicator( Generator.Mode.ON );
    }


    // on WAIT, DOWN -> GEN...
    private void genAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "grid down" );
        generator.runningIndicator( Generator.Mode.ON );
        engineController.start();
    }


    // on GEN, FAIL -> FAIL...
    // on RUN, FAIL -> FAIL...
    private void failAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "engine fail" );
        engineController.stop();
        generator.runningIndicator( Generator.Mode.OFF );
        generator.failureIndicator( Generator.Mode.ON );
        generator.generatingIndicator( Generator.Mode.OFF );
        generator.ats( Generator.Mode.OFF );
    }


    // on RUN, OFF -> OFF...
    private void offAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "off" );
        generator.runningIndicator( Generator.Mode.OFF );
        engineController.stop();
    }


    // on OFF,  ON -> RUN...
    // on WAIT, ON -> RUN...
    private void onAction( final FSMTransition<State, Event> _transition, FSMEvent<Event> _event ) {
        out( "on" );
        generator.runningIndicator( Generator.Mode.ON );
        engineController.start();
    }


    // translate events from the generator to internal events...
    private void generatorListener( final Generator.Event _event ) {

        switch( _event ) {
            case ON    -> fsm.onEvent( Event.ON   );
            case UP    -> fsm.onEvent( Event.UP   );
            case OFF   -> fsm.onEvent( Event.OFF  );
            case AUTO  -> fsm.onEvent( Event.AUTO );
            case DOWN  -> fsm.onEvent( Event.DOWN );
            case FIXED -> fsm.onEvent( Event.FIX  );
        }
    }


    // translate events from the engine controller to internal events...
    private void engineListener( final EngineController.Report _report ) {

        switch( _report ) {

            case FAILED:
                fsm.onEvent( Event.FAIL );
                break;

            case RUNNING:
                fsm.onEvent( Event.RUN );
                break;

            case STOPPED:
                // naught to do; we just don't care...
                break;

            case OVERLOADED:
                fsm.onEvent( Event.OVER );
                break;
        }
    }


    private final DateTimeFormatter logDateTimeFormatter = DateTimeFormatter.ofPattern( "HH:mm:ss.SSS " );

    private void out( final String _msg ) {
        System.out.println( logDateTimeFormatter.format( ZonedDateTime.now() ) + "Generator: " +  _msg );
    }
}
