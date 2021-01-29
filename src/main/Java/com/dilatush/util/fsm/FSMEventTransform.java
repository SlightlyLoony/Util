package com.dilatush.util.fsm;

/**
 * Implemented by classes or methods that transform FSM events.  Instances of this class must be added to the FSM specification (with
 * {@link FSMSpec#addEventTransform(Enum, FSMEventTransform)}) in order to have effect.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface FSMEventTransform<S extends Enum<S>, E extends Enum<E>> {


    /**
     * <p>Transform the given event into another event (or no event), using the given FSM event transform context.  The transform occurs as the FSM is
     * handling the event - if the FSM can't find a state transition for the given event, it then checks to see if there is a matching FSM event
     * transform.  FSM event transforms can be used to "preprocess" events, typically to transform events reporting quantitative data into more
     * discrete events.  For example, you might have a {@code TEMPERATURE} event whose data was the measured temperature in degrees C.  However, your
     * state machine might only care whether the temperature was in a given range, below it, or above it.  In that case, you might have an FSM event
     * transform that transformed a TEMPERATURE event into the appropriate TEMP_LO, TEMP_HI, TEMP_GOOD FSM event.</p>
     * <p>FSM event transforms are not limited to cases like the example above; they are free to take any action they'd like.  In particular, they
     * can:</p>
     * <ul>
     *     <li>Return an FSM event, which will be handled immediately, not queued for dispatch.  Naturally this must be a different kind of event
     *     than the one being transformed (else we'd have an infinite recursion problem).</li>
     *     <li>Return a {@code null}, which means no event is handled immediately; the next handled event will come from the dispatch queue.</li>
     *     <li>Dispatch one (or more) events by calling the FSM {@code onEvent()} methods; these events will be handled normally, possibly being
     *     queued behind other events.</li>
     *     <li>Schedule one (or more) events by calling the FSM {@code scheduleEvent()} methods.</li>
     * </ul>
     * <p>The code in an FSM event transform must not block or consume significant CPU time.  What constitutes "significant" is of course completely
     * application dependent.</p>
     *
     * @param _event The FSM event to be transformed.
     * @param _context The {@link FSMEventTransformContext} for the event transformation.
     * @return the transformed event, or {@code null} if none
     */
    FSMEvent<E> transform( final FSMEvent<E> _event, final FSMEventTransformContext<S,E> _context );

}
