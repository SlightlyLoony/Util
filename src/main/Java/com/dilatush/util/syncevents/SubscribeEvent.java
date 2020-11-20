package com.dilatush.util.syncevents;

/**
 * Instances of this class implement an immutable synchronous event to subscribe a handler to an event.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SubscribeEvent implements SynchronousEvent {

    public final SubscriptionDefinition definition;


    /**
     * Creates a new instance of this class with the given synchronous event subscription definition.
     *
     * @param _definition the subscription definition with the handler and event class
     */
    public SubscribeEvent( final SubscriptionDefinition _definition ) {
        definition = _definition;
    }
}
