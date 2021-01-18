package com.dilatush.util.console;

import static java.lang.Thread.sleep;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestConsoleServer {

    public static void main( final String[] _args ) {

//        try {
//            SecureRandom random = new SecureRandom();
//            Cipher cipher = Cipher.getInstance( "AES/CTR/NoPadding" );
//            byte[] key = new byte[ 128 / 8 ];
//            random.nextBytes( key );
//            byte[] nonce = new byte[ 64 / 8 ];
//            random.nextBytes( nonce );
//            byte[] iv = new byte[ 128 / 8 ];
//            arraycopy( nonce, 0, iv, 0, nonce.length );
//            Key keySpec = new SecretKeySpec( key, "AES" );
//            IvParameterSpec ivSpec = new IvParameterSpec( iv );
//            cipher.init( Cipher.ENCRYPT_MODE, keySpec, ivSpec );
//
//            byte[] c1 = cipher.update( "This.\n".getBytes( StandardCharsets.UTF_8 ) );
//            byte[] c2 = cipher.update( "And this is the second part, I hope.\n".getBytes( StandardCharsets.UTF_8 ) );
//            byte[] c3 = cipher.doFinal();
//
//            cipher.init( Cipher.DECRYPT_MODE, keySpec, ivSpec );
//            byte[] d1 = cipher.update( c1 );
//            byte[] d2 = cipher.update( c2 );
//            byte[] d3 = cipher.update( c3 );
//            byte[] d4 = cipher.doFinal();
//
//
//            cipher.hashCode();
//        }
//        catch( Exception _e ) {
//            _e.printStackTrace();
//        }
//    }

        try {
            ConsoleServer.Config config = new ConsoleServer.Config();
            config.port = 8217;
            config.bindTo = null;
            config.maxClients = 1;
            config.name = "test";
            config.key = "abcdefghijklmnopqrstuv";
            ConsoleServer server = new ConsoleServer( config );
            server.start();


            while( true ) {
                sleep( 1000 );
            }
        }
        catch( Exception _e ) {
            System.out.println( _e.getMessage() );
            _e.printStackTrace();
        }
    }
}
