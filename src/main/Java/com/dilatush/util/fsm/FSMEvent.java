package com.dilatush.util.fsm;

import java.util.Objects;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent FSM events.  Each event <i>must</i> include an event {@code Enum} that indicates the kind of event it is, and
 * <i>may</i> include an arbitrary data object with more information about the event.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMEvent<E extends Enum<E>> {

    public final E      event;
    public final Object data;


    /**
     * Create a new instance of this class with the given event type and optional data object.
     *
     * @param _event The event type {@code Enum} for this event.
     * @param _data The optional data object for this event, which may be {@code null}.
     */
    public FSMEvent( final E _event, final Object _data ) {

        // fail fast if we didn't get an event...
        if( isNull( _event) )
            throw new IllegalArgumentException( "No Enum for FSMEvent" );

        event = _event;
        data = _data;
    }


    /**
     * Create a new instance of this class with the given event type and no optional data object.
     *
     * @param _event The event type {@code Enum} for this event.
     */
    public FSMEvent( final E _event ) {
        this( _event, null );
    }


    /**
     * Return a string representing this instance.  If the event has no data object, then this method returns the result of the {@code toString()}
     * method on the type {@code Enum}.  Otherwise this method returns the the result of the {@code toString()} method on the type {@code Enum},
     * followed by a space, and finally the result of the {@code toString()} method on the data object.
     *
     * @return a string representing this instance
     */
    @Override
    public String toString() {
        return (data == null) ? event.toString() : event.toString() + " " + data.toString();
    }


    /**
     * Returns {@code true} if this instance has the same value as the given object.
     *
     * @param _obj The object to compare with this instance.
     * @return {@code true}  if this instance has the same value as the given object
     */
    @Override
    public boolean equals( final Object _obj ) {
        if( this == _obj ) return true;
        if( _obj == null || getClass() != _obj.getClass() ) return false;
        FSMEvent<?> fsmEvent = (FSMEvent<?>) _obj;
        return event.equals( fsmEvent.event ) && Objects.equals( data, fsmEvent.data );
    }


    /**
     * Returns the hash code for this instance.
     *
     * @return the hash code for this instance
     */
    @Override
    public int hashCode() {
        return Objects.hash( event, data );
    }
}
