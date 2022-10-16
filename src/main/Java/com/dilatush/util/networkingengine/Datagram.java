package com.dilatush.util.networkingengine;

import com.dilatush.util.ip.IPAddress;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static com.dilatush.util.General.isNull;


@SuppressWarnings( "unused" )
public class Datagram {

    private final ByteBuffer data;
    private final IPAddress ipAddress;
    private final int port;
    private final boolean truncated;

    public Datagram( ByteBuffer _data, InetSocketAddress _remoteAddress, boolean _truncated ) {

        // sanity checks...
        if( isNull( _data, _remoteAddress ) )
            throw new IllegalArgumentException( "_data or _remote address is null" );
        if( IPAddress.fromInetAddress( _remoteAddress.getAddress() ).isWildcard() )
            throw new IllegalArgumentException( "IP address in _remoteAddress is the wildcard address" );
        if( _remoteAddress.getPort() == 0 )
            throw new IllegalArgumentException( "Port in _remoteAddress is the ephemeral port (0)" );

        data = _data;
        port = _remoteAddress.getPort();
        ipAddress = IPAddress.fromInetAddress( _remoteAddress.getAddress() );
        truncated = _truncated;
    }


    public Datagram( ByteBuffer data, InetSocketAddress remoteAddress ) {
        this( data, remoteAddress, false );
    }


    public ByteBuffer getData() {

        return data;
    }


    public IPAddress getIpAddress() {

        return ipAddress;
    }


    public int getPort() {

        return port;
    }


    public boolean isTruncated() {

        return truncated;
    }
}
