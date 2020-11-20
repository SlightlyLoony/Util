package com.dilatush.util.syncevents;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface SynchronousEventSubscriber {

    void handle( final SynchronousEvent _event );
}
