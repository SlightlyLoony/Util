package com.dilatush.util.fsm;

import java.util.Objects;

import static com.dilatush.util.General.isNull;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMEvent<E extends Enum<E>> {

    public final E      event;
    public final Object data;


    public FSMEvent( final E _event, final Object _data ) {

        // fail fast if we didn't get an event...
        if( isNull( _event) )
            throw new IllegalArgumentException( "No enum for FSMEvent" );

        event = _event;
        data = _data;
    }


    public FSMEvent( final E _event ) {
        this( _event, null );
    }


    @Override
    public String toString() {
        return (data == null) ? event.toString() : event.toString() + " " + data.toString();
    }


    @Override
    public boolean equals( final Object _o ) {
        if( this == _o ) return true;
        if( _o == null || getClass() != _o.getClass() ) return false;
        FSMEvent<?> fsmEvent = (FSMEvent<?>) _o;
        return event.equals( fsmEvent.event ) && Objects.equals( data, fsmEvent.data );
    }


    @Override
    public int hashCode() {
        return Objects.hash( event, data );
    }
}
