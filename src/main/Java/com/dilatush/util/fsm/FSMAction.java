package com.dilatush.util.fsm;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMAction<S extends Enum<S>, E extends Enum<E>> {

    void action( FSMActionContext<S,E> _actionContext );
}
