package com.dilatush.util.fsm;

/**
 * A simple POJO that contains the {@link FSMAction} and FSM state after the FSM state transition (the "to" state).  It is used internally by
 * {@link FSM} and {@link FSMSpec}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/*package-private*/ class FSMTransition<A extends FSMAction<S,E>, S extends Enum<S>, E extends Enum<E>> {


    /*package-private*/ final S toState;
    /*package-private*/ final A action;


    /**
     * Creates a new instance of this class with the given FSM action and FSM state.
     *
     * @param _action The FSM action for this FSM transition.
     * @param _toState The FSM state for this FSM transition.
     */
    /*package-private*/ FSMTransition( final A _action, final S _toState ) {
        toState = _toState;
        action  = _action;
    }
}
