package com.dilatush.util.syncevents;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class define a synchronous events subscription as an immutable couple of the event handler and the event type.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SubscriptionDefinition {

    public final SynchronousEventSubscriber subscriber;
    public final Class<?>                   eventClass;


    /**
     * Creates a new instance of this class that defines a synchronous event subscription with the given handler and event class.
     *
     * @param _handler the handler to invoke for events of the given class
     * @param _eventClass the class of the events to subscribe to
     * @throws IllegalArgumentException if <code>_handler</code> is <code>null</code> or if the given event class does not implement the marker
     *                                  interface {@link SynchronousEvent}.
     */
    public SubscriptionDefinition( final SynchronousEventSubscriber _handler, final Class<?> _eventClass ) {
        subscriber = _handler;
        eventClass = _eventClass;

        // make sure we actually got a subscriber...
        if( isNull( _handler ) )
            throw new IllegalArgumentException( "No handler supplied (_handler == null)" );

        // make sure that the given class is actually a synchronous event class...
        if( !SynchronousEvent.class.isAssignableFrom( _eventClass ) )
            throw new IllegalArgumentException( _eventClass.getCanonicalName() + " does not implement " + SynchronousEvent.class.getCanonicalName() );
    }
}
