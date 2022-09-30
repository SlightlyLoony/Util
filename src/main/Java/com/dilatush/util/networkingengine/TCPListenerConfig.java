package com.dilatush.util.networkingengine;

import com.dilatush.util.ip.IPAddress;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Creates a new instance of this record with the given parameters.
 *
 * @param bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
 *                 wildcard address for all local network interfaces.
 * @param bindToPort The local TCP port to bind this TCP listener to.
 * @param onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications on the
 *                        accepted TCP connection.
 * @param onErrorHandler The optional (it may be {@code null} to use the default error handler) handler to be called if an error occurs while accepting a connection.  A message
 *                       describing the problem, and possibly an exception causing the problem, are both passed to the handler.
 * @param rejectConnectionHandler The optional (it may be {@code null} to use the default reject connection handler) to be called after a TCP connection has been accepted, but
 *                                before the {@link #onAcceptHandler} is called.  If the reject connection handler returns {@code true}, the accepted connection is closed, and
 *                                the {@link #onAcceptHandler} is not called; otherwise the {@link #onAcceptHandler} is called with the new {@link TCPInboundPipe}.
 */
@SuppressWarnings( "unused" )
public record TCPListenerConfig( IPAddress bindToIP, int bindToPort, Consumer<TCPInboundPipe> onAcceptHandler, BiConsumer<String,Exception> onErrorHandler,
                                 Function<TCPInboundPipe,Boolean> rejectConnectionHandler ) {


    /**
     * Creates a new instance of this record with the given parameters, and {@code null} values for {@link #onErrorHandler} and {@link #rejectConnectionHandler}.
     *
     * @param bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                 wildcard address for all local network interfaces.
     * @param bindToPort The local TCP port to bind this TCP listener to.
     * @param onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications on the
     *                        accepted TCP connection.
     */
    public TCPListenerConfig( IPAddress bindToIP, int bindToPort, Consumer<TCPInboundPipe> onAcceptHandler ) {
        this( bindToIP, bindToPort, onAcceptHandler, null, null );
    }


    /**
     * Creates a new instance of this record with the given parameters, and a {@code null} value for {@link #onErrorHandler}.
     *
     * @param bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                 wildcard address for all local network interfaces.
     * @param bindToPort The local TCP port to bind this TCP listener to.
     * @param onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications on the
     *                        accepted TCP connection.
     * @param rejectConnectionHandler The optional (it may be {@code null} to use the default reject connection handler) to be called after a TCP connection has been accepted, but
     *                                before the {@link #onAcceptHandler} is called.  If the reject connection handler returns {@code true}, the accepted connection is closed, and
     *                                the {@link #onAcceptHandler} is not called; otherwise the {@link #onAcceptHandler} is called with the new {@link TCPInboundPipe}.
     */
    public TCPListenerConfig(  IPAddress bindToIP, int bindToPort, Consumer<TCPInboundPipe> onAcceptHandler,
                               Function<TCPInboundPipe,Boolean> rejectConnectionHandler ) {
        this( bindToIP, bindToPort, onAcceptHandler, null, rejectConnectionHandler );
    }


    /**
     * Creates a new instance of this record with the given parameters, and a {@code null} value for {@link #rejectConnectionHandler}.
     *
     * @param bindToIP The local IP address to bind this TCP listener to.  The IP address may be either IPv4 or IPv6 for a particular local network interface, or it may be the
     *                 wildcard address for all local network interfaces.
     * @param bindToPort The local TCP port to bind this TCP listener to.
     * @param onAcceptHandler The handler to be called when an inbound TCP connection is accepted.  The argument is the new {@link TCPInboundPipe} configured for communications on the
     *                        accepted TCP connection.
     * @param onErrorHandler The optional (it may be {@code null} to use the default error handler) handler to be called if an error occurs while accepting a connection.  A message
     *                       describing the problem, and possibly an exception causing the problem, are both passed to the handler.
     */
    public TCPListenerConfig(  IPAddress bindToIP, int bindToPort, Consumer<TCPInboundPipe> onAcceptHandler,
                               BiConsumer<String,Exception> onErrorHandler ) {
        this( bindToIP, bindToPort, onAcceptHandler, onErrorHandler, null );
    }
}


