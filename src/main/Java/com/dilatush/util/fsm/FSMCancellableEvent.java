package com.dilatush.util.fsm;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMCancellableEvent<E extends Enum<E>> extends FSMEvent<E> {

    private boolean cancelled;

    private ScheduledFuture<?> future;

    public FSMCancellableEvent( final FSMEvent<E> _event ) {

        super( _event.event, _event.data );

        cancelled = false;
    }


    /*package-private*/ void setFuture( final ScheduledFuture<?> _future ) {
        future = _future;
    }


    public boolean isCancelled() {
        return cancelled;
    }


    public void cancel() {

        future.cancel( /* interruptTask */ false );

        cancelled = true;
    }
}
