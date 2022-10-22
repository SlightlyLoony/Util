package com.dilatush.util.networkingengine;

import com.dilatush.util.networkingengine.interfaces.OnErrorHandler;

import java.io.IOException;
import java.nio.channels.DatagramChannel;

public class UDPClient extends UDPBase {


    protected UDPClient( final NetworkingEngine _engine, final DatagramChannel _channel,
                         final int _maxDatagramBytes, final OnErrorHandler _onErrorHandler ) throws IOException {

        super( _engine, _channel, _maxDatagramBytes, _onErrorHandler );
    }


    /**
     *
     */
    @Override
    void onReadable() {

    }
}
