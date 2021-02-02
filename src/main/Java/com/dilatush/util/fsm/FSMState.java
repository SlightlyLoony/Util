package com.dilatush.util.fsm;

import com.dilatush.util.fsm.events.FSMEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements an FSM state.  The FSM maintains four statistics values in the state: the total time spent in the state, the number of entries
 * to the state, the last time the state was entered, and the last time the state was exited.  These values are accessible outside the FSM (especially
 * from actions), but mutable only within it.  This class maintains the state-specific properties (in a map), the optional state-specific context
 * objects, and stores the cancellable events for timeouts, if they have been set.  It also holds references to the optional on-entry and on-exit
 * state actions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public final class FSMState<S extends Enum<S>,E extends Enum<E>> {

    // these mutable values are set by FSM...
    private Duration               timeInState = Duration.ZERO;  // the total time spent in this state...
    private Instant                lastEntered = null;           // time this state was most recently entered...
    private Instant                lastLeft    = null;           // time this state was most recently left...
    private long                   entries     = 0;              // the total number of times this state has been entered...

    // these mutable values are maintained by this class...
    private Map<String,Object>     properties  = null;           // the state-specific property map, if any properties have been set...
    private FSMEvent<?>            timeout     = null;           // the timeout, if one has been set...

    // these immutable values are set at instantiation, but are accessible publicly...


    /**
     * The FSM state enum.
     */
    public final S        state;


    /**
     * The {@link FSM} instance associated with this transition.
     */
    public final FSM<S,E> fsm;


    /**
     * The optional FSM global context.
     */
    public final Object   fsmContext;


    /**
     * The optional FSM state context.
     */
    public final Object   context;


    /**
     * Create a new instance of this class with the given values.
     *
     * @param _state The FSM state enum for this state.
     * @param _fsm The FSM instance associated with this state.
     * @param _fsmContext The FSM global context for the FSM associated with this state.
     * @param _context The optional FSM state context.
     */
    /*package-private*/ FSMState( final S _state, final FSM<S, E> _fsm, final Object _fsmContext, final Object _context ) {

        state = _state;
        fsm = _fsm;
        fsmContext = _fsmContext;
        context = _context;
    }


    /**
     * Set the time-in-state for this FSM state.
     *
     * @param _timeInState The time-in-state for this FSM state.
     */
    /*package-private*/ void setTimeInState( final Duration _timeInState ) {
        timeInState = _timeInState;
    }


    /**
     * Set the time that this state was last entered.
     *
     * @param _lastEntered The time that this state was last entered.
     */
    /*package-private*/ void setLastEntered( final Instant _lastEntered ) {
        lastEntered = _lastEntered;
    }


    /**
     * Set the time that this state was last left (exited).
     *
     * @param _lastLeft The time that this state was last left (exited).
     */
    /*package-private*/ void setLastLeft( final Instant _lastLeft ) {
        lastLeft = _lastLeft;
    }


    /**
     * Set the number of times this state has been entered.
     *
     * @param _entries The number of times this state has been entered.
     */
    /*package-private*/ void setEntries( final long _entries ) {
        entries = _entries;
    }


    /**
     * Sets the {@link FSMEvent} representing a timeout for this state.
     *
     * @param _timeout The {@link FSMEvent} representing a timeout for this state.
     */
    /*package-private*/ void setTimeout( FSMEvent<?> _timeout ) {
        timeout = _timeout;
    }


    /**
     * Cancel the timeout for this state, if there is one.
     */
    /*package-private*/ void cancelTimeout() {
        if( timeout != null ) {
            timeout.cancel();
            timeout = null;
        }
    }


    /**
     * Set the state-specific FSM property with the given name to the given value (which may be {@code null}).
     *
     * @param _name The name of the state-specific FSM property to be set.
     * @param _value The value to set the state-specific FSM property to.
     */
    public void setProperty( final String _name, final Object _value ) {

        // fail fast if our name is invalid...
        if( isEmpty( _name ) )
            throw new IllegalArgumentException( "No property name" );

        // if this is the first property we've ever set, then we must first instantiate the map...
        if( properties == null )
            properties = new HashMap<>();

        // set the property's value...
        properties.put( _name, _value );
    }


    /**
     * Return the value of the state-specific FSM property with the given name, or {@code null} if there is no state-specific FSM property with the
     * given name.
     *
     * @param _name The name of the state-specific FSM property to retrieve.
     * @return the value of the named state-specific FSM property, {@code null} if the named property does not exist
     */
    public Object getProperty( final String _name ) {

        // fail fast if our name is invalid...
        if( isEmpty( _name ) )
            throw new IllegalArgumentException( "No property name" );

        // if we've never set a property, just return null...
        if( properties == null )
            return null;

        // otherwise, return the value we find, or null if there was no such property...
        return properties.get( _name );
    }


    /**
     * Returns the total time (as a {@link Duration} instance) that the FSM has been in this state.
     *
     * @return the total time that the FSM has been in this state
     */
    public Duration getTimeInState() {

        // if we've never entered this state, then zero is the answer...
        if( lastEntered == null )
            return Duration.ZERO;

        // if we've never left this state, then the answer is from entry time to now...
        if( lastLeft == null )
            return Duration.between( lastEntered, Instant.now() );

        // if lastLeft is after lastEntered, then we're currently in some other state, and the stored duration is correct...
        if( lastLeft.isAfter( lastEntered ) )
            return timeInState;

        // otherwise, we're currently in this state, and we have to add the stored duration to the current time in state to get the right answer...
        // if we're currently in this state, we have to add the time since the last entry to the sum we've stored...
        return timeInState.plus( Duration.between( lastEntered, Instant.now() ) );
    }


    /**
     * Returns the time (as an {@link Instant} instance) that this state was last entered, or {@code null} if the FSM has never entered this state.
     *
     * @return the time this state was last entered
     */
    public Instant getLastEntered() {
        return lastEntered;
    }


    /**
     * Returns the time (as an {@link Instant} instance) that this state was last left, or {@code null} if the FSM has never left this state.  Note
     * that if the FSM is currently <i>in</i> this state, then the time left will be from the previous occasion that the FSM was in this state, which
     * would be before the time it last entered this state. Read this three times so you can be <i>really</i> confused!
     *
     * @return the time this state was last left
     */
    public Instant getLastLeft() {
        return lastLeft;
    }


    /**
     * Returns the number of times the FSM has entered this state.
     *
     * @return the number of times the FSM has entered this state
     */
    public long getEntries() {
        return entries;
    }
}
