package com.dilatush.util.fsm.events;

import com.dilatush.util.fsm.FSM;

/**
 * Instances of this class represent simple FSM events: those with no data and which are not cancellable.  Each event <i>must</i> include an event
 * {@code Enum} that indicates the kind of event it is.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/*package-private*/ class FSMSimpleEvent<E extends Enum<E>> extends FSMEvent<E> {


    /**
     * Create a new instance of this class with the given event type and no optional data object.
     *
     * @param _event The event type {@code Enum} for this event.
     */
    /*package-private*/ FSMSimpleEvent( final E _event ) {
        super( _event );
    }


    /**
     * Cancel this event, if it was scheduled by one of {@link FSM}'s {@code scheduleEvent()} methods.  Otherwise, this method does nothing.
     */
    public void cancel() {
    }


    /**
     * Returns this event's associated data (which is an arbitrary object), or {@code null} if there is none.
     *
     * @return this event's associated data (which is an arbitrary object), or {@code null} if there is none
     */
    public Object getData() {
        return null;
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
     * Return a string representing this instance, which is simply the result of the {@code toString()} method of the event enum.
     *
     * @return a string representing this instance
     */
    @Override
    public String toString() {
        return event.toString();
    }
}
