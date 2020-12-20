package com.dilatush.util.noisefilter;

import java.time.Instant;
import java.util.Random;

/**
 * A simple test of {@link NoiseFilter}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class NoiseFilterTests {

    private static final Random random = new Random( 83696234 );

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main( final String[] _args ) {

        Config config = new Config();
        config.numSamples = 41;
        config.errorCalc = new MedianErrorCalc();
        config.maxIgnoreFraction = .25f;
        config.maxTotalErrorIgnoreFraction = 1;
        config.minSampleErrorIgnore = .75f;

        NoiseFilter nf1 = new NoiseFilter( config  );
        String nums =
                        "23,23,12,12,12,12,12,12,12,12," +
                        "23,23,23,23,23,23,23,23,23,23," +
                        "23,23,23,23,23,23,23,23,23,23," +
                        "23,23,23,23,23,23,23,23,23,23," +
                        "23,23,12,12,12,12,12,12,12,12," +
                        "23,23,23,23,23,23,23,23,23,23," +
                        "23,23,23,23,23,23,23,23,23,23," +
                        "23,23,23,23,23,23,23,23,23,23," +
                        "23,23,12,12,12,12,12,12,12,12";
        Sample[] samples = getSamples( getData( nums ), 250);
        runTest( nf1, samples );

        NoiseFilter nf2 = new NoiseFilter( config );
        samples = getNoisySamples( .2f, -5f, 8, .001f, 22, .001f, 100, 250 );
        runTest( nf2, samples );

        NoiseFilter nf3 = new NoiseFilter( config );
        samples = getNoisySamples( .25f, -6f, 7, .0015f, 25, .0015f, 200, 200 );
        runTest( nf3, samples );

        nf1.hashCode();
    }


    private static void runTest( final NoiseFilter _nf, final Sample[] _samples ) {
        out( "--------" );
        for( Sample sample : _samples ) {
            _nf.add( sample );
            Sample n = _nf.getFilteredAt( sample.timestamp );
            out( (n == null) ? "null" : n.toString() );
        }
    }


    private static Sample[] getNoisySamples( final float _anomalyFraction, final float _anomalyDelta, final int _anomalyLength,
                                             final float _slope, final float _start, final float _noiseFraction, final int _numSamples, final long _intervalMS ) {

        // first we build our array of noisy values...
        float[] values = new float[ _numSamples ];
        float currentNominalValue = _start;
        int numSamplesPerCycle = Math.round( _anomalyLength / _anomalyFraction );
        int currentStep = random.nextInt(numSamplesPerCycle);
        for( int i = 0; i < values.length; i++ ) {

            // are we in the anomaly?
            if( currentStep < _anomalyLength ) {
                values[i] = currentNominalValue + addNoise( _anomalyDelta, _noiseFraction / _anomalyFraction );
            }

            // otherwise, we're in good data...
            else {
                values[i] = addNoise( currentNominalValue, _noiseFraction );
            }

            // update our nominal value...
            currentNominalValue = _slope * _intervalMS + currentNominalValue;

            // update our current step...
            currentStep = (currentStep + 1) % numSamplesPerCycle;
        }

        return getSamples( values, _intervalMS );
    }


    private static float addNoise( final float _nominal, final float _noiseFactor ) {
        return _nominal + _noiseFactor * _nominal * random.nextFloat() * (random.nextBoolean() ? -1 : 1 );
    }


    /**
     * Returns an array of samples with timestamps at the given interval (in milliseconds), and values from the given array of floats.  The first
     * sample returned will have a timestamp equal to the time this function was executed.
     *
     * @param _values the samples' values
     * @param _intervalMS the intervals between samples, in milliseconds
     * @return an array of Samples
     */
    private static Sample[] getSamples( final float[] _values, final long _intervalMS ) {
        Instant current = Instant.now();
        Sample[] result = new Sample[_values.length];
        for( int i = 0; i < _values.length; i++ ) {
            result[i] = new Sample( _values[i], current );
            current = current.plusMillis( _intervalMS );
        }
        return result;
    }


    /**
     * Convert comma-separated numeric values to a float array.
     *
     * @param _dataString comma-separated numeric values
     * @return a float array
     */
    private static float[] getData( final String _dataString ) {
        String[] nums = _dataString.split( "," );
        float[] result = new float[nums.length];
        for( int i = 0; i < nums.length; i++ ) {
            result[i] = Float.parseFloat( nums[i] );
        }
        return result;
    }


    private static void out( final String _s ) {
        System.out.println( _s );
    }
}
