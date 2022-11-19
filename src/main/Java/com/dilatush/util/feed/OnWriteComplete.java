package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

/**
 * Implemented by {@link OutFeed} write complete handlers.
 */
@FunctionalInterface
public interface OnWriteComplete {

    /**
     * Handle the {@link OutFeed} write completion with the given outcome.
     *
     * @param _outcome The outcome of the {@link OutFeed} write completion.
     */
    void handle( final Outcome<?> _outcome );
}
