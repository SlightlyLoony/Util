package com.dilatush.util.feed;

import com.dilatush.util.Outcome;

import java.nio.ByteBuffer;

/**
 * Implemented by {@link InFeed} read complete handlers.
 */
@FunctionalInterface
public interface ReadCompleteHandler {

    /**
     * Handle the {@link InFeed} read completion with the given outcome.
     *
     * @param _outcome The outcome of the {@link InFeed} read completion, with the given data if ok.
     */
    void handle( final Outcome<ByteBuffer> _outcome );
}
