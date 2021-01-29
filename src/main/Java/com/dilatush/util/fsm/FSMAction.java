package com.dilatush.util.fsm;

/**
 * Implemented to provide an action that's associated with an FSM state transition.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMAction<S extends Enum<S>, E extends Enum<E>> {

    /**
     * <p>Perform an action that is associated with an FSM state transition, using the given {@link FSMActionContext}.  Note that any given action may
     * be associated with more than one FSM state transition, and the given context will be different for each of them.</p>
     * <p>The code in an FSM action must not block or consume significant CPU time.  What constitutes "significant" is of course completely
     * application dependent.</p>
     *
     * @param _actionContext the context for the action
     */
    void action( FSMActionContext<S,E> _actionContext );
}
