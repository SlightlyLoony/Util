package com.dilatush.util.networkingengine.interfaces;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;

/**
 * Implemented by handlers of TCP read completion.
 */
@FunctionalInterface
public interface OnTCPReadCompleteHandler {

    /**
     * Handle the TCP read completion with the given outcome.
     *
     * @param _outcome The outcome of the TCP read completion, with the given data if ok.
     */
    void handle( final Outcome<ByteBuffer> _outcome );
}
