package com.dilatush.util.networkingengine.interfaces;

import com.dilatush.util.Outcome;

/**
 * Implemented by handlers for send complete notifications, which occur after sending a datagram is complete.
 */
@FunctionalInterface
public interface OnSendCompleteHandler {

    /**
     * Handle the given outcome of a datagram send operation.
     *
     * @param _outcome The outcome of sending a datagram. If ok, then the datagram was successfully sent.  If not ok, then there is an explanatory message and possibly the
     *                 exception that caused a problem.
     */
    void handle( final Outcome<?> _outcome );
}
