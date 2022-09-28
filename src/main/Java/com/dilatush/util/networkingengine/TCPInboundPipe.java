package com.dilatush.util.networkingengine;

import com.dilatush.util.Outcome;

import java.io.IOException;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;

/**
 * Instances of this class (or its subclasses) are used to communicate over the TCP protocol.  Instances may be created by a listener ({@link TCPListener} to respond to inbound
 * TCP connections, or they may be created by application code to initiate an outbound TCP connection.
 */
public class TCPInboundPipe extends TCPPipe {

    private static final Logger                      LOGGER                                      = getLogger();
    private static final int                         DEFAULT_FINISH_CONNECTION_TIMEOUT_MS        = 2000;
    private static final int                         MAX_FINISH_CONNECTION_INTERVAL_INCREMENT_MS = 100;

    private static final Outcome.Forge<TCPInboundPipe>      forgeTCPInboundPipe = new Outcome.Forge<>();
    private static final Outcome.Forge<?>            forge              = new Outcome.Forge<>();


    private int  finishConnectionIntervalMs;  // delay (in milliseconds) until the next finish connection check...
    private long finishConnectionStartTime;   // when we started the finish connection process...


    /* package-private */ static Outcome<TCPInboundPipe> getTCPInboundPipe( final NetworkingEngine _engine, final TCPPipeInboundConfig _config ) {
        try {
            var pipe = new TCPInboundPipe( _engine, _config );
            return forgeTCPInboundPipe.ok( pipe );
        }
        catch( Exception _e ) {
            return forgeTCPInboundPipe.notOk( "Problem creating or configuring TCP inbound pipe: " + _e.getMessage(), _e );
        }
    }



    protected TCPInboundPipe( final NetworkingEngine _engine, final TCPPipeInboundConfig _config ) throws IOException {
        super( _engine, _config );
    }


    public String toString() {
        return "TCPInboundPipe: " + channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();
    }
}
