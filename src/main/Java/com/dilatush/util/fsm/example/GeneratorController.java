package com.dilatush.util.fsm.example;

import com.dilatush.util.fsm.FSM;
import com.dilatush.util.fsm.FSMActionContext;
import com.dilatush.util.fsm.FSMSpec;

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


    public GeneratorController( final Generator _generator, final Engine _engine ) {


        engineController = new EngineController( _engine, this::engineListener );
        generator = _generator;
        fsm = createFSM();
        generator.setListener( this::generatorListener );
    }


    private enum State {
        OFF,    // generator is off; will not back up the grid
        RUN,    // generator engine is running, but will not back up the grid (exercise)
        WAIT,   // generator engine is off, grid has the load, waiting for the grid to go down
        GEN,    // generator engine is running, and has the load
        OVER,   // generator has overloaded, and engine is not running
        FAIL    // generator has failed
    }


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


    private FSM<State,Event> createFSM() {

        FSMSpec<State,Event> spec = new FSMSpec<>( State.OFF, Event.OFF );

        spec.enableBufferedEvents();
        spec.enableEventScheduling();

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
        spec.addTransition( State.FAIL,    Event.FIX,    null,                State.OFF    );

        return new FSM<>( spec );
    }


    private void overAction( final FSMActionContext<State, Event> _context ) {
        out( "overload" );
        engineController.stop();
        generator.ats( Generator.Mode.OFF );
        generator.runningIndicator( Generator.Mode.OFF );
        generator.generatingIndicator( Generator.Mode.OFF );
    }


    private void atsOffAction( final FSMActionContext<State, Event> _context ) {
        out( "ATS off, engine stop" );
        generator.ats( Generator.Mode.OFF );
        generator.generatingIndicator( Generator.Mode.OFF );
        generator.runningIndicator( Generator.Mode.OFF );
        engineController.stop();
    }


    private void atsOnAction( final FSMActionContext<State, Event> _context ) {
        out( "ATS on" );
        generator.ats( Generator.Mode.ON );
        generator.generatingIndicator( Generator.Mode.ON );
    }


    private void genAction( final FSMActionContext<State, Event> _context ) {
        out( "grid down" );
        generator.runningIndicator( Generator.Mode.ON );
        engineController.start();
    }


    private void failAction( final FSMActionContext<State, Event> _context ) {
        out( "engine fail" );
        engineController.stop();
        generator.runningIndicator( Generator.Mode.OFF );
        generator.failureIndicator( Generator.Mode.ON );
        generator.generatingIndicator( Generator.Mode.OFF );
        generator.ats( Generator.Mode.OFF );
    }


    private void offAction( final FSMActionContext<State, Event> _context ) {
        out( "off" );
        generator.runningIndicator( Generator.Mode.OFF );
        engineController.stop();
    }


    private void onAction( final FSMActionContext<State, Event> _context ) {
        out( "on" );
        generator.runningIndicator( Generator.Mode.ON );
        engineController.start();
    }


    private void generatorListener( final Generator.Event _event ) {

        switch( _event ) {

            case ON:
                fsm.onEvent( Event.ON );
                break;

            case UP:
                fsm.onEvent( Event.UP );
                break;

            case OFF:
                fsm.onEvent( Event.OFF );
                break;

            case AUTO:
                fsm.onEvent( Event.AUTO );
                break;

            case DOWN:
                fsm.onEvent( Event.DOWN );
                break;

            case FIXED:
                fsm.onEvent( Event.FIX );
                break;
        }
    }


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


    private final DateTimeFormatter ldtf = DateTimeFormatter.ofPattern( "HH:mm:ss.SSS " );

    private void out( final String _msg ) {
        System.out.println( ldtf.format( ZonedDateTime.now() ) + "Generator: " +  _msg );
    }
}
