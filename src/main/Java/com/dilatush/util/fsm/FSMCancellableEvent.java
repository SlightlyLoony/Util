package com.dilatush.util.fsm;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * <p>Instances of this class are created only by an {@link FSM} instance, and they represent FSM events that were scheduled, and which may be cancelled
 * by calling their {@link #cancel()} method.  Note that these events may be cancelled even <i>after</i> the scheduler has submitted them to the FSM.
 * This works because the FSM handles events purely sequentially (never with multiple threads in parallel), so long as the event being cancelled is
 * not the current event being handled, that cancellation will have occurred before the FSM tries to handle it.  The FSM might still have a cancelled
 * event submitted to one of its {@code onEvent()} handlers, but the handler will ignore the cancelled event.</p>
 * <p>This mechanism prevents race conditions which would otherwise allow cancelled events to be handled by the FSM.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMCancellableEvent<E extends Enum<E>> extends FSMEvent<E> {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // true if this event has been cancelled...
    volatile private boolean cancelled;

    // set by the FSM's scheduler...
    volatile private ScheduledFuture<?> future;


    /**
     * Create a new instance of this class with the given FSM event.
     *
     * @param _event The FSM event to be made cancellable.
     */
    /*package-private*/ FSMCancellableEvent( final FSMEvent<E> _event ) {

        super( _event.event, _event.data );
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
}
