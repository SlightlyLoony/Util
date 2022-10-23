package com.dilatush.util.networkingengine.interfaces;

import com.dilatush.util.Outcome;
import com.dilatush.util.networkingengine.InboundDatagram;

/**
 * Implemented by handlers for a received datagram.
 */
@FunctionalInterface
public interface OnReceiveDatagramHandler {

    /**
     * Handle the given datagram.
     *
     * @param _datagram The datagram to be handled.
     */
    void handle( final Outcome<InboundDatagram> _datagram );
}
