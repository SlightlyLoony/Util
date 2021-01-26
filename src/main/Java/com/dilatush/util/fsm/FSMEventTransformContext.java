package com.dilatush.util.fsm;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMEventTransformContext<S extends Enum<S>, E extends Enum<E>> {

    final FSM<S,E>        fsm;            // the finite state machine (FSM)
    final Object          fsmContext;     // the FSM global context
    final FSMStateContext stateContext;   // the state context for the current state


    public FSMEventTransformContext( final FSM<S, E> _fsm, final Object _fsmContext, final FSMStateContext _stateContext ) {
        fsm = _fsm;
        fsmContext = _fsmContext;
        stateContext = _stateContext;
    }
}
