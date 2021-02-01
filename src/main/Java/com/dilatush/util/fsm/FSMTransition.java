package com.dilatush.util.fsm;

import java.time.Duration;

/**
 * Instances of this class define an FSM state transition, from one state to another, triggered by a specific FSM event.  The FSM transition may
 * include an optional transition action.  Note that it is possible to define an FSM state transition where the initial state (the "from" state) and
 * the ending state (the "to" state) are the same.  In this case the FSM state transition really isn't a transition at all, but more like an
 * "on event" action.  All the fields of this class are final and public.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public final class FSMTransition<S extends Enum<S>,E extends Enum<E>> {

    /**
     * The {@link FSM} instance associated with this transition.
     */
    public final FSM<S,E>                 fsm;


    /**
     * The optional FSM global context.
     */
    public final Object                   fsmContext;


    /**
     * The {@link FSMState} instance for the FSM state we're transitioning away from.
     */
    public final FSMState<S,E>            fromState;


    /**
     * The enum for the FSM event that triggered this transition.
     */
    public final E                        event;


    /**
     * The optional {@link FSMTransitionAction} associated with this transition.
     */
    public final FSMTransitionAction<S,E> action;


    /**
     * The {@link FSMState} instance for the FSM state we're transitioning to.
     */
    public final FSMState<S,E>            toState;


    /**
     * Creates a new instance of this class with the given values.
     *
     * @param _fsm The {@link FSM} associated with this transition.
     * @param _fsmContext The FSM global context from the FSM associated with this transition.
     * @param _fromState The {@link FSMState} this transition is moving from.
     * @param _event The enum of the {@link FSMEvent} that triggered this transition.
     * @param _action The optional {@link FSMTransitionAction} to be run on this transition.
     * @param _toState The {@link FSMState} this transition is moving to.
     */
    public FSMTransition( final FSM<S, E> _fsm, final Object _fsmContext, final FSMState<S, E> _fromState, final E _event,
                          final FSMTransitionAction<S, E> _action, final FSMState<S, E> _toState ) {
        fsm = _fsm;
        fsmContext = _fsmContext;
        fromState = _fromState;
        event = _event;
        action = _action;
        toState = _toState;
    }


    /**
     * Sets a timeout on the state being transitioned to (the "to" state).  The timeout is simply an event scheduled for the timeout delay that is
     * automatically cancelled if a transition <i>from</i>the "to" state occurs before the scheduled time.  By putting this method in the transition,
     * the FSM can enforce a timeout being set <i>only</i> on the state being transitioned to.  This restriction allows the FSM to ensure
     * that the cancellation logic works correctly.  Event scheduling must be enabled in the FSM if timeouts will be used.  If this method is called
     * and event scheduling is <i>not</i> enabled in the FSM, an {@link UnsupportedOperationException} will be thrown.
     *
     * @param _event The {@link FSMEvent} to occur when the timeout expires.
     * @param _delay The delay (as a {@link Duration} instance) for the timeout.
     */
    public void setTimeout( final FSMEvent<E> _event, final Duration _delay ) {

        // schedule the event, and store the resulting cancellable event in the "to" state...
        toState.setTimeout( fsm.scheduleEvent( _event, _delay ) );
    }



    /**
     * Sets a timeout on the state being transitioned to (the "to" state).  The timeout is simply an event scheduled for the timeout delay that is
     * automatically cancelled if a transition <i>from</i>the "to" state occurs before the scheduled time.  By putting this method in the transition,
     * the FSM can enforce a timeout being set <i>only</i> on the state being transitioned to.  This restriction allows the FSM to ensure
     * that the cancellation logic works correctly.  Event scheduling must be enabled in the FSM if timeouts will be used.  If this method is called
     * and event scheduling is <i>not</i> enabled in the FSM, an {@link UnsupportedOperationException} will be thrown.
     *
     * @param _event The enum for the FSM event to occur when the timeout expires.
     * @param _eventData The optional (may be {@code null}) data associated with the FSM event.
     * @param _delay The delay (as a {@link Duration} instance) for the timeout.
     */
    @SuppressWarnings( "unused" )
    public void setTimeout( final E _event, final Object _eventData, final Duration _delay ) {
        setTimeout( new FSMEvent<>( _event, _eventData ), _delay );
    }



    /**
     * Sets a timeout on the state being transitioned to (the "to" state).  The timeout is simply an event scheduled for the timeout delay that is
     * automatically cancelled if a transition <i>from</i>the "to" state occurs before the scheduled time.  By putting this method in the transition,
     * the FSM can enforce a timeout being set <i>only</i> on the state being transitioned to.  This restriction allows the FSM to ensure
     * that the cancellation logic works correctly.  Event scheduling must be enabled in the FSM if timeouts will be used.  If this method is called
     * and event scheduling is <i>not</i> enabled in the FSM, an {@link UnsupportedOperationException} will be thrown.
     *
     * @param _event The enum for the FSM event to occur when the timeout expires.
     * @param _delay The delay (as a {@link Duration} instance) for the timeout.
     */
    @SuppressWarnings( "unused" )
    public void setTimeout( final E _event, final Duration _delay ) {
        setTimeout( new FSMEvent<>( _event ), _delay );
    }
}
