package com.dilatush.util.networkingengine;

import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.ip.IPv4Address;

import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Logger;

import static java.lang.System.exit;

public class Test {

    private static Logger LOGGER;

    private static ServerSocketChannel serverSocketChannel;
    private static ServerSocket serverSocket;


    public static void main( final String[] _args ) {

        // set the configuration file location (must do before any logging actions occur)...
        General.initLogging( "networkEngineLogging.properties" );
        LOGGER = General.getLogger();
        LOGGER.info( "Networking Engine Test Started" );

        // get an instance of a networking engine...
        var engineOutcome = NetworkingEngine.getInstance( "Test" );
        if( engineOutcome.notOk() ) {
            LOGGER.severe( engineOutcome.msg() );
            exit( 1 );
        }
        var engine = engineOutcome.info();

        // set up a TCP listener...
        Outcome<TCPListener> listenerOutcome = engine.newTCPListener( IPv4Address.LOOPBACK, 12345 );
        if( listenerOutcome.notOk() ) {
            LOGGER.severe( listenerOutcome.msg() );
            exit( 1 );
        }
        var listener = listenerOutcome.info();

        // set up a TCP client pipe to connect to the listener...
        Outcome<TCPPipe> clientPipeOutcome = TCPPipe.getTCPPipe( engine );
        if( clientPipeOutcome.notOk() ) {
            LOGGER.severe( clientPipeOutcome.msg() );
            exit( 1 );
        }
        var clientPipe = clientPipeOutcome.info();

        // connect to the listener...
//        var connectOutcome = clientPipe.connect( IPv4Address.LOOPBACK, 12345 );
        var connectOutcome = clientPipe.connect( IPv4Address.fromString( "151.101.71.5" ).info(), 80 );
        if( connectOutcome.notOk() ) {
            LOGGER.severe( "Client connection outcome: " + connectOutcome.msg() );
            exit( 1 );
        }

        LOGGER.hashCode();

//        try {
//
//            // send some data client -> server...
//            for( int i = 0; i < 100000; i++ ) {
//                ByteBuffer buffer = ByteBuffer.allocate( 8 );
//                buffer.putLong( i + 1 );
//                buffer.flip();
//                int remaining = buffer.remaining();
//                int wrote = serverClient.write(buffer);
//                if( remaining != wrote ) {
//                    LOGGER.finest( "Wrote: " + (i * 8 + buffer.position()) );
//                    break;
//                }
//            }
//        }
//        catch( IOException _e ) {
//            LOGGER.log( SEVERE, "Problem setting up server", _e );
//        }
//        catch( InterruptedException _e ) {
//            LOGGER.info( "interrupted" );
//        }
    }
}
