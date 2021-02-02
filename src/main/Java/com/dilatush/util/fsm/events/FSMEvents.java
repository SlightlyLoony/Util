package com.dilatush.util.fsm.events;

import com.dilatush.util.fsm.FSM;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private final ScheduledExecutorService eventScheduler;

    // the FSM this instance is associated with...
    private final FSM<S,E> fsm;


    public FSMEvents( final FSM<S,E> _fsm, final ScheduledExecutorService _eventScheduler, final E _event ) {

        fsm = _fsm;
        eventScheduler = _eventScheduler;

        // make our cache of simple events...
        List<E> events = getEventValues( _event );
        simpleEventCache = new ArrayList<>(events.size());
        events.forEach( (event) -> simpleEventCache.add( new FSMSimpleEvent<>( event ) ) );
    }


    public FSMEvent<E> from( final E _event ) {
        return simpleEventCache.get( _event.ordinal() );
    }


    public FSMEvent<E> from( final E _event, final Object _data ) {
        if( _data == null )
            return simpleEventCache.get( _event.ordinal() );
        return new FSMDataEvent<>( _event, _data );
    }


    public FSMEvent<E> schedule( final FSMEvent<E> _event, final Duration _delay ) {

        // fail fast...
        if( _event == null )
            throw new IllegalArgumentException( "No event to schedule" );

        return schedule( _event.event, _event.getData(), _delay );
    }


    public FSMEvent<E> schedule( final E _event, final Duration _delay ) {
        return schedule( _event, null, _delay );
    }


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
                _delay.toNanos(),                      // the delay in nanoseconds...
                TimeUnit.NANOSECONDS                   // tell the scheduler that it's in nanoseconds...
        );
        cancellableEvent.setFuture( scheduledFuture ); // stuff the scheduled future into our cancellable event...
        return cancellableEvent;
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
