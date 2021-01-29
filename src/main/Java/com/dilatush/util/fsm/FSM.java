package com.dilatush.util.fsm;

import com.dilatush.util.Threads;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Instances of this class implement event-driven finite state machines.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSM<S extends Enum<S>,E extends Enum<E>> {

    // TODO: get rid of "convenience method" javadocs...

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // the enum of the current state of the FSM, which is the only mutable field in an FSM instance...
    private       S                                                             state;

    // the optional listener for state changes...
    private final Consumer<S>                                                   stateChangeListener;

    // arbitrary and optional object containing a context for the entire FSM; used primarily in actions and transforms...
    private final Object                                                        fsmContext;

    // FSMState instances per state, by state enum ordinal...
    private final List<FSMState<S,E>>                                           states;

    // arbitrary and optional properties that are available to the entire FSM; used primarily in actions and transforms...
    private final Map<String,Object>                                            fsmProperties;

    // map of <transition ID> -> <transition> for every defined transition; transition ID is <STATE,EVENT> tuple...
    private final Map<FSMSpec.FSMTransitionID<S,E>, FSMTransition<S,E>>         transitions;

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

        // copy what we can directly from the specification...
        state               = _spec.initialState;
        fsmContext          = _spec.context;
        bufferedEvents      = _spec.bufferedEvents;
        eventScheduling     = _spec.eventScheduling;
        transforms          = _spec.transforms;
        stateChangeListener = _spec.stateChangeListener;

        // make our list of states...
        states = new ArrayList<>();
        for( FSMSpec.FSMStateSpec<S,E> stateSpec : _spec.stateSpecs ) {
            states.add( new FSMState<>( stateSpec.onEntry, stateSpec.onExit, stateSpec.state, this, fsmContext, stateSpec.context ) );
        }

        // build our transition map...
        transitions = new HashMap<>();
        _spec.transitions.forEach( ( id, spec ) -> {
            FSMState<S,E> fromState = states.get( id.fromState.ordinal() );
            FSMState<S,E> toState   = states.get( spec.toState.ordinal() );
            transitions.put( id, new FSMTransition<>( this, fsmContext, fromState,id.event, spec.action, toState ) );
        } );

        // set up the initial state...
        FSMState<S,E> initialState = states.get( state.ordinal() );
        initialState.setLastEntered( Instant.now() );
        initialState.setEntries( 1 );

        // set up our properties map...
        fsmProperties    = new HashMap<>();

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
     *     <li>The current state and the received event together uniquely identify a possible FSM transition.  If that combination matches a defined
     *     transition, then that transition will be processed.  Note that the transition may not actually involve a change in the FSM's state (that
     *     is, the "from" state and the "to" state may be the same), in which case processing the transaction means at most the running of the
     *     transaction action (if there is one).  There are several steps to that process:
     *     <ol>
     *         <li>If the state we're transitioning from is different than the state we're transitioning to:
     *             <ol style="list-style-type:lower-alpha">
     *                 <li>Set the time that we left the current FSM state.</li>
     *                 <li>Update the total time spent in the current FSM state.</li>
     *                 <li>If there is an on-exit {@link FSMStateAction} associated with the state we're transitioning from, run it.</li>
     *             </ol>
     *         </li>
     *         <li>If there is an {@link FSMTransitionAction} associated with this transaction, run it.</li>
     *         <li>If the state we're transitioning from is different than the state we're transitioning to:
     *             <ol style="list-style-type:lower-alpha">
     *                 <li>Set the time we entered the next FSM state.  Note that if there was an action, and if that action took some time, then this might
     *                 be different than the time we left the last state.</li>
     *                 <li>Update the number of entries to the next state.</li>
     *                 <li>If there is an on-entry {@link FSMStateAction} associated with the state we're transitioning to, run it.</li>
     *                 <li>Set the FSM's state to the next state.</li>
     *             </ol>
     *         </li>
     *     </ol></li>
     *     <li>If the current FSM state and the received event do <i>not</i> match a defined transition, but the received event
     *     does have a configured event transform, then that transform is executed.  That transform may result in new events, and there are
     *     two ways to do this:<ul>
     *         <li>The transform may return an event.  If it does so, the returned event will be handled by a recursive call to this method.  That
     *         means it will be handled immediately, whether or not the FSM is configured for event buffering.</li>
     *         <li>The transform may post one or more events by calling one of the {@code onEvent()} methods.  If the FSM is configured for event
     *         buffering, then these events will be queued - possibly <i>behind</i> events queued by other threads.  If the FSM is not configured
     *         for event buffering, then these events will be handled immediately.</li>
     *     </ul></li>
     *     <li>If the current state and the received event do <i>not</i> match a defined transition, and the received event
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
        FSMTransition<S,E> transition = transitions.get( new FSMSpec.FSMTransitionID<>( state, _event.event ) );
        if( transition == null ) {
            runEventTransform( _event );
            return;
        }

        // get the the states we're leaving and going to...
        FSMState<S,E> fromState = states.get( transition.fromState.state.ordinal() );
        FSMState<S,E> toState   = states.get( transition.toState.state.ordinal()   );

        // if we're actually changing states, we've got some things to do...
        if( fromState.state != toState.state ) {

            LOGGER.finest( () -> "Transitioning from " + fromState.state + " to " + toState.state + " on event " + _event );

            // cancel any timeout that we might have had going in the state we're leaving...
            fromState.cancelTimeout();

            // if the state we're leaving has an on-exit state action, run it...
            fromState.runOnExit();

            // update the state we're leaving...
            fromState.setLastLeft( Instant.now() );
            Duration thisTime = Duration.between( fromState.getLastEntered(), fromState.getLastLeft() );
            fromState.setTimeInState( fromState.getTimeInState().plus( thisTime ) );
        }
        else {
            LOGGER.finest( () -> "Handling event " + _event + " without state change" );
        }

        // dispatch the action, if we have one...
        if( transition.action != null ) {

            LOGGER.finest( () -> "Running transition action" );

            // run the action...
            transition.action.run( transition );
        }

        // if we're actually changing the state...
        if(  fromState.state != toState.state  ) {

            // update the state we're going to...
            toState.setLastEntered( Instant.now() );
            toState.setEntries( toState.getEntries() + 1 );

            // if the state we're entering has an on-entry state action, run it...
            toState.runOnEntry();

            // set the new state...
            state = toState.state;

            // if we have a listener, inform them...
            if( stateChangeListener != null )
                stateChangeListener.accept( state );
        }
    }


    /**
     * If there is an event transform associated with the given event, run it.
     *
     * @param _event The FSM event to (possibly) run an event transform on.
     */
    private void runEventTransform( final FSMEvent<E> _event ) {

        // if we don't have an event transform, just leave, as we've got nothing to do...
        FSMEventTransform<S,E> transform = transforms.get( _event.event );
        if( transform == null )
            return;

        // ah, we DID get a transform - so run it...
        LOGGER.finest( () -> "Transforming event " + _event );
        FSMEvent<E> result = transform.run( _event, this );

        // if we got a new event, handle it immediately...
        if( result != null ) {

            // let's be sure we don't get nasty infinite recursion going here...
            if( result.event == _event.event )
                throw new IllegalStateException( "Event transform returned the same event type: " + result.event.toString() );

            // otherwise, we handle it right now, recursively...
            onEventImpl( result );
        }
    }


    /**
     * Returns the enum of the current state of this FSM.  This method is synchronized and threadsafe, though the caller should be aware that the
     * current state may change in multiple threads and with arbitrary frequency.
     *
     * @return the enum of the current state of this FSM
     */
    @SuppressWarnings( "unused" )
    public synchronized S getStateEnum() {
        return state;
    }


    /**
     * Returns the current state (as an instance of {@link FSMState}) of this FSM.  This method is synchronized and threadsafe, though the caller
     * should be aware that the current state may change in multiple threads and with arbitrary frequency.
     *
     * @return the current state (as an instance of {@link FSMState}) of this FSM
     */
    public synchronized FSMState<S,E> getState() {
        return states.get( state.ordinal() );
    }


    /**
     * <p>This method is the heart of the FSM - it handles state transitions and event transformations, all triggered by receiving an event.  There
     * are several things that can happen, depending on what state the FSM is currently in and what event is received:</p>
     * <ul>
     *     <li>If the event is an instance of {@link FSMCancellableEvent} and the event has been cancelled, then this method does nothing at all.
     *     This mechanism eliminates race conditions that might otherwise arise because the scheduled events are posted in a different thread from
     *     the one actions are run in.</li>
     *     <li>The current state and the received event together uniquely identify a possible FSM transition.  If that combination matches a defined
     *     transition, then that transition will be processed.  Note that the transition may not actually involve a change in the FSM's state (that
     *     is, the "from" state and the "to" state may be the same), in which case processing the transaction means at most the running of the
     *     transaction action (if there is one).  There are several steps to that process:
     *     <ol>
     *         <li>If the state we're transitioning from is different than the state we're transitioning to:
     *             <ol style="list-style-type:lower-alpha">
     *                 <li>Set the time that we left the current FSM state.</li>
     *                 <li>Update the total time spent in the current FSM state.</li>
     *                 <li>If there is an on-exit {@link FSMStateAction} associated with the state we're transitioning from, run it.</li>
     *             </ol>
     *         </li>
     *         <li>If there is an {@link FSMTransitionAction} associated with this transaction, run it.</li>
     *         <li>If the state we're transitioning from is different than the state we're transitioning to:
     *             <ol style="list-style-type:lower-alpha">
     *                 <li>Set the time we entered the next FSM state.  Note that if there was an action, and if that action took some time, then this might
     *                 be different than the time we left the last state.</li>
     *                 <li>Update the number of entries to the next state.</li>
     *                 <li>If there is an on-entry {@link FSMStateAction} associated with the state we're transitioning to, run it.</li>
     *                 <li>Set the FSM's state to the next state.</li>
     *             </ol>
     *         </li>
     *     </ol></li>
     *     <li>If the current FSM state and the received event do <i>not</i> match a defined transition, but the received event
     *     does have a configured event transform, then that transform is executed.  That transform may result in new events, and there are
     *     two ways to do this:<ul>
     *         <li>The transform may return an event.  If it does so, the returned event will be handled by a recursive call to this method.  That
     *         means it will be handled immediately, whether or not the FSM is configured for event buffering.</li>
     *         <li>The transform may post one or more events by calling one of the {@code onEvent()} methods.  If the FSM is configured for event
     *         buffering, then these events will be queued - possibly <i>behind</i> events queued by other threads.  If the FSM is not configured
     *         for event buffering, then these events will be handled immediately.</li>
     *     </ul></li>
     *     <li>If the current state and the received event do <i>not</i> match a defined transition, and the received event
     *     does not have a configured event transform, then this method does nothing at all.</li>
     * </ul>
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
     * Convenience method that simply wraps the given event {@link Enum} and event data {@link Object} in an instance of {@link FSMEvent}
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
     * Convenience method that simply wraps the given event {@link Enum} and a {@code null}  for the event data {@link Object} in an
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
     * Convenience method that simply wraps the given event {@link Enum} and event data {@link Object} in an instance of
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
     * Convenience method that simply wraps the given event {@link Enum} and a {@code null} for the event data {@link Object} in an instance
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
     * Set the global FSM property with the given name to the given value (which may be {@code null}).
     *
     * @param _name The name of the global FSM property to be set.
     * @param _value  The value to set the named global FSM property to.
     */
    @SuppressWarnings( "unused" )
    public synchronized void setProperty( final String _name, final Object _value ) {

        // fail fast if we have an argument problem...
        if( isEmpty( _name ) )
            throw new IllegalArgumentException( "No property name" );

        fsmProperties.put( _name, _value );
    }


    /**
     * Return the value of the global FSM property with the given name, or {@code null} if there is no global FSM property with the given name.
     *
     * @param _name The name of the global FSM property to retrieve.
     * @return the value of the named global FSM property, or {@code null} if the named property does not exist
     */
    public synchronized Object getProperty( final String _name ) {

        // fail fast if we have an argument problem...
        if( isEmpty( _name ) )
            throw new IllegalArgumentException( "No property name" );

        return fsmProperties.get( _name );
    }


    /**
     * Set the state-specific FSM property with the given name to the given value (which may be {@code null}).
     *
     * @param _state The FSM state whose property is to be set.
     * @param _name The name of the state-specific FSM property to be set.
     * @param _value The value to set the state-specific FSM property to.
     */
    @SuppressWarnings( "unused" )
    public synchronized void setProperty( final S _state, final String _name, final Object _value ) {

        // fail fast if we have an argument problem...
        if( isNull( _state ) )
            throw new IllegalArgumentException( "No state supplied" );
        if( isEmpty( _name ) )
            throw new IllegalArgumentException( "No property name" );

        // get the given state...
        FSMState<S,E> givenState = states.get( _state.ordinal() );

        // set the property using the state's setter...
        givenState.setProperty( _name, _value );
    }


    /**
     * Get the FSM state context for the given FSM state enum.
     *
     * @param _state The FSM state enum to retrieve the FSM state context for.
     * @return the FSM state context for the given FSM state enum
     */
    @SuppressWarnings( "unused" )
    public Object getStateContext( final S _state ) {
        return states.get( _state.ordinal() ).context;
    }


    /**
     * Get the FSM global context.
     *
     * @return the FSM global context
     */
    @SuppressWarnings( "unused" )
    public Object getGlobalContext() {
        return fsmContext;
    }


    /**
     * Return the value of the state-specific FSM property with the given name, or {@code null} if there is no state-specific FSM property with the
     * given name.
     *
     * @param _state The FSM state enum for the FSM state whose property is to be retrieved.
     * @param _name The name of the state-specific FSM property to retrieve.
     * @return the value of the named state-specific FSM property, {@code null} if the named property does not exist
     */
    public synchronized Object getProperty( final S _state, final String _name ) {

        // fail fast if we have an argument problem...
        if( isNull( _state ) )
            throw new IllegalArgumentException( "No state supplied" );
        if( isEmpty( _name ) )
            throw new IllegalArgumentException( "No property name" );

        // get the FSM state for the given state enum...
        FSMState<S,E> givenState = states.get( _state.ordinal() );

        // get the property using the state's getter...
        return givenState.getProperty( _name );
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
