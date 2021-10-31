package com.dilatush.util.fsm;

import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.fsm.events.FSMEvent;

import java.util.*;
import java.util.function.Consumer;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class provide the specifications for constructing an instance of {@link FSM}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class FSMSpec<S extends Enum<S>,E extends Enum<E>> {

    private static final int DEFAULT_MAX_BUFFERED_EVENTS = 100;

    /*package-private*/ S                                                                           initialState;
    /*package-private*/ Map<FSMTransitionID<S,E>, FSMTransitionSpec<FSMTransitionAction<S,E>,S,E>>  transitions;
    /*package-private*/ Map<E,FSMEventTransform<S,E>>                                               transforms;
    /*package-private*/ Object                                                                      context;
    /*package-private*/ boolean                                                                     eventScheduling;
    /*package-private*/ boolean                                                                     bufferedEvents;
    /*package-private*/ int                                                                         maxBufferedEvents = DEFAULT_MAX_BUFFERED_EVENTS;
    /*package-private*/ Consumer<S>                                                                 stateChangeListener;
    /*package-private*/ Consumer<FSMEvent<E>>                                                       eventListener;
    /*package-private*/ List<FSMStateSpec<S>>                                                       stateSpecs;
    /*package-private*/ final List<E>                                                               eventEnums;
    /*package-private*/ final List<S>                                                               stateEnums;
    /*package-private*/ final Map<S,FSMStateAction<S,E>>                                            onEntryActions;
    /*package-private*/ final Map<S,FSMStateAction<S,E>>                                            onExitActions;
    /*package-private*/ final Set<S>                                                                terminals;
    /*package-private*/ final Map<E,FSMEventAction<S,E>>                                            eventActions;
    /*package-private*/ ScheduledExecutor scheduler;


    private final List<String>                 errorMessages;
    private final List<FSMTransitionDef<S,E>>  defs;



    /**
     * Create a new instance of this class with the given initial FSM state and example FSM event.  The initial FSM state determines the state of the
     * FSM immediately following instantiation.  The example FSM event has no effect on the instantiated FSM; it is needed only to allow the generic
     * class access to the concrete events.
     *
     * @param _initialState The initial state for the FSM.
     * @param _example An example event; it makes no difference which one.
     */
    public FSMSpec( final S _initialState, final E _example ) {

        // fail fast if we don't have our arguments...
        if( isNull( _initialState, _example ) )
            throw new IllegalArgumentException( "Missing initial state or example event" );

        initialState   = _initialState;
        transitions    = new HashMap<>();
        transforms     = new HashMap<>();
        onEntryActions = new HashMap<>();
        onExitActions  = new HashMap<>();
        terminals      = new HashSet<>();
        eventActions   = new HashMap<>();
        errorMessages  = new ArrayList<>();
        defs           = new ArrayList<>();
        stateSpecs     = new ArrayList<>();

        eventEnums = getEventValues( _example );
        stateEnums = getStateValues( _initialState );

        // load up the state specs with their default values...
        for( S stateEnum : stateEnums ) {
            stateSpecs.add( new FSMStateSpec<>( stateEnum ) );
        }
    }


    /**
     * Add a new FSM state transition definition to this FSM specification.  Each transition defines one allowed transition between two FSM states
     * (the "from state" and the "to state") when a particular FSM event occurs, along with the optional FSM action that will be executed on that
     * transition.  Together with the enumerated FSM states and FSM events, the FSM state transitions define all the possible states of the FSM
     * and all the possible ways to transition from one state to another.
     *
     * @param _fromState The FSM state being transitioned from.
     * @param _event The FSM event that triggers the transition.
     * @param _action The optional FSM action ({@link FSMTransitionAction} to execute on the transition; this may be {@code null} for no action.
     * @param _toState The FSM state being transitioned to.
     */
    public void addTransition( final S _fromState, final E _event, final FSMTransitionAction<S,E> _action, final S _toState ) {

        FSMTransitionDef<S,E> def = new FSMTransitionDef<>( _fromState, _event, _action, _toState );
        defs.add( def );
    }


    /**
     * <p>Set the optional global FSM context, which may be an object of any type.  This global context defaults to {@code null} if it is not set, and
     * it is not used with {@link FSM} at all, other than to make it available to actions, transforms, and any code with access to the FSM.</p>
     *
     * @param _fsmContext The optional global FSM context.
     */
    public void setFSMContext( final Object _fsmContext ) {
        context = _fsmContext;
    }


    /**
     * Sets the optional state change listener for this FSM.  If provided, the listener will be called for every state change of this FSM.  The
     * listener could be called from multiple threads, and it must not block or consume significant CPU time.  What constitutes "significant" is of
     * course completely application dependent.
     *
     * @param _listener the state change listener for this FSM
     */
    @SuppressWarnings( "unused" )
    public void setStateChangeListener( final Consumer<S> _listener ) {
        stateChangeListener = _listener;
    }


    /**
     * Sets the optional event listener for this FSM.  If provided, the listener will be called for every event processed by this FSM.  The listener
     * could be called from multiple threads, and it must not block or consume significant CPU time.  What constitutes "significant" is of course
     * completely application dependent.
     *
     * @param _eventListener the event listener for this FSM
     */
    @SuppressWarnings( "unused" )
    public void setEventListener( final Consumer<FSMEvent<E>> _eventListener ) {
        eventListener = _eventListener;
    }


    /**
     * Enable event scheduling in the FSM, including FSM state timeouts.  Event scheduling is disabled by default.  When event scheduling is enabled
     * with this method, an additional thread is created in the FSM to handle the scheduling.
     */
    public void enableEventScheduling() {
        eventScheduling = true;
    }


    /**
     * Enable event scheduling in this FSM, including state timeouts, and use the given scheduler to do the scheduling.  Event scheduling is disabled
     * by default.  If enabled by this method, there will not be an additional thread created in the FSM for scheduling.
     *
     * @param _scheduler The {@link ScheduledExecutor} to use for event scheduling.
     */
    @SuppressWarnings( "unused" )
    public void enableEventScheduling( final ScheduledExecutor _scheduler ) {

        // fail fast if we got a null...
        if( _scheduler == null )
            throw new IllegalArgumentException( "Missing scheduler" );

        eventScheduling = true;
        scheduler = _scheduler;
    }


    /**
     * Enable buffered events in the FSM.  Buffered events are disabled by default.  When buffered events are enabled, the FSM's {@code onEvent()}
     * methods will queue events in a FIFO buffer, and a separate thread (created for the purpose) dequeues events from the FIFO buffer and handles
     * them.  When buffered events are not enabled, the FSM's {@code onEvent()} methods are synchronized and handle events sequentially in the
     * caller's thread.
     */
    public void enableBufferedEvents() {
        bufferedEvents = true;
    }


    /**
     * Set the maximum number of events that may be buffered, when buffered events are enabled.  The default value is 100.  If the maximum number
     * of buffered events is exceeded, the FSM's {@code onEvent()} methods will throw an {@link IllegalStateException}.
     *
     * @param _max The maximum number of events that may be buffered, when buffered events are enabled.
     */
    @SuppressWarnings( "unused" )
    public void setMaxBufferedEvents( final int _max ) {
        maxBufferedEvents = _max;
    }


    /**
     * Set the optional state context object for the given FSM state to the given value.
     *
     * @param _state The FSM state to set a state context for.
     * @param _context The state context object to set.
     */
    public void setStateContext( final S _state, final Object _context ) {
        stateSpecs.get( _state.ordinal() ).context = _context;
    }


    /**
     * Set the optional state on-entry action for the given FSM state to the given {@link FSMStateAction} implementation.  A state on-entry action is
     * run on an FSM event that causes a transition from one FSM state to another is being handled - transitions that don't involve a state change will <i>not</i> cause the
     * on-entry action to be run.  The on-entry action for the state being entered is run after the on-exit action (if there is one) for the state being left, and after the
     * transition action (if there is one).
     *
     * @param _state The FSM state to set a state on-entry action for.
     * @param _action The FSM state action to set.
     */
    public void setStateOnEntryAction( final S _state, final FSMStateAction<S,E> _action ) {
        onEntryActions.put( _state, _action );
    }


    /**
     * Set the optional state on-exit action for the given FSM state to the given {@link FSMStateAction} implementation.  A state on-exit action is
     * run while an FSM event that causes a transition from one FSM state to another is being handled - transitions that don't involve a state change will <i>not</i> cause the
     * on-exit action to be run.  The on-exit action for the state being left is run before the on-entry action (if there is one) for the state being entered, and before the
     * transition action (if there is one).
     *
     * @param _state The FSM state to set a state on-entry action for.
     * @param _action The FSM state action to set.
     */
    @SuppressWarnings( "unused" )
    public void setStateOnExitAction( final S _state, final FSMStateAction<S,E> _action ) {
        onExitActions.put( _state, _action );
    }


    /**
     * Sets the given FSM state as a terminal state.
     *
     * @param _state The FSM state to set as a terminal state.
     */
    public void setStateTerminal( final S _state ) {
        terminals.add( _state );
    }


    /**
     * Set the optional event action for the given FSM event to the given {@link FSMEventAction} implementation.  An event action is run when a
     * matching event is handled.
     *
     * @param _event The FSM event to set an event action for.
     * @param _action The FSM event action to set.
     */
    @SuppressWarnings( "unused" )
    public void setEventAction( final E _event, final FSMEventAction<S,E> _action ) {
        eventActions.put( _event, _action );
    }


    /**
     * Add the given FSM event transform for the given FSM event.  See {@link FSMEventTransform} for details on FSM event transforms.
     *
     * @param _event The {@link FSMEvent} to be transformed via the given FSM event transform.
     * @param _transform The {@link FSMEventTransform} for the given FSM event.
     */
    public void addEventTransform( final E _event, final FSMEventTransform<S,E> _transform ) {
        transforms.put( _event, _transform );
    }


    /**
     * Returns {@code true} if this FSM specification is valid.  The specification is invalid if any of the following conditions exist:
     * <ul>
     *     <li>More than one FSM state transition has the same FSM transition ID (the combination of current FSM state and triggering FSM event).</li>
     *     <li>There are no FSM state transitions defined <i>from</i> a particular FSM state.</li>
     *     <li>There are no FSM state transitions defined <i>to</i> a particular FSM state.</li>
     *     <li>There are FSM events that are not part of any FSM state transition.</li>
     * </ul>
     *
     * @return {@code true} if this FSM specification is valid.
     */
    public boolean isValid() {

        // clear any error messages and our transitions...
        errorMessages.clear();
        transitions.clear();

        // iterate over all the transition definitions to map all our transitions...
        for( FSMTransitionDef<S,E> def : defs ) {
            FSMTransitionID<S,E> index = new FSMTransitionID<>( def.fromState, def.onEvent );
            Object checker = transitions.get( index );
            if( checker != null ) {
                errorMessages.add( "   Duplicate transition ID: " + index );
                continue;
            }
            transitions.put( index, new FSMTransitionSpec<>( def.action, def.toState ) );
        }

        // see what non-terminal states we have no transitions from...
        Set<S> fromStates = new HashSet<>( stateEnums );
        for( FSMTransitionID<S,E> def : transitions.keySet() ) {
            fromStates.remove( def.fromState );
        }
        for( S state : fromStates ) {
            if( !terminals.contains( state ) )
                errorMessages.add( "   No transition from non-terminal state: " + state.toString() );
        }

        // see what terminal states we do have transitions from...
        fromStates = new HashSet<>();
        for( FSMTransitionID<S,E> def : transitions.keySet() ) {
            fromStates.add( def.fromState );
        }
        for( S state : fromStates ) {
            if( terminals.contains( state ) )
                errorMessages.add( "   Transition from terminal state: " + state.toString() );
        }

        // see what states we have no transitions to...
        Set<S> toStates = new HashSet<>( stateEnums );
        for( FSMTransitionSpec<FSMTransitionAction<S,E>,S,E> transition : transitions.values() ) {
            toStates.remove( transition.toState );
        }
        for( S state : toStates ) {
            if( state != initialState )
                errorMessages.add( "   No transition to state: " + state.toString() );
        }

        // see what events we don't use...
        Set<E> usedEvents = new HashSet<>( eventEnums );
        for( FSMTransitionID<S,E> def : transitions.keySet() ) {
            usedEvents.remove( def.event );
        }
        for( E event : transforms.keySet() ) {
            usedEvents.remove( event );
        }
        for( E event : usedEvents ) {
            errorMessages.add( "   Event never used: " + event.toString() );
        }

        return errorMessages.size() == 0;
    }


    /**
     * Returns a list containing all the FSM state values.
     *
     * @param _state An example FSM state.  The concrete example is necessary so that this method can use the {@code _state.getClass()} method,
     *               which is not available from just the type name.
     * @return a list containing all the FSM state values
     */
    @SuppressWarnings( "unchecked" )
    private List<S> getStateValues( final S _state ) {
        return Arrays.asList( ((Class<S>) _state.getClass()).getEnumConstants() );
    }


    /**
     * Returns a list containing all the FSM event values.
     *
     * @param _event An example FSM event.  The concrete example is necessary so that this method can use the {@code _event.getClass()} method,
     *               which is not available from just the type name.
     * @return a list containing all the FSM event values
     */
    @SuppressWarnings( "unchecked" )
    private List<E> getEventValues( final E _event ) {
        return Arrays.asList( ((Class<E>) _event.getClass()).getEnumConstants() );
    }


    /**
     * Returns a single string with explanatory messages about any problems found during validation (i.e., {@link #isValid()} returned {@code false}),
     * or an empty string if validation has not yet been run, or if there were no problems found during validation (i.e., {@link #isValid()} returned
     * {@code true}).
     *
     * @return a single string with explanatory messages about any problems found during validation
     */
    public String getErrorMessage() {
        return String.join( "\n", errorMessages );
    }


    /**
     * A simple POJO that contains a complete definition of an FSM state transition.
     */
    private static class FSMTransitionDef<S extends Enum<S>, E extends Enum<E>> {

        private final S fromState;
        private final E onEvent;
        private final FSMTransitionAction<S,E> action;
        private final S toState;


        /**
         * Create a new instance of this class with the given values.
         *
         * @param _fromState The state being left.
         * @param _onEvent The triggering event.
         * @param _action The optional transition action.
         * @param _toState The state being entered.
         */
        private FSMTransitionDef( final S _fromState, final E _onEvent, final FSMTransitionAction<S,E> _action, final S _toState ) {

            // fail fast if any required arguments are missing...
            if( isNull( _fromState, _onEvent, _toState ) )
                throw new IllegalArgumentException( "From state, on event, or to state is null" );

            fromState = _fromState;
            onEvent = _onEvent;
            action = _action;
            toState = _toState;
        }
    }


    /**
     * A simple POJO that allows specification of the optional attributes of an FSM state.
     */
    /*package-private*/ static class FSMStateSpec<S extends Enum<S>> {

        /*package-private*/ S                   state;
        /*package-private*/ Object              context;
        /*package-private*/ boolean             terminal;


        /**
         * Create a new instance of this class for the given FSM state.
         *
         * @param _state The FSM state for this instance.
         */
        public FSMStateSpec( final S _state ) {
            state = _state;
        }
    }


    /**
     * A simple POJO that contains the {@link FSMTransitionAction} and FSM state after the FSM state transition (the "to" state).  It is used
     * internally by {@link FSM} and {@link FSMSpec}.
     *
     * @author Tom Dilatush  tom@dilatush.com
     */
    /*package-private*/ static class FSMTransitionSpec<A extends FSMTransitionAction<S,E>, S extends Enum<S>, E extends Enum<E>> {


        /*package-private*/ final S toState;
        /*package-private*/ final A action;


        /**
         * Creates a new instance of this class with the given FSM action and FSM state.
         *
         * @param _action The FSM action for this FSM transition.
         * @param _toState The FSM state for this FSM transition.
         */
        /*package-private*/ FSMTransitionSpec( final A _action, final S _toState ) {
            toState = _toState;
            action  = _action;
        }
    }

    /**
     * A simple POJO that contains the triggering FSM event and FSM state before the FSM state transition (the "from" state).  It is used internally
     * by {@link FSM} and {@link FSMSpec}.
     *
     * @author Tom Dilatush  tom@dilatush.com
     */
    /*package-private*/ static class FSMTransitionID<S extends Enum<S>, E extends Enum<E>> {

        /*package-private*/ final S fromState;
        /*package-private*/ final E event;


        /**
         * Create a new instance of this class with the given FSM state and FSM event.
         *
         * @param _fromState The FSM state for this FSM transition ID.
         * @param _event The FSM event for this FSM transition ID.
         */
        /*package-private*/ FSMTransitionID( final S _fromState, final E _event ) {
            fromState = _fromState;
            event = _event;
        }


        /**
         * Returns a string representation of this instance.
         *
         * @return a string representation of this instance
         */
        @Override
        public String toString() {
            return "(" + fromState.toString() + "/" + event.toString() + ")";
        }


        /**
         * Returns {@code true} if this instance has the same value as the given object.
         *
         * @param _obj The object to compare with this instance.
         * @return {@code true} if this instance has the same value as the given object
         */
        @Override
        public boolean equals( final Object _obj ) {
            if( this == _obj ) return true;
            if( _obj == null || getClass() != _obj.getClass() ) return false;
            FSMTransitionID<?, ?> defIndex = (FSMTransitionID<?, ?>) _obj;
            return fromState.equals( defIndex.fromState ) && event.equals( defIndex.event );
        }


        /**
         * Returns the hash code for this instance.
         *
         * @return the hash code for this instance
         */
        @Override
        public int hashCode() {
            return Objects.hash( fromState, event );
        }
    }
}
