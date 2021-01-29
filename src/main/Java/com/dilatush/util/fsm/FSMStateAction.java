package com.dilatush.util.fsm;

/**
 * Implemented to provide an action that's associated with an FSM state: on-entry and on-exit actions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMStateAction<S extends Enum<S>,E extends Enum<E>> {

    /**
     * <p>Perform an action associated with an FSM state (either on entry to the state, or on exit from it) using the given {@link FSMState} as the
     * context for the action.</p>
     * <p>The code in an FSM state action must not block or consume significant CPU time.  What constitutes "significant" is of course completely
     * application dependent.</p>
     *
     * @param _state The FSM state that provides the context for this action.
     */
    void run( final FSMState<S,E> _state );
}
