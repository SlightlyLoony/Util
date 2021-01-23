package com.dilatush.util.fsm;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMActionContext<S extends Enum<S>, E extends Enum<E>> {

    public final S           fromState;
    public final S           toState;
    public final FSMEvent<E> event;
    public final Object      fromStateContext;
    public final Object      toStateContext;
    public final Object      fsmContext;
    public final FSM<S,E>    fsm;


    public FSMActionContext( final S _fromState, final S _toState, final FSMEvent<E> _event,
                             final Object _fromStateContext, final Object _toStateContext, final Object _fsmContext,
                             final FSM<S,E> _fsm ) {

        fromState = _fromState;
        toState = _toState;
        event = _event;
        fromStateContext = _fromStateContext;
        toStateContext = _toStateContext;
        fsmContext = _fsmContext;
        fsm = _fsm;
    }
}
