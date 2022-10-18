package com.dilatush.util.networkingengine;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class UDPClient extends UDPBase {


    protected UDPClient( final NetworkingEngine _engine, final DatagramChannel _channel, final Consumer<InboundDatagram> _onReceiptHandler,
                         final int _maxDatagramBytes, BiConsumer<String,Exception> _onErrorHandler ) throws IOException {

        super( _engine, _channel, _maxDatagramBytes, _onErrorHandler );
    }


    /**
     *
     */
    @Override
    void onReadable() {

    }
}
