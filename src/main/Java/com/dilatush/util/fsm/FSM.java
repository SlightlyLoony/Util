package com.dilatush.util.fsm;

import com.dilatush.util.Threads;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Instances of this class implement event-driven finite state machines.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSM<S extends Enum<S>,E extends Enum<E>> {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // the current state of the FSM, which is the only mutable field in an instance...
    private       S                                                             state;

    // arbitrary and optional object containing a context for the entire FSM; used in actions and transforms...
    private final Object                                                        fsmContext;

    // FSMStateContext instances per state, by state enum ordinal; may be subclassed and may be different subclasses for each state...
    private final FSMStateContext[]                                             stateContexts;

    // map of <transition ID> -> <transition> for every defined transition; transition ID is <STATE,EVENT> tuple...
    private final Map<FSMTransitionID<S,E>, FSMTransition<FSMAction<S,E>,S,E>>  transitions;

    // map of <event> -> <event transform> for every defined event transform...
    private final Map<E, FSMEventTransform<S,E>>                                transforms;

    // true if event scheduling services are enabled...
    private final boolean                                                       eventScheduling;

    // true if event buffering is is enabled...
    private final boolean                                                       bufferedEvents;

    // the thread for the event dispatcher, IF event buffering is enabled...
    private final Thread                                                        eventDispatcher;

    // the deque for event buffering, IF event buffering is enabled...
    private final BlockingDeque<FSMEvent<E>>                                    eventsBuffer;

    // the scheduled executor for scheduled events, IF event scheduling is enabled...
    private final ScheduledExecutorService                                      eventScheduler;


    /**
     * Creates a new instance of this class with the given {@link FSMSpec}.  If the specification fails validation, it throws an
     * {@link IllegalArgumentException}.
     *
     * @param _spec The {@link FSMSpec} specification for this finite state machine.
     */
    public FSM( final FSMSpec<S,E> _spec ) {

        // if the specification is not valid, bail out with a detailed error message...
        if( !_spec.isValid() )
            throw new IllegalArgumentException( "FSMSpec is not valid:\n" + _spec.getErrorMessage() );

        // copy the core bits from the specification...
        state            = _spec.initialState;
        fsmContext       = _spec.context;
        transitions      = _spec.transitions;
        stateContexts    = _spec.stateContexts;
        bufferedEvents   = _spec.bufferedEvents;
        eventScheduling  = _spec.eventScheduling;
        transforms       = _spec.transforms;

        // if we're buffering events, set up our dispatch thread...
        if( bufferedEvents ) {
            eventsBuffer = new LinkedBlockingDeque<>( _spec.maxBufferedEvents );
            eventDispatcher = Threads.startDaemonThread( this::dispatch, "FSMEventDispatcher" );
        }

        // otherwise, null the fields to make the compiler happy...
        else {
            eventsBuffer = null;
            eventDispatcher = null;
        }

        // if we're supporting event scheduling, set up the scheduled executor...
        if( eventScheduling )
            eventScheduler = Executors.newSingleThreadScheduledExecutor( new Threads.DaemonThreadFactory( "FSMEventScheduler" ));

        // otherwise, null the field to make the compiler happy...
        else
            eventScheduler = null;
    }


    /**
     * <p>This method is the heart of the FSM - it handles state transitions and event transformations, all triggered by receiving an event.  There
     * are several things that can happen, depending on what state the FSM is currently in and what event is received:</p>
     * <ul>
     *     <li>If the event is an instance of {@link FSMCancellableEvent} and the event has been cancelled, then this method does nothing at all.
     *     This mechanism eliminates race conditions that might otherwise arise because the scheduled events are posted in a different thread from
     *     the one actions are run in.</li>
     *     <li>If the current state and the received event (the transition ID) match a defined transition, then the FSM transitions from its
     *     current state to the TO_STATE defined in the transition definition.  There are three steps to that process:
     *     <ol>
     *         <li>Set the time that we left the current FSM state.</li>
     *         <li>Update the total time spent in the current FSM state.</li>
     *         <li>If there is an {@link FSMAction} associated with this transaction, execute it.</li>
     *         <li>Set the time we entered the next FSM state.  Note that if there was an action, and if that action took some time, then this might
     *         be different than the time we left the last state.</li>
     *         <li>Update the number of entries to the next state.</li>
     *         <li>Set the FSM's state to the next state.</li>
     *     </ol></li>
     *     <li>If the current FSM state and the received event (the transition ID) <i>do not</i> match a defined transition, but the received event
     *     does have a configured event transform, then execute that transform.  Often these transforms will result in new events, and there are
     *     two ways to do this:<ul>
     *         <li>The transform may return an event.  If it does so, the returned event will be handled by a recursive call to this method.  That
     *         means it will be handled immediately, whether or not the FSM is configured for event buffering.</li>
     *         <li>The transform may post one or more events by calling one of the {@code onEvent()} methods.  If the FSM is configured for event
     *         buffering, then these events will be queued - possibly <i>behind</i> events queued by other threads.  If the FSM is not configured
     *         for event buffering, then these events will be handled immediately.</li>
     *     </ul></li>
     *     <li>If the current state and the received event (the transition ID) <i>do not</i> match a defined transition, and the received event
     *     does not have a configured event transform, then this method does nothing at all.</li>
     * </ul>
     *
     * @param _event The event to be handled.
     */
    private void onEventImpl( final FSMEvent<E> _event ) {

        // if our event is a cancellable event that has been cancelled, ignore it...
        if( _event instanceof FSMCancellableEvent ) {
            FSMCancellableEvent<E> cancellableEvent = (FSMCancellableEvent<E>) _event;
            if( cancellableEvent.isCancelled() )
                return;
        }

        // if we don't have a transition mapped for the current state and this event, then let's see if we have a transform...
        FSMTransition<FSMAction<S,E>,S,E> transition = transitions.get( new FSMTransitionID<>( state, _event.event ) );
        if( transition == null ) {

            // if we don't have an event transform, just leave, as we've got nothing to do...
            FSMEventTransform<S,E> transform = transforms.get( _event.event );
            if( transform == null )
                return;

            // ah, we DID get a transform - so run it...
            FSMEvent<E> result = transform.transform( _event, this, fsmContext, stateContexts[state.ordinal()] );

            // if we got a new event, handle it immediately...
            if( result != null )
                onEventImpl( result );

            // and we're done...
            return;
        }

        LOGGER.finest( () -> "Transition from " + state + " on event " + _event );

        // update the context of the state we're leaving: exit time and time in state...
        FSMStateContext fromContext = stateContexts[state.ordinal()];
        if( state != transition.toState ) {
            fromContext.setLastLeft( Instant.now() );
            Duration thisTime = Duration.between( fromContext.getLastEntered(), fromContext.getLastLeft() );
            fromContext.setTimeInState( fromContext.getTimeInState().plus( thisTime ) );
        }

        // get the context for the state we're going to...
        FSMStateContext toContext = stateContexts[transition.toState.ordinal()];

        // dispatch the action, if we have one...
        if( transition.action != null ) {

            LOGGER.finest( () -> "Running action" );

            // create the action context for this transition...
            FSMActionContext<S,E> context = new FSMActionContext<>(
                    state,
                    transition.toState,
                    _event,
                    fromContext,
                    toContext,
                    fsmContext,
                    this
            );

            // run the action...
            transition.action.action( context );
        }

        // update the context of the state we're going to: entry time and number of entries...
        if( state != transition.toState ) {
            toContext.setLastEntered( Instant.now() );
            toContext.setEntries( toContext.getEntries() + 1 );

            // set the new state...
            state = transition.toState;
        }

        LOGGER.finest( () -> "Transitioned to state " + state );
    }


    /**
     * <p>This method is at the heart of the FSM - it handles state transitions and event transformations, all triggered by receiving an event.  If
     * this FSM instance is configured for event buffering, this method queues the given event in the event buffer, and another thread does the actual
     * event handling.  If this FSM instance is not configured for event buffering, then this method is synchronized (on the FSM instance) and then
     * handles the event directly.  Once the event is being handled, there are several things that can happen, depending on what state the FSM is
     * currently in and what event is received:</p>
     * <ul>
     *     <li>If the event is an instance of {@link FSMCancellableEvent} and the event has been cancelled, then this method does nothing at all.
     *     This mechanism eliminates race conditions that might otherwise arise because the scheduled events are posted in a different thread from
     *     the one actions are run in.</li>
     *     <li>If the current state and the received event (the transition ID) match a defined transition, then the FSM transitions from its
     *     current state to the TO_STATE defined in the transition definition.  There are several steps to that process:
     *     <ol>
     *         <li>Set the time that we left the current FSM state.</li>
     *         <li>Update the total time spent in the current FSM state.</li>
     *         <li>If there is an {@link FSMAction} associated with this transaction, execute it.</li>
     *         <li>Set the time we entered the next FSM state.  Note that if there was an action, and if that action took some time, then this might
     *         be different than the time we left the last state.</li>
     *         <li>Update the number of entries to the next state.</li>
     *         <li>Set the FSM's state to the next state.</li>
     *     </ol></li>
     *     <li>If the current FSM state and the received event (the transition ID) <i>do not</i> match a defined transition, but the received event
     *     does have a configured event transform, then execute that transform.  Often these transforms will result in new events, and there are
     *     two ways to do this:<ul>
     *         <li>The transform may return an event.  If it does so, the returned event will be handled immediately, whether or not the FSM is
     *         configured for event buffering.</li>
     *         <li>The transform may post one or more events by calling one of the FSM's {@code onEvent()} methods.  If the FSM is configured for
     *         event buffering, then these events will be queued - possibly <i>behind</i> events queued by other threads.  If the FSM is not
     *         configured for event buffering, then these events will be handled immediately.</li>
     *     </ul></li>
     *     <li>If the current state and the received event (the transition ID) <i>do not</i> match a defined transition, and the received event
     *     does not have a configured event transform, then this method does nothing at all.</li>
     * </ul>
     * <p>This method is threadsafe whether or not event buffering is enabled.</p>
     *
     * @param _event The event to be handled.
     */
    public void onEvent( final FSMEvent<E> _event ) {

        // if event buffering is enabled, add the event to our queue...
        if( bufferedEvents ) {
            LOGGER.finest( () -> "Queuing event " + _event );
            eventsBuffer.addLast( _event );
        }

        // if event buffering is disabled, synchronize on the FSM instance and then call the handler...
        else {
            LOGGER.finest( () -> "Synchronizing, handling event " + _event );
            synchronized( this ) {
                onEventImpl( _event );
            }
        }
    }


    /**
     * This is a convenience method that simply wraps the given event {@link Enum} and event data {@link Object} in an instance of {@link FSMEvent}
     * and calls {@link #onEvent(FSMEvent)}.
     *
     * @param _event The event enum for the event to be handled.
     * @param _eventData The event data object for the event to be handled.
     */
    @SuppressWarnings( "unused" )
    public void onEvent( final E _event, final Object _eventData ) {
        onEvent( new FSMEvent<>( _event, _eventData ) );
    }


    /**
     * This is a convenience method that simply wraps the given event {@link Enum} and a {@code null}  for the event data {@link Object} in an
     * instance of {@link FSMEvent} and calls {@link #onEvent(FSMEvent)}.
     *
     * @param _event The event enum for the event to be handled.
     */
    public void onEvent( final E _event ) {
        onEvent( new FSMEvent<>( _event, null ) );
    }


    /**
     * <p>Schedule the given {@link FSMEvent} to be handled after the given {@link Duration} delay, and return a {@link ScheduledFuture} instance
     * that can be used to cancel this schedule any time before it delivers the event.  If event buffering is not enabled on this FSM instance, then
     * the event handling of these events will be handled (including executing actions and event transforms) in the scheduler's thread. If event
     * buffering <i>is</i> enabled on this FSM instance, then the scheduler will simply add these events to the internal event queue.  If event
     * scheduling is not enabled in this instance, an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @param _event The {@link FSMEvent} to be scheduled.
     * @param _delay The {@link Duration} delay until it is to be handled.
     * @return The {@link FSMCancellableEvent} that can be used to cancel this scheduled event.
     */
    public FSMCancellableEvent<E> scheduleEvent( final FSMEvent<E> _event, final Duration _delay ) {

        // if we don't have event scheduling enabled, complain loudly...
        if( !eventScheduling )
            throw new UnsupportedOperationException( "Event scheduling is disabled in this FSM" );

        // the compiler is confused by the branch in the constructor for eventScheduling...
        assert eventScheduler != null;

        LOGGER.finest( () -> "Scheduled " + _event + " for " + _delay.toString().substring( 2 ) );

        // schedule the event and return the cancellable event...
        FSMCancellableEvent<E> cancellableEvent = new FSMCancellableEvent<>( _event );
        ScheduledFuture<?> scheduledFuture = eventScheduler.schedule(
                new EventSender( cancellableEvent ),   // the Runnable with our event ready to post...
                _delay.toNanos(),                      // the delay in nanoseconds...
                TimeUnit.NANOSECONDS                   // tell the scheduler that it's in nanoseconds...
        );
        cancellableEvent.setFuture( scheduledFuture ); // stuff the scheduled future into our cancellable event...
        return cancellableEvent;
    }



    /**
     * This is a convenience method that simply wraps the given event {@link Enum} and event data {@link Object} in an instance of
     * {@link FSMEvent} and calls {@link #scheduleEvent(FSMEvent, Duration)}.
     *
     * @param _event The event enum for the event to be scheduled.
     * @param _eventData The event data object for the event to be scheduled.
     * @param _delay The delay until it is to be handled.
     * @return The {@link FSMCancellableEvent} that can be used to cancel this scheduled event.
     */
    @SuppressWarnings( "unused" )
    public FSMCancellableEvent<E> scheduleEvent( final E _event, final Object _eventData, final Duration _delay ) {
        return scheduleEvent( new FSMEvent<>( _event, _eventData ), _delay );
    }


    /**
     * This is a convenience method that simply wraps the given event {@link Enum} and a {@code null} for the event data {@link Object} in an instance
     * of {@link FSMEvent} and calls {@link #scheduleEvent(FSMEvent, Duration)}.
     *
     * @param _event The event enum for the event to be scheduled.
     * @param _delay The delay until it is to be handled.
     * @return The {@link FSMCancellableEvent} that can be used to cancel this scheduled event.
     */
    public FSMCancellableEvent<E> scheduleEvent( final E _event, final Duration _delay ) {
        return scheduleEvent( new FSMEvent<>( _event, null ), _delay );
    }


    /**
     * A simple {@link Runnable} implementation used to schedule a future posting of an event.
     */
    private class EventSender implements Runnable {

        // the event to be posted in the future...
        private final FSMEvent<E> event;


        /**
         * Creates a new instance of this class with the given {@link FSMEvent} to be handled at a future time.
         *
         * @param _event The {@link FSMEvent} to be handled at a future time.
         */
        private EventSender( final FSMEvent<E> _event ) {
            event = _event;
        }


        /**
         * Post the {@link FSMEvent} contained in this instance.
         */
        @Override
        public void run() {
            LOGGER.finest( () -> "Handling scheduled event " + event );
            onEvent( event );
        }
    }


    /**
     * Runnable that pulls events from the event buffer and dispatches them to the event handler.
     */
    private void dispatch() {

        try {
            // the compiler is confused by the branch in the constructor that sets these up...
            assert eventsBuffer != null;
            assert eventDispatcher != null;

            while( !eventDispatcher.isInterrupted() ) {
                FSMEvent<E> event = eventsBuffer.takeFirst();
                onEventImpl( event );
            }
        }
        catch( InterruptedException _e ) {
            // naught to do here; our thread will simply terminate...
        }
    }
}
