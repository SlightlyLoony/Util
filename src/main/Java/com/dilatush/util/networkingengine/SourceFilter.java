package com.dilatush.util.networkingengine;

import com.dilatush.util.ip.IPAddress;

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
