package com.dilatush.util.networkingengine.interfaces;

import com.dilatush.util.ip.IPAddress;

/**
 * Implemented by functions accept the IP address and UDP port for the source of a connection or datagram, and return {@code true} if that connection or datagram should be
 * accepted for processing.
 */
@FunctionalInterface
public interface SourceFilter {

    /**
     * Return {@code true} if the given IP address and given port should be accepted for further processing.
     *
     * @param _ip The IP address of the source of a TCP connection request or a UDP datagram.
     * @param _port The TCP port of the source of a TCP connection request or the UDP port of the source of a UDP datagram.
     * @return {@code true} if the TCP connection request or UDP datagram should be accepted.
     */
    boolean accept( final IPAddress _ip, final int _port );
}
