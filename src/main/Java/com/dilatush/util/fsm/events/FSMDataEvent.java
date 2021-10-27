package com.dilatush.util.fsm.events;

import com.dilatush.util.fsm.FSM;

/**
 * Instances of this class represent events that have associated data (an arbitrary {@code Object}).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/*package-private*/ class FSMDataEvent<E extends Enum<E>> extends FSMEvent<E> {


    /**
     * The event's optional data.
     */
    protected final Object data;


    /**
     * Create a new instance of this class with the given event type and optional data object.
     *
     * @param _event The event type {@code Enum} for this event.
     * @param _data  The optional data object for this event, which may be {@code null}.
     */
    protected FSMDataEvent( final E _event, final Object _data ) {
        super( _event );

        data = _data;
    }


    /**
     * Cancel this event, if it was scheduled by one of {@link FSM}'s {@code scheduleEvent()} methods.  Otherwise, this method does nothing.
     */
    @Override
    public void cancel() {
    }


    /**
     * Returns this event's associated data (which is an arbitrary object), or {@code null} if there is none.
     *
     * @return this event's associated data (which is an arbitrary object), or {@code null} if there is none
     */
    public Object getData() {
        return data;
    }


    /**
     * Returns {@code true} if this event has been cancelled.
     *
     * @return {@code true} if this event has been cancelled
     */
    public boolean isCancelled() {
        return false;
    }


    /**
     * Return a string representing this instance.  If the event has no data object, then this method returns the result of the {@code toString()}
     * method on the type {@code Enum}.  Otherwise, this method returns the result of the {@code toString()} method on the type {@code Enum},
     * followed by a space, and finally the result of the {@code toString()} method on the data object.
     *
     * @return a string representing this instance
     */
    @Override
    public String toString() {
        return (data == null) ? event.toString() : event.toString() + " " + data;
    }
}
