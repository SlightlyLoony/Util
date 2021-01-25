package com.dilatush.util.fsm;

import java.util.*;

/**
 * Instances of this class are required in order to construct an instance of {@link FSM}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMSpec<S extends Enum<S>,E extends Enum<E>> {

    private static final int DEFAULT_MAX_BUFFERED_EVENTS = 100;

    /*package-private*/ S                                                             initialState;
    /*package-private*/ FSMStateContext[]                                             stateContexts;
    /*package-private*/ Map<FSMTransitionID<S,E>, FSMTransition<FSMAction<S,E>,S,E>>  transitions;
    /*package-private*/ Map<E,FSMEventTransform<S,E>>                                 transforms;
    /*package-private*/ Object                                                        context;
    /*package-private*/ boolean                                                       eventScheduling;
    /*package-private*/ boolean                                                       bufferedEvents;
    /*package-private*/ int                                                           maxBufferedEvents = DEFAULT_MAX_BUFFERED_EVENTS;


    private final List<String>                 errorMessages;
    private final List<FSMTransitionDef<S,E>>  defs;

    private final List<E> events;
    private final List<S> states;


    public FSMSpec( final S _initialState, final E _anyEvent ) {

        initialState  = _initialState;
        transitions   = new HashMap<>();
        transforms    = new HashMap<>();
        errorMessages = new ArrayList<>();
        defs          = new ArrayList<>();

        events = getEventValues( _anyEvent );
        states = getStateValues( _initialState );

        // set up the default state contexts...
        stateContexts = new FSMStateContext[states.size()];
        for( int i = 0; i < stateContexts.length; i++ )
            stateContexts[i] = new FSMStateContext();
    }


    public void addTransition( final S _fromState, final E _event, final FSMAction<S,E> _action, final S _toState ) {

        FSMTransitionDef<S,E> def = new FSMTransitionDef<>( _fromState, _event, _action, _toState );
        defs.add( def );
    }


    public void setFSMContext( final Object _fsmContext ) {
        context = _fsmContext;
    }


    public void enableEventScheduling() {
        eventScheduling = true;
    }


    public void enableBufferedEvents() {
        bufferedEvents = true;
    }


    public void setMaxBufferedEvents( final int _max ) {
        maxBufferedEvents = _max;
    }


    public void setStateContext( final S _state, final FSMStateContext _context ) {
        stateContexts[_state.ordinal()] = _context;
    }


    public void addEventTransform( final E _event, final FSMEventTransform<S,E> _transform ) {
        transforms.put( _event, _transform );
    }


    public boolean isValid() {

        // iterate over all the transition definitions to map all our transitions, and collecting a concrete event...
        for( FSMTransitionDef<S,E> def : defs ) {
            FSMTransitionID<S,E> index = new FSMTransitionID<>( def.fromState, def.onEvent );
            transitions.put( index, new FSMTransition<FSMAction<S,E>,S,E>(def.action, def.toState) );
        }

        boolean valid = true;
// TODO: we need to check for duplicates...
        // see what states we have no transitions from...
        Set<S> fromStates = new HashSet<>( states );
        for( FSMTransitionID<S,E> def : transitions.keySet() ) {
            fromStates.remove( def.fromState );
        }
        for( S state : fromStates ) {
            valid = false;
            errorMessages.add( "   No transition from state: " + state.toString() );
        }

        // see what states we have no transitions to...
        Set<S> toStates = new HashSet<>( states );
        for( FSMTransition<FSMAction<S,E>,S,E> transition : transitions.values() ) {
            toStates.remove( transition.toState );
        }
        for( S state : toStates ) {
            valid = false;
            errorMessages.add( "   No transition to state: " + state.toString() );
        }

        // see what events we don't use...
        Set<E> usedEvents = new HashSet<>( events );
        for( FSMTransitionID<S,E> def : transitions.keySet() ) {
            usedEvents.remove( def.event );
        }
        for( E event : transforms.keySet() ) {
            usedEvents.remove( event );
        }
        for( E event : usedEvents ) {
            valid = false;
            errorMessages.add( "   Event never used: " + event.toString() );
        }

        return valid;
    }


    @SuppressWarnings( "unchecked" )
    private List<S> getStateValues( final S _state ) {
        return Arrays.asList( ((Class<S>) _state.getClass()).getEnumConstants() );
    }


    @SuppressWarnings( "unchecked" )
    private List<E> getEventValues( final E _event ) {
        return Arrays.asList( ((Class<E>) _event.getClass()).getEnumConstants() );
    }


    public String getErrorMessage() {
        return String.join( "\n", errorMessages );
    }


    /**
     * @author Tom Dilatush  tom@dilatush.com
     */
    public static class FSMTransitionDef<S extends Enum<S>, E extends Enum<E>> {

        public final S fromState;
        public final E onEvent;
        public final FSMAction<S,E> action;
        public final S toState;


        public FSMTransitionDef( final S _fromState, final E _onEvent, final FSMAction<S,E> _action, final S _toState ) {
            fromState = _fromState;
            onEvent = _onEvent;
            action = _action;
            toState = _toState;
        }
    }
}
