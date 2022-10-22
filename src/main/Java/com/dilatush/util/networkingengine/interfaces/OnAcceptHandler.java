package com.dilatush.util.networkingengine.interfaces;

import com.dilatush.util.networkingengine.TCPInboundPipe;

/**
 * Implemented by handlers of new inbound TCP connections.
 */
@FunctionalInterface
public interface OnAcceptHandler {

    /**
     * Handle the given newly accepted TCP connection.
     *
     * @param _newConnection The {@link TCPInboundPipe} representing the newly accepted TCP connection.
     */
    void handle( final TCPInboundPipe _newConnection );
}
