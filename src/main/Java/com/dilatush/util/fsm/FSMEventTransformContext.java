package com.dilatush.util.fsm;

/**
 * Instances of this class are the context in which an FSM event transform operates, giving that transform access to the FSM itself, the FSM's
 * optional global context, and the FSM state context for the current FSM state.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMEventTransformContext<S extends Enum<S>, E extends Enum<E>> {


    /**
     * The FSM instance associated with this instance.  The FSM is included to give the transform access to its public methods for event posting and
     * scheduling.
     */
    public final FSM<S,E>        fsm;            // the finite state machine (FSM)


    /**
     * The context object associated with the FSM globally, not any particular state or event.  This object is optional, and its type is arbitrary,
     * determined by the program using the FSM.  It must be cast to the agreed type before it can be used.
     */
    public final Object          fsmContext;     // the FSM global context


    /**
     * The context object associated with the FSM state currently active.  By default this is an instance of {@link FSMStateContext}, but most
     * often it is a subclass that contains fields specific to the state.
     */
    public final FSMStateContext stateContext;   // the state context for the current state


    /**
     * Create a new instance of this class with the given values.
     *
     * @param _fsm The {@link FSM} instance associated with this transform.
     * @param _fsmContext The optional FSM global context associated with the FSM associated with this transform.
     * @param _stateContext The {@link FSMStateContext} (or subclass) associated with the current state of the associated FSM.
     */
    /*package-private*/ FSMEventTransformContext( final FSM<S, E> _fsm, final Object _fsmContext, final FSMStateContext _stateContext ) {
        fsm = _fsm;
        fsmContext = _fsmContext;
        stateContext = _stateContext;
    }
}
