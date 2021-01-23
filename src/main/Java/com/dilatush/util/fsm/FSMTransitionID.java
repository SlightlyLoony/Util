package com.dilatush.util.fsm;

import java.util.Objects;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMTransitionID<S extends Enum<S>, E extends Enum<E>> {
    public final S fromState;
    public final E event;


    public FSMTransitionID( final S _fromState, final E _event ) {
        fromState = _fromState;
        event = _event;
    }


    @Override
    public boolean equals( final Object _o ) {
        if( this == _o ) return true;
        if( _o == null || getClass() != _o.getClass() ) return false;
        FSMTransitionID<?, ?> defIndex = (FSMTransitionID<?, ?>) _o;
        return fromState.equals( defIndex.fromState ) && event.equals( defIndex.event );
    }


    @Override
    public int hashCode() {
        return Objects.hash( fromState, event );
    }
}
