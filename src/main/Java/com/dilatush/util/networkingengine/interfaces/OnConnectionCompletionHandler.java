package com.dilatush.util.networkingengine.interfaces;

import com.dilatush.util.Outcome;

/**
 * Implemented by TCP connection completion handlers.
 */
@FunctionalInterface
public interface OnConnectionCompletionHandler {

    /**
     * Handle a connection completion with the given outcome.
     *
     * @param _outcome The outcome of a connection completion.
     */
    void handle( final Outcome<?> _outcome);
}
