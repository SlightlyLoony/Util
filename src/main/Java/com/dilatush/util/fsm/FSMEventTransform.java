package com.dilatush.util.fsm;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMEventTransform<S extends Enum<S>, E extends Enum<E>> {

    FSMEvent<E> transform( final FSMEvent<E> _event, final FSM<S,E> _fsm, final Object _fsmContext, final FSMStateContext _stateContext );

}
