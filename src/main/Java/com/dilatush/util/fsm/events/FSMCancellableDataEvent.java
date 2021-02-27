package com.dilatush.util.fsm.events;

import com.dilatush.util.fsm.FSM;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * <p>Instances of this class represent events that have associated data and which are cancellable.</p>
 * <p>Instances of this class are created only by an {@link FSM} instance, and they represent FSM events that were scheduled, and which may be
 * cancelled by calling their {@link #cancel()} method.  Note that these events may be cancelled even <i>after</i> the scheduler has submitted them to
 * the FSM.  This works because the FSM handles events purely sequentially (never with multiple threads in parallel), so long as the event being
 * cancelled is not the current event being handled, that cancellation will have occurred before the FSM tries to handle it.  The FSM might still have
 * a cancelled event submitted to one of its {@code onEvent()} handlers, but the handler will ignore the cancelled event.</p>
 * <p>This mechanism prevents race conditions which would otherwise allow cancelled events to be handled by the FSM.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMCancellableDataEvent<E extends Enum<E>> extends FSMDataEvent<E> {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // true if this event has been cancelled...
    volatile private boolean cancelled;

    // set by the FSM's scheduler...
    volatile private ScheduledFuture<?> future;


    /**
     * Create a new instance of this class with the given FSM event.
     *
     * @param _event The FSM event to be made cancellable.
     * @param _data The data for the cancellable event.
     */
    /*package-private*/ FSMCancellableDataEvent( final E _event, final Object _data ) {
        super( _event, _data );
        cancelled = false;
    }


    /**
     * Used by the FSM to set the {@link ScheduledFuture} in this instance, after scheduling the FSM event.
     *
     * @param _future The {@link ScheduledFuture} returned by the FSM event scheduler.
     */
    /*package-private*/ void setFuture( final ScheduledFuture<?> _future ) {
        future = _future;
    }


    /**
     * Returns {@code true} if this event has been cancelled.
     *
     * @return {@code true} if this event has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }


    /**
     * <p>Cancel this scheduled FSM event.  Note that a scheduled event may be cancelled even <i>after</i> the scheduler has submitted it to the FSM.
     * This works because the FSM handles events purely sequentially (never with multiple threads in parallel), and so long as the event being
     * cancelled is not the current event being handled, that cancellation will have occurred before the FSM tries to handle it.  The FSM might still
     * have a cancelled event submitted to one of its {@code onEvent()} handlers, but the handler will ignore the cancelled event.</p>
     * <p>This mechanism prevents race conditions which would otherwise allow cancelled events to be handled by the FSM.</p>
     */
    public void cancel() {

        LOGGER.finest( "Cancelling event " + event );

        // cancel it in the scheduler, in case it hasn't been dispatched yet...
        future.cancel( /* interruptTask */ false );

        // then set our flag, in case the scheduler HAS dispatched it...
        cancelled = true;
    }


    /**
     * Return a string representing this instance.  If the event has no data object, then this method returns the result of the {@code toString()}
     * method on the type {@code Enum}.  Otherwise this method returns the the result of the {@code toString()} method on the type {@code Enum},
     * followed by a space, and finally the result of the {@code toString()} method on the data object.
     *
     * @return a string representing this instance
     */
    @Override
    public String toString() {
        return "Cancellable: " + ((data == null) ? event.toString() : event.toString() + " " + data.toString());
    }
}
