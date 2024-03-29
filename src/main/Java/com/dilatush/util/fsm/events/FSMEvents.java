package com.dilatush.util.fsm.events;

import com.dilatush.util.ScheduledExecutor;
import com.dilatush.util.fsm.FSM;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Instances of this class create {@link FSMEvent} instances from the event's identifying enum and optional associated data, and also schedule
 * events.  The simplest kinds of events have no associated data and are not cancellable scheduled events; these can be reused so instead of
 * instantiating them when needed, they are provided from a cache.  These simple events are by far the most common events in most FSM implementations,
 * so this cache can save a lot of instantiation.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMEvents<S extends Enum<S>,E extends Enum<E>> {

    // the cache of simple events, indexed by event ordinal...
    private final List<FSMSimpleEvent<E>> simpleEventCache;

    // the scheduled executor for scheduled events, IF event scheduling is enabled...
    private final ScheduledExecutor eventScheduler;

    // the FSM this instance is associated with...
    private final FSM<S,E> fsm;


    /**
     * Create a new instance of this class with the given associated {@link FSM}, event scheduler, and sample event (which is used only to generate
     * a list of all the events).
     *
     * @param _fsm The FSM associated with this instance.
     * @param _eventScheduler The event scheduler for this instance (can be {@code null} if scheduled events are not configured).
     * @param _event A sample event.
     */
    public FSMEvents( final FSM<S,E> _fsm, final ScheduledExecutor _eventScheduler, final E _event ) {

        fsm = _fsm;
        eventScheduler = _eventScheduler;

        // make our cache of simple events...
        List<E> events = getEventValues( _event );
        simpleEventCache = new ArrayList<>(events.size());
        events.forEach( (event) -> simpleEventCache.add( new FSMSimpleEvent<>( event ) ) );
    }


    /**
     * Returns the cached {@link FSMSimpleEvent} for the given event.
     *
     * @param _event The event enum to get an {@link FSMEvent} instance for.
     * @return the {@link FSMSimpleEvent} for the given event enum
     */
    public FSMEvent<E> from( final E _event ) {
        return simpleEventCache.get( _event.ordinal() );
    }


    /**
     * Returns a new instance of {@link FSMDataEvent} created from the given event and data.  However, if the given data is {@code null}, then
     * the cached {@link FSMSimpleEvent} for the given event is returned.
     *
     * @param _event The event enum to get an {@link FSMEvent} instance for.
     * @param _data The optional data for the event.
     * @return the {@link FSMEvent} for the given event enum and data
     */
    public FSMEvent<E> from( final E _event, final Object _data ) {
        if( _data == null )
            return simpleEventCache.get( _event.ordinal() );
        return new FSMDataEvent<>( _event, _data );
    }


    /**
     * Schedule the given event to be sent after the given delay from now.
     *
     * @param _event The event to be sent after the given delay.
     * @param _delay The delay before sending the given event.
     * @return the cancellable {@link FSMEvent}
     */
    public FSMEvent<E> schedule( final FSMEvent<E> _event, final Duration _delay ) {

        // fail fast...
        if( _event == null )
            throw new IllegalArgumentException( "No event to schedule" );

        return schedule( _event.event, _event.getData(), _delay );
    }


    /**
     * Schedule the event specified by the given event enum to be sent after the given delay from now.
     *
     * @param _event The event enum specifying the event to be sent after the given delay.
     * @param _delay The delay before sending the given event.
     * @return the cancellable {@link FSMEvent}
     */
    public FSMEvent<E> schedule( final E _event, final Duration _delay ) {
        return schedule( _event, null, _delay );
    }


    /**
     * Schedule the event specified by the given event enum, with the given event data, to be sent after the given delay from now.
     *
     * @param _event The event enum specifying the event to be sent after the given delay.
     * @param _data The event data.
     * @param _delay The delay before sending the given event.
     * @return the cancellable {@link FSMEvent}
     */
    public FSMEvent<E> schedule( final E _event, final Object _data, final Duration _delay ) {

        // fail fast...
        if( eventScheduler == null )
            throw new IllegalStateException( "No event scheduler" );
        if( _event == null )
            throw new IllegalArgumentException( "No event to schedule" );
        if( _delay == null )
            throw new IllegalArgumentException( "No delay for schedule" );

        // schedule the event and return the cancellable event...
        FSMEvent<E> cancellableEvent = (_data == null) ? new FSMCancellableEvent<>( _event ) : new FSMCancellableDataEvent<>( _event, _data );
        ScheduledFuture<?> scheduledFuture = eventScheduler.schedule(
                new EventSender( cancellableEvent ),   // the Runnable with our event ready to post...
                _delay                                 // the delay...
        );
        cancellableEvent.setFuture( scheduledFuture ); // stuff the scheduled future into our cancellable event...
        return cancellableEvent;
    }


    public void shutdown() {

        // if we have an event scheduler, shut it down...
        if( eventScheduler != null ) {
            eventScheduler.shutdown();
        }
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
            fsm.onEvent( event );
        }
    }
}
