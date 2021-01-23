package com.dilatush.util.fsm;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMTransition<A extends FSMAction<S,E>, S extends Enum<S>, E extends Enum<E>> {

    public final S toState;
    public final A action;

    public FSMTransition( final A _action, final S _toState ) {
        toState = _toState;
        action  = _action;
    }
}
