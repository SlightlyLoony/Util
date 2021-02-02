package com.dilatush.util.fsm.events;

import com.dilatush.util.fsm.FSM;

import java.util.concurrent.ScheduledFuture;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent FSM events.  Each event <i>must</i> include an event {@code Enum} that indicates the kind of event it is, and
 * <i>may</i> include an arbitrary data object with more information about the event.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class FSMEvent<E extends Enum<E>> {


    /**
     * The event's identifying enum.
     */
    public final E      event;


    /**
     * Create a new instance of this class with the given event type and no optional data object.
     *
     * @param _event The event type {@code Enum} for this event.
     */
    protected FSMEvent( final E _event ) {

        // fail fast if we didn't get an event...
        if( isNull( _event) )
            throw new IllegalArgumentException( "No Enum for FSMEvent" );

        event = _event;
    }


    /**
     * Cancel this event, if it was scheduled by one of {@link FSM}'s {@code scheduleEvent()} methods.  Otherwise this method does nothing.
     */
    public abstract void cancel();


    /**
     * Returns this event's associated data (which is an arbitrary object), or {@code null} if there is none.
     *
     * @return this event's associated data (which is an arbitrary object), or {@code null} if there is none
     */
    public abstract Object getData();


    /**
     * Returns {@code true} if this event has been cancelled.
     *
     * @return {@code true} if this event has been cancelled
     */
    public abstract boolean isCancelled();


    /**
     * Used by the FSM to set the {@link ScheduledFuture} in this instance, after scheduling the FSM event.
     *
     * @param _future The {@link ScheduledFuture} returned by the FSM event scheduler.
     */
    /*package-private*/ void setFuture( final ScheduledFuture<?> _future ) {
    }
}
