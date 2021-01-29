package com.dilatush.util.fsm;

import java.time.Duration;

/**
 * The context for an FSM action, which is mostly a collection of potentially useful information in a form that is easily accessible to the action.
 * This class does include a few important methods, however: timeout setters.  These setters are in this class in order to enforce the notion that
 * timeouts are set on states that are being transitioned <i>to</i>.  This ensures that the timeout cancellation logic in the FSM works correctly.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMActionContext<S extends Enum<S>, E extends Enum<E>> {


    /**
     * The FSM state being transitioned from.  This is mostly useful for actions that are associated with multiple transitions, whose behavior needs
     * to change depending on what this state is.
     */
    public final S                fromState;


    /**
     * The FSM state being transitioned to.  This is mostly useful for actions that are associated with multiple transitions, whose behavior needs
     * to change depending on what this state is.
     */
    public final S                toState;


    /**
     * The FSM event that triggered this action.  This is mostly useful for actions that are associated with multiple transitions, whose behavior
     * needs to change depending on what this event is.
     */
    public final FSMEvent<E>      event;


    /**
     * The context object associated with the FSM state being transitioned from.  By default this is an instance of {@link FSMStateContext}, but most
     * often it is a subclass that contains fields specific to the state.
     */
    public final FSMStateContext fromStateContext;


    /**
     * The context object associated with the FSM state being transitioned to.  By default this is an instance of {@link FSMStateContext}, but most
     * often it is a subclass that contains fields specific to the state.
     */
    public final FSMStateContext toStateContext;


    /**
     * The context object associated with the FSM globally, not any particular state or event.  This object is optional, and its type is arbitrary,
     * determined by the program using the FSM.  It must be cast to the agreed type before it can be used.
     */
    public final Object          fsmContext;


    /**
     * The FSM instance associated with this instance.  The FSM is included to give the action access to its public methods for event posting and
     * scheduling.
     */
    public final FSM<S,E>        fsm;


    /**
     * Create a new instance of this class with the given values.
     *
     * @param _fromState The FSM state being transitioned from.
     * @param _toState The FSM state being transitioned to.
     * @param _event The FSM event that triggered this action.
     * @param _fromStateContext The {@link FSMStateContext} for the state being transitioned from.
     * @param _toStateContext The {@link FSMStateContext} for the state being transitioned to.
     * @param _fsmContext The FSM global context.
     * @param _fsm The FSM this action context is associated with.
     */
    public FSMActionContext( final S _fromState, final S _toState, final FSMEvent<E> _event,
                             final FSMStateContext _fromStateContext, final FSMStateContext _toStateContext, final Object _fsmContext,
                             final FSM<S,E> _fsm ) {

        fromState = _fromState;
        toState = _toState;
        event = _event;
        fromStateContext = _fromStateContext;
        toStateContext = _toStateContext;
        fsmContext = _fsmContext;
        fsm = _fsm;
    }


    /**
     * Sets a timeout on the state being transitioned to (the "to" state).  The timeout is simply an event scheduled for the timeout delay that is
     * automatically cancelled if a transition <i>from</i>the "to" state occurs before the scheduled time.  By putting this method in the action
     * context, the FSM can enforce a timeout being set <i>only</i> on the state being transitioned to.  This restriction allows the FSM to ensure
     * that the cancellation logic works correctly.  Event scheduling must be enabled in the FSM if timeouts will be used.  If this method is called
     * and event scheduling is <i>not</i> enabled in the FSM, an {@link UnsupportedOperationException} will be thrown.
     *
     * @param _event The FSM event to occur when the timeout expires.
     * @param _delay The delay (as a {@link Duration} instance) for the timeout.
     */
    public void setTimeout( final FSMEvent<E> _event, final Duration _delay ) {

        // schedule the event, and store the resulting cancellable event in the "to" state context...
        toStateContext.setTimeout( fsm.scheduleEvent( _event, _delay ) );
    }



    /**
     * This is a convenience method that simply wraps the given event {@link Enum} and event data {@link Object} in an instance of
     * {@link FSMEvent} and calls {@link #setTimeout(FSMEvent, Duration)}.
     *
     * @param _event The event enum for the event to be scheduled.
     * @param _eventData The event data object for the event to be scheduled.
     * @param _delay The delay until it is to be handled.
     */
    @SuppressWarnings( "unused" )
    public void setTimeout( final E _event, final Object _eventData, final Duration _delay ) {
        setTimeout( new FSMEvent<>( _event, _eventData ), _delay );
    }



    /**
     * This is a convenience method that simply wraps the given event {@link Enum} in an instance of
     * {@link FSMEvent} and calls {@link #setTimeout(FSMEvent, Duration)}.
     *
     * @param _event The event enum for the event to be scheduled.
     * @param _delay The delay until it is to be handled.
     */
    @SuppressWarnings( "unused" )
    public void setTimeout( final E _event, final Duration _delay ) {
        setTimeout( new FSMEvent<>( _event ), _delay );
    }
}
