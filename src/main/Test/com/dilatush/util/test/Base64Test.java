package com.dilatush.util.test;

import com.dilatush.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Tests for correctness and speed against the Java JRE's Base64 classes.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Base64Test {

    public static final int REPS = 1_000_000;

    public static void main( final String[] _args ) {

        // some setup...
        Random random = new Random( 89298472 );  // our repeatable source of "randomness"...
        java.util.Base64.Decoder jreDecoder = java.util.Base64.getDecoder();
        java.util.Base64.Encoder jreEncoder = java.util.Base64.getEncoder().withoutPadding();

        // correctness tests...
        for( int i = 0; i < REPS; i++ ) {

            // get some randomish bytes...
            byte[] bytes = new byte[1 + random.nextInt(300)];  // create some bytes, length in [1..301]...
            random.nextBytes( bytes );

            // encode with my encoder, JRE's...
            String myEncoding = Base64.encode( bytes );
            String jreEncoding = jreEncoder.encodeToString( bytes );

            // see if they're different...
            if( !myEncoding.equals( jreEncoding ) ) {
                System.out.println( "Incorrect encoding" );
            }

            // decode with my decoder, JRE's...
            byte[] myBytes = Base64.decodeBytes( myEncoding );
            byte[] jreBytes = jreDecoder.decode( jreEncoding );

            // see if they're different...
            if( !Arrays.equals( myBytes, jreBytes ) ) {
                System.out.println( "Incorrect decoding" );
            }
        }

        // benchmarks...

        // make ourselves a bunch of byte arrays to encode during benchmarking...
        List<byte[]> bytes = new ArrayList<>();
        for( int i = 0; i < REPS; i++ ) {
            byte[] pbytes = new byte[1 + random.nextInt( 300 ) ];
            bytes.add( pbytes );
            random.nextBytes( pbytes );
        }

        // my codec...
        byte[] stopper = new byte[0];   // this exists to prevent compiler from optimizing out the decoder calls below...
        long start = System.currentTimeMillis();
        for( int i = 0; i < REPS; i++ ) {
            String encoded = Base64.encode( bytes.get( i ) );
            stopper = Base64.decodeBytes( encoded );
        }
        long stop = System.currentTimeMillis();
        System.out.println( "My codec: " + (stop - start) + " milliseconds" );

        // JRE codec...
        start = System.currentTimeMillis();
        for( int i = 0; i < REPS; i++ ) {
            String encoded = jreEncoder.encodeToString( bytes.get( i ) );
            stopper = jreDecoder.decode( encoded );
        }
        stop = System.currentTimeMillis();
        System.out.println( "JRE codec: " + (stop - start) + " milliseconds" );

        System.out.println( "Done " + Arrays.hashCode( stopper ) );
    }
}
