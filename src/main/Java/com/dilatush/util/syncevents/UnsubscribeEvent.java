package com.dilatush.util.syncevents;

/**
 * Instances of this class implement an immutable synchronous event to unsubscribe a handler from an event.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class UnsubscribeEvent implements SynchronousEvent {

    public final SubscriptionDefinition definition;


    /**
     * Creates a new instance of this class with the given synchronous event subscription definition.
     *
     * @param _definition the subscription definition with the handler and event class
     */
    public UnsubscribeEvent( final SubscriptionDefinition _definition ) {
        definition = _definition;
    }
}
