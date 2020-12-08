package com.dilatush.util.noisefilter;

import java.time.Instant;
import java.util.Random;

/**
 * A simple test of {@link NoiseFilter}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class NoiseFilterTests {

    public static void main( final String[] _args ) {

        NoiseFilter nf1 = new NoiseFilter( 40000, NoiseFilterTests::distance );
        float[] data1 = new float[]
                {
                        22f, 23f, 12f, 22f, 25f, 24f, 25f, 26f, 27f, 28f,
                        29f, 30f, 14f, 59f, 32f, 33f, 34f, 35f, 36f, 36f,
                        87f, 35f, 34f, 32f, 28f, 25f, 24f, 24f, 24f, 24f,
                        24f, 24f, 24f, 24f, 24f, 24f, 24f, 24f, 11f, 25f,
                        26f, 27f, 28f, 27f, 26f, 25f, 25f, 24f, 23f, 22f,
                        25f, 26f, 27f, 28f, 29f, 27f, 26f, 25f, 11f, 24f,
                        25f, 26f, 27f, 28f, 29f, 27f, 26f, 25f, 11f, 24f,
                        23f, 21f, 19f, 17f, 14f, 12f, 15f, 17f, 20f, 15f,
                        22f, 23f, 25f, 28f, 29f, 27f, 26f, 25f, 11f, 24f
                };
        test( nf1, data1, 500, 20000, 5000 );
        out( nf1.toString() );

        NoiseFilter nf2 = new NoiseFilter( 40000, NoiseFilterTests::distance );
        float[] data2 = new float[100];

        // generate some randomish data...
        Random random = new Random( 555 );   // change this seed value to get different datasets...
        float val = 20;  // our nominal signal value...
        for( int i = 0; i < data2.length; i++ ) {

            // do something realistic, or an anomaly?
            if( random.nextFloat() < 0.2 ) {

                // anomaly time!
                data2[i] = random.nextFloat() * 60 - 30;
            }

            else {
                val = (float) Math.round( 4 * (val + 5 * (random.nextFloat() - .5) ) ) / 4f;
                data2[i] = val;
            }
        }
        test( nf2, data2, 500, 20000, 5000 );
        out( nf2.toString() );
    }


    /**
     * A simple distance implementation.  This implementation assumes that temperature values generally lie within an 80C range, and the square of
     * the difference between the two sample's temperature has a 98% weight in the result.  The range of time differences is assumed to lie within
     * a 40 second (40,000 millisecond) range, and the square of the difference of the timestamps, in milliseconds, has a 2% weight in the result.
     * The two weighted computations are simply added to get the final distance.
     */
    private static float distance( final Sample _newSample, final Sample _existingSample ) {
        float measurementScore = .98f * (float) Math.pow( _newSample.value - _existingSample.value,                                       2 ) / 6400;
        float timeScore        = .02f * (float) Math.pow( _newSample.timestamp.toEpochMilli() - _existingSample.timestamp.toEpochMilli(), 2 ) / 16000000;
        return measurementScore + timeScore;
    }


    private static void test( final NoiseFilter _nf, final float[] _measurements, final long _intervalMS, final long _minDepth, final long _noise ) {
        Instant currentTime = Instant.now().minusMillis( _intervalMS * _measurements.length );
        for( float measurement : _measurements ) {
            _nf.addSample( new Sample( measurement, currentTime ) );
            currentTime = currentTime.plusMillis( _intervalMS );
            _nf.prune( currentTime );
            Sample reading = _nf.sampleAt( _minDepth, _noise, currentTime );
            if( reading != null)
                out( reading.toString() );
        }
    }


    private static void out( final String _s ) {
        System.out.println( _s );
    }
}
