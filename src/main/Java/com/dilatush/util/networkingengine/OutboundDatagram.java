package com.dilatush.util.networkingengine;

import com.dilatush.util.ip.IPAddress;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Objects;

import static com.dilatush.util.General.isNull;


/**
 * Instances of this class represent datagrams that will be sent: the bytes in the datagram, a timestamp for when the datagram was created, and the IP address and UDP port of the
 * destination of the datagram.
 */
@SuppressWarnings( "unused" )
public class OutboundDatagram {

    private final ByteBuffer data;          // the bytes contained in the datagram to be sent...
    private final IPAddress  ipAddress;     // the IP address of the datagram's destination...
    private final int        port;          // the UDP port of the datagram's destination...
    private final Instant    whenCreated;   // when the datagram was created...


    /**
     * Creates a new instance of this class, with the given datagram data, the given remote address, and the given truncation flag.  The time received is generated in the
     * constructor.
     *
     * @param _data The data contained in this datagram.  The {@link ByteBuffer}'s position must be at the first byte of the data, and the limit must be one beyond the last byte.
     * @param _ip The IP address of the datagram's destination.
     * @param _port The UDP port of the datagram's destination, in the range [1..65535].
     */
    public OutboundDatagram( final ByteBuffer _data, final IPAddress _ip, final int _port ) {

        // sanity checks...
        if( isNull( _data, _ip ) )
            throw new IllegalArgumentException( "_data or _ip is null" );
        if( _ip.isWildcard() )
            throw new IllegalArgumentException( "_ip is the wildcard address" );
        if( (_port < 1) || (_port > 65535) )
            throw new IllegalArgumentException( "_port is out of range [1..65535]: " + _port );

        // save it all, and generate a timestamp...
        data         = _data;
        port         = _port;
        ipAddress    = _ip;
        whenCreated  = Instant.now();
    }


    /**
     * Creates a new instance of this class, with the given datagram data, as a reply to the given {@link InboundDatagram}.
     *
     * @param _data The data for the reply datagram.
     * @param _datagram The {@link InboundDatagram} to reply to.
     */
    public OutboundDatagram( final ByteBuffer _data, final InboundDatagram _datagram ) {
        this( _data, getDatagram( _datagram ).getIpAddress(), getDatagram( _datagram ).getPort() );
    }


    /**
     * Silly little helper method to allow checking for null datagram before calling the other constructor.
     *
     * @param _datagram The datagram to return, but if it is {@code null}, an {@link IllegalArgumentException} is thrown.
     * @return The given datagram, if it is not {@code null}.
     */
    private static InboundDatagram getDatagram( final InboundDatagram _datagram ) {

        // sanity checks...
        if( isNull( _datagram ) )
            throw new IllegalArgumentException( "_datagram is null" );

        return _datagram;
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
     * Returns the {@link Instant} that this datagram was received.
     *
     * @return The {@link Instant} that this datagram was received.
     */
    public Instant getWhenCreated() {

        return whenCreated;
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
        OutboundDatagram that = (OutboundDatagram) _o;
        return port == that.port && data.equals( that.data ) && ipAddress.equals( that.ipAddress ) && whenCreated.equals( that.whenCreated );
    }


    /**
     * The hash code of this instance.
     *
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        return Objects.hash( data, ipAddress, port, whenCreated );
    }
}
