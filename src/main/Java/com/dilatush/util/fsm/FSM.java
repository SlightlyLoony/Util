package com.dilatush.util.fsm;

import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.Threads;
import com.dilatush.util.fsm.events.FSMEvent;
import com.dilatush.util.fsm.events.FSMEvents;

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

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // the enum of the current state of the FSM, which is the only mutable field in an FSM instance...
    private       S                                                             state;

    // the initial state of this FSM...
    private final S                                                             initialState;

    // the optional listener for state changes...
    private final Consumer<S>                                                   stateChangeListener;

    // arbitrary and optional object containing a context for the entire FSM; used primarily in actions and transforms...
    private final Object                                                        fsmContext;

    // FSMState instances per state, by state enum ordinal...
    private final List<FSMState<S,E>>                                           states;

    // arbitrary and optional properties that are available to the entire FSM; used primarily in actions and transforms...
    private final Map<String,Object>                                            fsmProperties;

    // lookup table for transitions, indexed by state, then event...
    private final List<List<FSMTransition<S,E>>>                                transitions;

    // lookup table for event transforms...
    private final List<FSMEventTransform<S,E>>                                  transforms;

    // lookup table for on-entry actions...
    private final List<FSMStateAction<S,E>>                                     onEntryActions;

    // lookup table for on-exit actions...
    private final List<FSMStateAction<S,E>>                                     onExitActions;

    // lookup table for event actions...
    private final List<FSMEventAction<S,E>>                                     eventActions;

    // true if event scheduling services are enabled...
    private final boolean                                                       eventScheduling;

    // true if event buffering is is enabled...
    private final boolean                                                       bufferedEvents;

    // the thread for the event dispatcher, IF event buffering is enabled...
    private final Thread                                                        eventDispatcher;

    // the deque for event buffering, IF event buffering is enabled...
    private final BlockingDeque<FSMEvent<E>>                                    eventsBuffer;

    // the events source for this FSM...
    private final FSMEvents<S,E>                                                events;


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
        initialState        = _spec.initialState;
        fsmContext          = _spec.context;
        bufferedEvents      = _spec.bufferedEvents;
        eventScheduling     = _spec.eventScheduling;
        stateChangeListener = _spec.stateChangeListener;

        // current state is null until init() is run...
        state = null;

        // make our list of states...
        states = new ArrayList<>();
        for( FSMSpec.FSMStateSpec<S> stateSpec : _spec.stateSpecs ) {
            states.add( new FSMState<>( stateSpec.state, this, fsmContext, stateSpec.context ) );
        }

        // make our event transform lookup table...
        transforms = new ArrayList<>( _spec.eventEnums.size() );
        for( int i = 0; i < _spec.eventEnums.size(); i++ ) transforms.add( null );  // fill the list with nulls for each event...
        _spec.transforms.forEach( (event, transform) -> transforms.set( event.ordinal(), transform ) );

        // build our transition lookup table...
        transitions = new ArrayList<>( _spec.stateEnums.size() );                            // the list for the state dimension of our table...
        for( int i = 0; i < _spec.stateEnums.size(); i++ ) {                                 // for every state...
            List<FSMTransition<S,E>> byEvents = new ArrayList<>( _spec.eventEnums.size() );  // make the list for the enum dimension of our table...
            for( int j = 0; j < _spec.eventEnums.size(); j++ ) byEvents.add( null );         // fill the list with nulls for each event...
            transitions.add( byEvents );                                                     // add the by event list to our by state list...
        }

        // add the transitions to our lookup table...
        _spec.transitions.forEach( (id, spec) -> {
            FSMState<S,E> fromState = states.get( id.fromState.ordinal() );  // get the from state FSMState object...
            FSMState<S,E> toState   = states.get( spec.toState.ordinal() );  // get the to state FSMState object...
            transitions.get( id.fromState.ordinal() )
                    .set(
                            id.event.ordinal(),
                            new FSMTransition<>( this, fsmContext, fromState,id.event, spec.action, toState )
                    );
        });

        // build our on-entry action lookup table...
        onEntryActions = new ArrayList<>( _spec.stateEnums.size() );
        for( int i = 0; i < _spec.stateEnums.size(); i++ ) onEntryActions.add( null );                   // fill the list with nulls for each state...
        _spec.onEntryActions.forEach( (state, action) -> onEntryActions.set( state.ordinal(), action) ); // set any defined on-entry actions...

        // build our on-exit action lookup table...
        onExitActions = new ArrayList<>( _spec.stateEnums.size() );
        for( int i = 0; i < _spec.stateEnums.size(); i++ ) onExitActions.add( null );                    // fill the list with nulls for each state...
        _spec.onEntryActions.forEach( (state, action) -> onExitActions.set( state.ordinal(), action) );  // set any defined on-exit actions...

        // build our event action lookup table...
        eventActions = new ArrayList<>( _spec.eventEnums.size() );
        for( int i = 0; i < _spec.eventEnums.size(); i++ ) eventActions.add( null );                     // fill the list with nulls for each event...
        _spec.eventActions.forEach( (event, action) -> eventActions.set( event.ordinal(), action ) );    // set any defined event actions...

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
        ScheduledExecutor eventScheduler = null;
        if( eventScheduling ) {

            // if the spec supplied a scheduler, use it...
            if( _spec.scheduler != null )
                eventScheduler = _spec.scheduler;

            // otherwise, start up our own scheduler...
            else
                eventScheduler = new ScheduledExecutor();
        }

        // set up our events source...
        events = new FSMEvents<>( this, eventScheduler, _spec.eventEnums.get( 0 ) );
    }


    /**
     * <p>This method is the heart of the FSM - it handles state transitions and event transformations, all triggered by receiving an event.  There
     * are several things that can happen, depending on what state the FSM is currently in and what event is received:</p>
     * <ul>
     *     <li>If the event has been cancelled, then this method does nothing at all.
     *     This mechanism eliminates race conditions that might otherwise arise because the scheduled events are posted in a different thread from
     *     the one actions are run in.</li>
     *     <li>If the event has an associated {@link FSMEventAction}, run it.</li>
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

        // if the current state is null, then we need to set the initial state...
        // this only happens on the first event that this FSM sees...
        if( state == null ) {

            state = initialState;

            // if our initial state has an on-entry action, run it...
            FSMStateAction<S,E> initialOnEntry = onEntryActions.get( state.ordinal() );
            if( initialOnEntry != null )
                initialOnEntry.run( states.get( state.ordinal() ) );
        }

        // if our event is a cancellable event that has been cancelled, ignore it...
        if( _event.isCancelled() )
            return;

        // if our event has an event action associated with it, run it...
        FSMEventAction<S,E> eventAction = eventActions.get( _event.event.ordinal() );
        if( eventAction != null )
            eventAction.run( _event, states.get( state.ordinal() ) );

        // if we don't have a transition for the current state and this event, then let's see if we have a transform...
        FSMTransition<S,E> transition = transitions.get( state.ordinal() ).get( _event.event.ordinal() );
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
            FSMStateAction<S,E> onExitAction = onExitActions.get( fromState.state.ordinal() );
            if( onExitAction != null)
                onExitAction.run( fromState );

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
            FSMStateAction<S,E> onEntryAction = onEntryActions.get( toState.state.ordinal() );
            if( onEntryAction != null)
                onEntryAction.run( toState );

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
        FSMEventTransform<S,E> transform = transforms.get( _event.event.ordinal() );
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
     *     <li>If the event has been cancelled, then this method does nothing at all.
     *     This mechanism eliminates race conditions that might otherwise arise because the scheduled events are posted in a different thread from
     *     the one actions are run in.</li>
     *     <li>If the event has an associated {@link FSMEventAction}, run it.</li>
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
     * <p>This method is the heart of the FSM - it handles state transitions and event transformations, all triggered by receiving an event.  There
     * are several things that can happen, depending on what state the FSM is currently in and what event is received:</p>
     * <ul>
     *     <li>If the event has been cancelled, then this method does nothing at all.
     *     This mechanism eliminates race conditions that might otherwise arise because the scheduled events are posted in a different thread from
     *     the one actions are run in.</li>
     *     <li>If the event has an associated {@link FSMEventAction}, run it.</li>
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
     * @param _eventData The data associated with the event.
     */
    @SuppressWarnings( "unused" )
    public void onEvent( final E _event, final Object _eventData ) {
        onEvent( events.from( _event, _eventData ) );
    }


    /**
     * <p>This method is the heart of the FSM - it handles state transitions and event transformations, all triggered by receiving an event.  There
     * are several things that can happen, depending on what state the FSM is currently in and what event is received:</p>
     * <ul>
     *     <li>If the event has been cancelled, then this method does nothing at all.
     *     This mechanism eliminates race conditions that might otherwise arise because the scheduled events are posted in a different thread from
     *     the one actions are run in.</li>
     *     <li>If the event has an associated {@link FSMEventAction}, run it.</li>
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
    public void onEvent( final E _event ) {
        onEvent( events.from( _event ) );
    }


    /**
     * <p>Schedule the given {@link FSMEvent} to be handled after the given {@link Duration} delay, and return a cancellable {@link FSMEvent} instance
     * that can be used to cancel this scheduled event at any time before the event is actually handled.  If event buffering is not enabled on this
     * FSM instance, then these events will be handled (including executing actions and event transforms) in the scheduler's thread. If event
     * buffering <i>is</i> enabled on this FSM instance, then the scheduler will simply add these events to the internal event queue.  If event
     * scheduling is not enabled in this instance, an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @param _event The {@link FSMEvent} to be scheduled.
     * @param _delay The {@link Duration} delay until it is to be handled.
     * @return The {@link FSMEvent} that can be used to cancel this scheduled event.
     */
    public FSMEvent<E> scheduleEvent( final FSMEvent<E> _event, final Duration _delay ) {

        // if we don't have event scheduling enabled, complain loudly...
        if( !eventScheduling )
            throw new UnsupportedOperationException( "Event scheduling is disabled in this FSM" );

        return events.schedule( _event, _delay );
    }


    /**
     * <p>Schedule the given {@link FSMEvent} to be handled after the given {@link Duration} delay, and return a cancellable {@link FSMEvent} instance
     * that can be used to cancel this scheduled event at any time before the event is actually handled.  If event buffering is not enabled on this
     * FSM instance, then these events will be handled (including executing actions and event transforms) in the scheduler's thread. If event
     * buffering <i>is</i> enabled on this FSM instance, then the scheduler will simply add these events to the internal event queue.  If event
     * scheduling is not enabled in this instance, an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @param _event The {@link FSMEvent} to be scheduled.
     * @param _eventData The data for the event to be scheduled.
     * @param _delay The {@link Duration} delay until it is to be handled.
     * @return The {@link FSMEvent} that can be used to cancel this scheduled event.
     */
    @SuppressWarnings( "unused" )
    public FSMEvent<E> scheduleEvent( final E _event, final Object _eventData, final Duration _delay ) {

        // if we don't have event scheduling enabled, complain loudly...
        if( !eventScheduling )
            throw new UnsupportedOperationException( "Event scheduling is disabled in this FSM" );

        return events.schedule( _event, _eventData, _delay );
    }


    /**
     * <p>Schedule the given {@link FSMEvent} to be handled after the given {@link Duration} delay, and return a cancellable {@link FSMEvent} instance
     * that can be used to cancel this scheduled event at any time before the event is actually handled.  If event buffering is not enabled on this
     * FSM instance, then these events will be handled (including executing actions and event transforms) in the scheduler's thread. If event
     * buffering <i>is</i> enabled on this FSM instance, then the scheduler will simply add these events to the internal event queue.  If event
     * scheduling is not enabled in this instance, an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @param _event The event enum for the event to be scheduled.
     * @param _delay The {@link Duration} delay until it is to be handled.
     * @return The {@link FSMEvent} that can be used to cancel this scheduled event.
     */
    public FSMEvent<E> scheduleEvent( final E _event, final Duration _delay ) {

        // if we don't have event scheduling enabled, complain loudly...
        if( !eventScheduling )
            throw new UnsupportedOperationException( "Event scheduling is disabled in this FSM" );

        return events.schedule( _event, _delay );
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
     * Returns an {@link FSMEvent} instance created from the given event enum.
     *
     * @param _event The event enum to create an {@link FSMEvent} instance from.
     * @return the {@link FSMEvent} instance created
     */
    public FSMEvent<E> event( final E _event ) {
        return events.from( _event );
    }


    /**
     * Returns an {@link FSMEvent} instance created from the given event enum and event data (an arbitrary {@link Object} instance.
     *
     * @param _event The event enum to create an {@link FSMEvent} instance from.
     * @param _data The data for the event.
     * @return the {@link FSMEvent} instance created
     */
    public FSMEvent<E> event( final E _event, final Object _data ) {
        return events.from( _event, _data );
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
