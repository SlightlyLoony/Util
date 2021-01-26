package com.dilatush.util.fsm;

/**
 * Implemented by classes that transform FSM events.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMEventTransform<S extends Enum<S>, E extends Enum<E>> {

    FSMEvent<E> transform( final FSMEvent<E> _event, final FSMEventTransformContext<S,E> _context );

}
