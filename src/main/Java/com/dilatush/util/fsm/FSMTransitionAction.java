package com.dilatush.util.fsm;

import com.dilatush.util.fsm.events.FSMEvent;

/**
 * Implemented to provide an action that's associated with an FSM state transition.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMTransitionAction<S extends Enum<S>, E extends Enum<E>> {

    /**
     * <p>Perform an action that is associated with an FSM state transition, using the given {@link FSMTransition}.  Note that any given action may
     * be associated with more than one FSM state transition, and the given transition will be different for each of them.</p>
     * <p>The code in an FSM transition action must not block or consume significant CPU time.  What constitutes "significant" is of course completely
     * application dependent.</p>
     *
     * @param _transition the transition this action is occurring in
     * @param _event the event the triggered this action
     */
    void run( FSMTransition<S,E> _transition, FSMEvent<E> _event );
}
