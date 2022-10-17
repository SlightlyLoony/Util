package com.dilatush.util.networkingengine;

import com.dilatush.util.ip.IPAddress;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Objects;

import static com.dilatush.util.General.isNull;


/**
 * Instances of this class represent received datagrams: the bytes in the datagram, a timestamp for when the datagram was received, a flag set if the datagram was truncated
 * (meaning that some bytes were discarded because the received datagram was larger than the maximum size), and the IP address and UDP port of the source of the datagram.
 */
@SuppressWarnings( "unused" )
public class ReceivedDatagram {

    private final ByteBuffer data;          // the bytes contained in the received datagram...
    private final IPAddress  ipAddress;     // the IP address of the datagram's source...
    private final int        port;          // the UDP port of the datagram's source...
    private final boolean    truncated;     // flag that is true if the datagram has been truncated...
    private final Instant    whenReceived;  // when the datagram was received...


    /**
     * Creates a new instance of this class, with the given datagram data, the given remote address, and the given truncation flag.  The time received is generated in the
     * constructor.
     *
     * @param _data The data contained in this datagram.  The {@link ByteBuffer}'s position must be at the first byte of the data, and the limit must be one beyond the last byte.
     * @param _remoteAddress The socket address of the source of this datagram.
     * @param _truncated A flag that is {@code true} if the data in this datagram has been truncated.
     */
    public ReceivedDatagram( ByteBuffer _data, InetSocketAddress _remoteAddress, boolean _truncated ) {

        // sanity checks...
        if( isNull( _data, _remoteAddress ) )
            throw new IllegalArgumentException( "_data or _remote address is null" );
        if( IPAddress.fromInetAddress( _remoteAddress.getAddress() ).isWildcard() )
            throw new IllegalArgumentException( "IP address in _remoteAddress is the wildcard address" );
        if( _remoteAddress.getPort() == 0 )
            throw new IllegalArgumentException( "Port in _remoteAddress is the ephemeral port (0)" );

        // save it all, and generate a timestamp...
        data         = _data;
        port         = _remoteAddress.getPort();
        ipAddress    = IPAddress.fromInetAddress( _remoteAddress.getAddress() );
        truncated    = _truncated;
        whenReceived = Instant.now();
    }


    /**
     * Returns the {@link ByteBuffer} containing the data in this datagram.
     *
     * @return The {@link ByteBuffer} containing the data in this datagram.
     */
    public ByteBuffer getData() {

        return data;
    }


    /**
     * Returns the IP address of the source of this datagram.
     *
     * @return The IP address of the source of this datagram.
     */
    public IPAddress getIpAddress() {

        return ipAddress;
    }


    /**
     * Returns the UDP port of the source of this datagram.
     *
     * @return The UDP port of the source of this datagram.
     */
    public int getPort() {

        return port;
    }


    /**
     * Returns {@code true} if this datagram was truncated.
     *
     * @return {@code true} if this datagram was truncated.
     */
    public boolean isTruncated() {

        return truncated;
    }


    /**
     * Returns the {@link Instant} that this datagram was received.
     *
     * @return The {@link Instant} that this datagram was received.
     */
    public Instant getWhenReceived() {

        return whenReceived;
    }


    /**
     * Returns {@code true} if this datagram is equal to the given object.
     *
     * @param _o The object to compare with.
     * @return {@code true} if this datagram is equal to the given object.
     */
    @Override
    public boolean equals( final Object _o ) {

        if( this == _o ) return true;
        if( _o == null || getClass() != _o.getClass() ) return false;
        ReceivedDatagram that = (ReceivedDatagram) _o;
        return port == that.port && truncated == that.truncated && data.equals( that.data ) && ipAddress.equals( that.ipAddress ) && whenReceived.equals( that.whenReceived );
    }


    /**
     * The hash code of this instance.
     *
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        return Objects.hash( data, ipAddress, port, truncated, whenReceived );
    }
}
