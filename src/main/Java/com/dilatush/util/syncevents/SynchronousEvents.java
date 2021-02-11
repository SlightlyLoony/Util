package com.dilatush.util.syncevents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A singleton class that implements a simple synchronous event system.</p>
 * <p>By "event system", we mean a way for code to publish an "event" (which is encapsulated by an instance of an event class) to zero or more
 * "subscribers" to these events.  For example, a method that measures temperature might periodically publish a "temperature event" that contains
 * the most recently measured temperature.  Any other code that was interested in temperature could subscribe that that event, and each time the
 * temperature event was published, each subscriber's event handler would be called with that temperature event instance (not a copy - the same
 * instance is sent to every subscriber).  This mechanism is very useful for loose and dynamic coupling between different parts of a larger system.</p>
 * <p>By "simple", we mean that this event system has the minimum required features.  For instance, subscriptions are made to particular event types,
 * not hierarchies of event types.  Similarly, there is no mechanism for a subscriber to determine the publisher of an event, and any code may
 * publish any type of event - so there is no security or protection of any kind within this system.  Malicious code can have a field day!</p>
 * <p>By "synchronous" we mean that asynchronously published events (from any thread) are dispatched serially on a single thread, simplifying the code
 * in the subscribed event handling functions.  This also, of course, introduces some significant limitations, especially sensitivity to the execution
 * time of subscribed handlers and a complete lack of parallelization in the event handling.  Published events are queued until they are dispatched to
 * event handlers. By default, this queue has a capacity of 100 events.  This capacity can be modified by using the {@link #getInstance(int)}
 * getter for the first usage in the process.</p>
 * <p>We strongly recommend the following "best practices":</p>
 * <ul>
 *     <li>Use immutable instances for events.  Mutable events provide a pathway for cross-thread synchronization issues.</li>
 *     <li>Keep the execution time of event handlers as short as possible.  In particular, you should avoid any sort of blocking I/O.</li>
 *     <li>Each subscription should be unsubscribed when no longer needed.  This is especially true when subscriptions are made to handlers in
 *     transient instances (that is, instances that are not permanent and may be destroyed).  Failure to unsubscribe such instances will result in a
 *     memory "leak" and possibly reduced performance as resources used within this class are increasingly consumed.</li>
 *     <li>Avoid any dependencies on the <i>order</i> in which handlers for a particular event are invoked.  That order is not guaranteed, nor is
 *     it guaranteed to be stable.</li>
 * </ul>
 * <p>Instances of any class that implement the marker interface {@link SynchronousEvent} may be published as an event.  There are no other
 * requirements.</p>
 * <p>To publish an event, use the following code (or the equivalent):</p>
 * <code>SynchronousEvents.getInstance().publish( event );</code>
 * <p>To subscribe to an event, publish a {@link SubscribeEvent} containing your {@link SubscriptionDefinition} (your handler and the event class).
 * Your handler must be compatible with {@link SynchronousEventSubscriber#handle(SynchronousEvent)}.  Note that events of the type you're subscribing
 * to that were queued before your subscription event will <i>not</i> be sent to your handler - only those queued after your subscription event will
 * be sent to your handler.</p>
 * <p>To unsubscribe to an event, publish a {@link UnsubscribeEvent} containing the same instance of {@link SubscriptionDefinition} that you used to
 * subscribe with.  Note that it is possible for your handler to get events after you unsubscribe.  That's because the unsubscribe event is queued
 * just like any other event, and there may be instances of the event you're unsubscribing from queued ahead of it.</p>
 * <p>Note that there may be any number of subscriptions to any particular event, and than any instance of a class may subscribe to any number of
 * events.  However, any given instance may subscribe only once using an instance of {@link SubscriptionDefinition}; attempts to subscribe more than
 * once will be ignored.  Note that if separate instances of {@link SubscriptionDefinition} defining the same handler and event class are used to
 * create subscriptions, they <i>will</i> work.  In that case, when that particular event is published, the specified handler will be invoked
 * multiple times, once for each subscription.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SynchronousEvents {

    private static final Logger LOGGER                    = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );
    private static final int    DEFAULT_MAX_QUEUED_EVENTS = 100;

    private static SynchronousEvents instance       = null;

    private final ArrayBlockingQueue< SynchronousEvent >              events;
    private final Map< Class<?>, List< SynchronousEventSubscriber > > subscriptions;


    /**
     * Creates a new instance of this class.  This constructor has private access, and is only called once, to create the singleton instance of this
     * class on the first invocation of {@link #getInstance()}.
     */
    private SynchronousEvents( final int _maxQueued ) {

        // create the simple blocking queue that contains published, unhandled events...
        events = new ArrayBlockingQueue<>( _maxQueued );

        // create the a map from event type to the list of subscribers to that event...
        subscriptions = new HashMap<>();

        // create subscriptions for subscribe and unsubscribe events...
        subscribe( this::subscribeHandler, SubscribeEvent.class );
        subscribe( this::unsubscribeHandler, UnsubscribeEvent.class );

        // start up our dispatcher thread...
        new Dispatcher();
    }


    /**
     * Publishes the given event to all subscribers of the given event's type.
     *
     * @param _event the event to publish
     */
    public void publish( SynchronousEvent _event ) {
        events.add( _event );
        LOGGER.finest( "Published: " + _event.toString() );
    }


    private void subscribe( final SynchronousEventSubscriber _subscriber, final Class<?> _eventClass ) {

        // verify that the given class actually represents a synchronous event...
        if( !SynchronousEvent.class.isAssignableFrom( _eventClass ) )
            throw new IllegalArgumentException( "Attempted to subscribe to " + _eventClass.getSimpleName()
                    + ", which does not implement SynchronousEvents" );

        // if we've never seen this event type before, then add a list for it...
        if( !subscriptions.containsKey( _eventClass ) ) {
            subscriptions.put( _eventClass, new ArrayList<>() );
        }

        // get our list of subscribers...
        List< SynchronousEventSubscriber > subs = subscriptions.get( _eventClass );

        // if our list already contains this subscriber, just leave...
        for( SynchronousEventSubscriber sub : subs ) {
            if( _subscriber.toString().equals( sub.toString() ) ) {
                LOGGER.finer( "Duplicate subscription to " + _eventClass.getSimpleName() + " ignored" );
                return;
            }
        }

        // we didn't already have this subscriber, so add it...
        subs.add( _subscriber );
    }


    private void subscribeHandler( final SynchronousEvent _event ) {
        SubscribeEvent event = (SubscribeEvent) _event;
        subscribe( event.definition.subscriber, event.definition.eventClass );
    }


    private void unSubscribe( final SynchronousEventSubscriber _subscriber, final Class<?> _eventClass ) {

        // verify that the given class actually represents a synchronous event...
        if( !SynchronousEvent.class.isAssignableFrom( _eventClass ) )
            throw new IllegalArgumentException( "Attempted to unsubscribe from " + _eventClass.getSimpleName()
                    + ", which does not implement SynchronousEvents" );

        // if we've never even seen this event type before, then just leave...
        if( !subscriptions.containsKey( _eventClass ) ) {
            LOGGER.finer( "Attempt made to unsubscribe from " + _eventClass.getSimpleName() + " which has never been subscribed to" );
            return;
        }

        // get our list of subscribers...
        List< SynchronousEventSubscriber > subs = subscriptions.get( _eventClass );

        // if our list contains this subscriber, delete it...
        subs.removeIf( subscriber -> _subscriber.toString().equals( subscriber.toString() ) );
    }


    private void unsubscribeHandler( final SynchronousEvent _event ) {
        UnsubscribeEvent event = (UnsubscribeEvent) _event;
        unSubscribe( event.definition.subscriber, event.definition.eventClass );
    }


    /**
     * Return the one and only instance of this class.  If this is the first time a {@code getInstance()} method has been called, the class will be
     * instantiated with the default maximum queue size.
     *
     * @return the one and only instance of this class
     */
    public static synchronized SynchronousEvents getInstance() {
        if( instance == null )
            instance = new SynchronousEvents( DEFAULT_MAX_QUEUED_EVENTS );
        return instance;
    }


    /**
     * Return the one and only instance of this class.  If this is the first time a {@code getInstance()} method has been called, the class will be
     * instantiated with the given maximum queue size.
     *
     * @param _maxQueued The maximum number of events to queue.
     * @return the one and only instance of this class
     */
    public static synchronized SynchronousEvents getInstance( final int _maxQueued ) {
        if( instance == null )
            instance = new SynchronousEvents( _maxQueued );
        return instance;
    }


    /**
     * Publish the given event to all handlers that subscribe to it.
     *
     * @param _event the event to publish
     */
    public static void publishEvent( final SynchronousEvent _event ) {
        SynchronousEvents.getInstance().publish( _event );
    }


    /**
     * Subscribe the given handler to the given class of events.  Note that the subscription is for the exact class given, and not any subclasses
     * of it.  A single handler may be used for multiple event classes by subscribing each class separately.  Note that a convenient call signature
     * is: <pre>{@code
     *     subscribeToEvent( event -> handleSomeEvent( (SomeEvent) event ), SomeEvent.class );
     *
     *     // where the handler is defined as:
     *
     *     private void handleSomeEvent( final SomeEvent _event ) {
     *         // blah, blah...
     *     }
     * }</pre>
     * Using the lambda conveniently allows casting to a specific {@link SynchronousEvent} subclass.
     *
     * @param _handler the handler for the event
     * @param _eventClass the class of event to be subscribed.
     */
    @SuppressWarnings( "unused" )
    public static void subscribeToEvent( final SynchronousEventSubscriber _handler, final Class<?> _eventClass ) {
        publishEvent( new SubscribeEvent( new SubscriptionDefinition( _handler, _eventClass ) ) );
    }


    /**
     * Unsubscribe the given handler from the given class of events.  Note that the subscription is for the exact class given, and not any subclasses
     * of it.  A single handler may be used for multiple event classes by subscribing each class separately.  Note that a convenient call signature
     * is: <pre>{@code
     *     subscribeToEvent( event -> handleSomeEvent( (SomeEvent) event ), SomeEvent.class );
     *
     *     // where the handler is defined as:
     *
     *     private void handleSomeEvent( final SomeEvent _event ) {
     *         // blah, blah...
     *     }
     * }</pre>
     * Using the lambda conveniently allows casting to a specific {@link SynchronousEvent} subclass.
     *
     * @param _handler the handler for the event
     * @param _eventClass the class of event to be subscribed.
     */
    @SuppressWarnings( "unused" )
    public static void unsubscribeFromEvent( final SynchronousEventSubscriber _handler, final Class<?> _eventClass ) {
        publishEvent( new UnsubscribeEvent( new SubscriptionDefinition( _handler, _eventClass ) ) );
    }


    /**
     * Implements a simple dispatcher that takes queued events from the events queue, looks up the subscriptions for an event's class, and invokes
     * each subscribed handler.  Of note is that all Throwables that weren't handled within the event handlers are caught, logged, and ignored.  This
     * guarantees that the dispatcher thread will not terminate.
     */
    private class Dispatcher extends Thread {

        private Dispatcher() {
            setName( "EventDispatcher" );
            setDaemon( true );
            start();
        }


        public void run() {

            while( true ) {

                // get an event from the queue, waiting until one is available...
                SynchronousEvent event;
                try { event = events.take(); } catch( InterruptedException _e ) { break; }

                // if we have no subscribers to this event, just move along...
                if( !subscriptions.containsKey( event.getClass() ) )
                    continue;

                // if we're dispatching a SubscribeEvent or an UnsubscribeEvent we need to use a copy of the subscriber list, rather than the
                // actual subscriber list, because we may be modifying it (which would cause a ConcurrentModificationException)...
                List< SynchronousEventSubscriber > subs = subscriptions.get( event.getClass() );
                if( (event instanceof SubscribeEvent) || (event instanceof UnsubscribeEvent) )
                    subs = new ArrayList<>( subs );

                // dispatch this event to each subscribers...
                for( SynchronousEventSubscriber sub : subs ) {

                    // catch, log, and ignore any unhandled exceptions or errors...
                    try {
                        sub.handle( event );
                    }
                    catch( Throwable _t ) {
                        LOGGER.log( Level.WARNING, "Unhandled exception from event subscriber in " + sub.getClass().getSimpleName(), _t );
                    }
                }
            }
        }
    }
}
