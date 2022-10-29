package com.dilatush.util.networkingengine;

/**
 * <p>The general pattern of use is very straightforward:</p>
 * <ul>
 *     <li>First, create an instance of this class, and keep it alive.</li>
 *     <li>If you want to listen on a TCP port, create a new instance of {@link com.dilatush.util.networkingengine.TCPListener} (using one of the {@code newTCPListener()} methods).
 *     When a connection is made to the listener, it will automatically create and connect to an instance of {@link com.dilatush.util.networkingengine.TCPInboundPipe}, with which
 *     you may read and write data.</li>
 *     <li>If you want to make a connection to a listening TCP port, create a new instance of {@link com.dilatush.util.networkingengine.TCPOutboundPipe} (using one of the
 *     {@code TCPOutboundPipe.getNewInstance(} methods), and use it to initiate the connection, read data, and write data.</li>
 *     <li>If you want to receive datagrams from multiple IP addresses and UDP ports, and optionally respond to them, then create a new instance of
 *     {@link com.dilatush.util.networkingengine.UDPServer} (using one of the {@code getNewInstance} methods), then use that instance to receive and send datagrams.</l>
 *     <li>If you want to send datagrams to, or receive datagrams from, a particular IP address and UDP port, then create a new instance of
 *     {@link com.dilatush.util.networkingengine.UDPClient} (using one of the {@code newInstance()} methods), then use that instance to receive or send datagrams.</li>
 * </ul>
 * <p>The classes in this package that implement the actual communications ({@link com.dilatush.util.networkingengine.TCPListener},
 * {@link com.dilatush.util.networkingengine.TCPInboundPipe}, {@link com.dilatush.util.networkingengine.UDPServer}, and {@link com.dilatush.util.networkingengine.UDPClient}) may
 * all be subclassed to provide specialized communications.  For instance, you might extend {@link com.dilatush.util.networkingengine.TCPListener} to make an {@code HTTPListener}
 * as the core of a web server.  Similarly you might extend {@link com.dilatush.util.networkingengine.TCPOutboundPipe} to make an {@code HTTPPipe} to implement individual
 * connections to your web server.</p>
 */