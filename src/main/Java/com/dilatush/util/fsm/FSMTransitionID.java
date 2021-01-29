package com.dilatush.util.fsm;

import java.util.Objects;

/**
 * A simple POJO that contains the triggering FSM event and FSM state before the FSM state transition (the "from" state).  It is used internally by
 * {@link FSM} and {@link FSMSpec}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/*package-private*/ class FSMTransitionID<S extends Enum<S>, E extends Enum<E>> {

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
