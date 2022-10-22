package com.dilatush.util.networkingengine.interfaces;

/**
 * Implemented by error handlers.
 */
public interface OnErrorHandler {

    /**
     * Handle the error with the given message and optional exception.
     *
     * @param _msg The message describing the error that occurred.
     * @param _e The optional (may be {@code null} exception associated with the error.
     */
    void handle( final String _msg, final Exception _e );
}
