package com.dilatush.util.fsm;

import java.util.*;
import java.util.function.Consumer;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class provide the specifications for constructing an instance of {@link FSM}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMSpec<S extends Enum<S>,E extends Enum<E>> {

    private static final int DEFAULT_MAX_BUFFERED_EVENTS = 100;

    /*package-private*/ S                                                             initialState;
    /*package-private*/ FSMStateContext[]                                             stateContexts;
    /*package-private*/ Map<FSMTransitionID<S,E>, FSMTransition<FSMAction<S,E>,S,E>>  transitions;
    /*package-private*/ Map<E,FSMEventTransform<S,E>>                                 transforms;
    /*package-private*/ Object                                                        context;
    /*package-private*/ boolean                                                       eventScheduling;
    /*package-private*/ boolean                                                       bufferedEvents;
    /*package-private*/ int                                                           maxBufferedEvents = DEFAULT_MAX_BUFFERED_EVENTS;
    /*package-private*/ Consumer<S>                                                   stateChangeListener;


    private final List<String>                 errorMessages;
    private final List<FSMTransitionDef<S,E>>  defs;

    private final List<E> events;
    private final List<S> states;


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

        initialState  = _initialState;
        transitions   = new HashMap<>();
        transforms    = new HashMap<>();
        errorMessages = new ArrayList<>();
        defs          = new ArrayList<>();

        events = getEventValues( _example );
        states = getStateValues( _initialState );

        // set up the default state contexts...
        stateContexts = new FSMStateContext[states.size()];
        for( int i = 0; i < stateContexts.length; i++ )
            stateContexts[i] = new FSMStateContext();
    }


    /**
     * Add a new FSM state transition definition to this FSM specification.  Each transition defines one allowed transition between two FSM states
     * (the "from state" and the "to state") when a particular FSM event occurs, along with the optional FSM action that will be executed on that
     * transition.  Together with the enumerated FSM states and FSM events, the FSM state transitions define all of the possible states of the FSM
     * and all the possible ways to transition from one state to another.
     *
     * @param _fromState The FSM state being transitioned from.
     * @param _event The FSM event that triggers the transition.
     * @param _action The optional FSM action ({@link FSMAction} to execute on the transition; this may be {@code null} for no action.
     * @param _toState The FSM state being transitioned to.
     */
    public void addTransition( final S _fromState, final E _event, final FSMAction<S,E> _action, final S _toState ) {

        FSMTransitionDef<S,E> def = new FSMTransitionDef<>( _fromState, _event, _action, _toState );
        defs.add( def );
    }


    /**
     * <p>Set the optional global FSM context, which may be an object of any type.  This global context defaults to {@code null} if it is not set, and
     * it is not used with {@link FSM} at all, other than to pass it into various contexts.  It exists for the convenience of code using the FSM, for
     * state or methods that may be widely used with FSM-related code, especially {@link FSMAction}s or {@link FSMEventTransform}s.</p>
     * <p>In general there are several mechanisms for sharing context within an FSM implementation, and each of them is useful in different
     * circumstances.  Those mechanisms are:</p>
     * <ul>
     *     <li>The standard Java mechanisms, such as the fields of the class incorporating the FSM.</li>
     *     <li>The properties of the FSM, either global or FSM state-specific.</li>
     *     <li>The global FSM context.</li>
     *     <li>The FSM state contexts.</li>
     * </ul>
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
     * Enable event scheduling in the FSM, including FSM state timeouts.  Event scheduling is disabled by default.  When event scheduling is enabled,
     * an additional thread is created to handle the scheduling.
     */
    public void enableEventScheduling() {
        eventScheduling = true;
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
     * Set the initial FSM state context for the given state to the given context.  By default, new instances of {@link FSMStateContext} are
     * automatically set for each FSM state.  This method need only be called when a subclass of {@link FSMStateContext} is needed for a particular
     * FSM state.
     *
     * @param _state The FSM state to set an FSM state context for.
     * @param _context The FSM state context to set.
     */
    public void setStateContext( final S _state, final FSMStateContext _context ) {
        stateContexts[_state.ordinal()] = _context;
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
     *     <li>The same FSM state context instance is used for multiple states.</li>
     * </ul>
     *
     * @return {@code true} if this FSM specification is valid.
     */
    public boolean isValid() {

        // clear any error messages...
        errorMessages.clear();

        // iterate over all the transition definitions to map all our transitions...
        for( FSMTransitionDef<S,E> def : defs ) {
            FSMTransitionID<S,E> index = new FSMTransitionID<>( def.fromState, def.onEvent );
            Object checker = transitions.get( index );
            if( checker != null ) {
                errorMessages.add( "   Duplicate transition ID: " + index.toString() );
                continue;
            }
            transitions.put( index, new FSMTransition<>( def.action, def.toState ) );
        }

        // see what states we have no transitions from...
        Set<S> fromStates = new HashSet<>( states );
        for( FSMTransitionID<S,E> def : transitions.keySet() ) {
            fromStates.remove( def.fromState );
        }
        for( S state : fromStates ) {
            errorMessages.add( "   No transition from state: " + state.toString() );
        }

        // see what states we have no transitions to...
        Set<S> toStates = new HashSet<>( states );
        for( FSMTransition<FSMAction<S,E>,S,E> transition : transitions.values() ) {
            toStates.remove( transition.toState );
        }
        for( S state : toStates ) {
            errorMessages.add( "   No transition to state: " + state.toString() );
        }

        // see what events we don't use...
        Set<E> usedEvents = new HashSet<>( events );
        for( FSMTransitionID<S,E> def : transitions.keySet() ) {
            usedEvents.remove( def.event );
        }
        for( E event : transforms.keySet() ) {
            usedEvents.remove( event );
        }
        for( E event : usedEvents ) {
            errorMessages.add( "   Event never used: " + event.toString() );
        }

        // make sure we haven't the same state context in multiple states...
        for( int i = 0; i < stateContexts.length - 1; i++ ) {
            for( int j = i + 1; j < stateContexts.length; j++ ) {
                if( stateContexts[i] == stateContexts[j] ) {
                    errorMessages.add( "Same state context used for both " + states.get( i ).toString() + " and " + states.get( j ).toString() );
                }
            }
        }

        return errorMessages.size() == 0;
    }


    /**
     * Returns a list containing all of the FSM state values.
     *
     * @param _state An example FSM state.  The concrete example is necessary so that this method can use the {@code _state.getClass()} method,
     *               which is not available from just the type name.
     * @return a list containing all of the FSM state values
     */
    @SuppressWarnings( "unchecked" )
    private List<S> getStateValues( final S _state ) {
        return Arrays.asList( ((Class<S>) _state.getClass()).getEnumConstants() );
    }


    /**
     * Returns a list containing all of the FSM event values.
     *
     * @param _event An example FSM event.  The concrete example is necessary so that this method can use the {@code _event.getClass()} method,
     *               which is not available from just the type name.
     * @return a list containing all of the FSM event values
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
        private final FSMAction<S,E> action;
        private final S toState;


        private FSMTransitionDef( final S _fromState, final E _onEvent, final FSMAction<S,E> _action, final S _toState ) {

            // fail fast if any required arguments are missing...
            if( isNull( _fromState, _onEvent, _toState ) )
                throw new IllegalArgumentException( "From state, on event, or to state is null" );

            fromState = _fromState;
            onEvent = _onEvent;
            action = _action;
            toState = _toState;
        }
    }
}
