package com.dilatush.util.fsm;

import com.dilatush.util.fsm.events.FSMEvent;

/**
 * Implemented to provide an action that's associated with an FSM event.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMEventAction<S extends Enum<S>,E extends Enum<E>> {

    /**
     * <p>Perform an action associated with an FSM event using the event itself and the given {@link FSMState} as the
     * context for the action.</p>
     * <p>The code in an FSM state action must not block or consume significant CPU time.  What constitutes "significant" is of course completely
     * application dependent.</p>
     *
     * @param _event The FSM event that triggered this action.
     * @param _state The FSM state that provides the context for this action.
     */
    void run( final FSMEvent<E> _event, final FSMState<S,E> _state );
}
