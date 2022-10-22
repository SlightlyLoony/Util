package com.dilatush.util.networkingengine.interfaces;

import com.dilatush.util.Outcome;

/**
 * Implemented by handlers of TCP write completion.
 */
@FunctionalInterface
public interface OnTCPWriteCompleteHandler {

    /**
     * Handle the TCP write completion with the given outcome.
     *
     * @param _outcome The outcome of the TCP write completion.
     */
    void handle( final Outcome<?> _outcome );
}
