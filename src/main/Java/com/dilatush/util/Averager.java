package com.dilatush.util;

/**
 * Implements a general purpose arithmetic averager over an arbitrary number of sequential samples and with an arbitrary weighting factor.  If the samples
 * are numbered [0..n] and the weighting factor is w, then the weight for any given sample is w^(n+1).  Therefore if the weighting factor is 1.0, then
 * this averager behaves as a straight arithmetic averager over the given samples, because the weight for each sample is 1.0.  To weight recent samples more
 * than less recent samples, use a weighting factor greater than 1.0.
 *
 * Instances of this class are mutable and not threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Averager {

    private final double weightingFactor;
    private final int numberOfSamples;

    private double[] samples;
    private int first = 0;
    private int next = 0;
    private int size = 0;
    private double value = 0;
    private double volatility = 0;


    /**
     * Creates a new instance of this class with the given weighting factor and number of samples.  The initial state is an empty sample collection and a
     * value of 0.0.
     *
     * @param _weightingFactor
     *      the weighting factor for newer samples, typically between 1 and 2 (though other values are also valid)
     * @param _numberOfSamples
     *      the number of samples to be averaged, which must be >= 1
     */
    public Averager( final double _weightingFactor, final int _numberOfSamples ) {

        // sanity check...
        Checks.isTrue( _numberOfSamples >= 1, "Averager number of samples must be >=1" );
        Checks.isTrue( Double.isFinite( _weightingFactor ), "Averager weighting factor must be a finite number" );

        weightingFactor = _weightingFactor;
        numberOfSamples = _numberOfSamples;
        samples = new double[numberOfSamples];
    }


    /**
     * Adds a new sample to this instance, computes the resulting average, and returns the average.
     *
     * @param _sample
     *      the new sample to add to this averager
     * @return
     *      the new average after the given sample has been added
     */
    public double sample( final double _sample ) {

        insertSample( _sample );

        value = average();
        volatility = volatility();

        return value;
    }


    public double getValue() {
        return value;
    }


    public double getVolatility() {
        return volatility;
    }


    private double average() {

        int index = first;
        double wf = 1.0;
        double swf = 0;
        double accum = 0;

        for( int i = 0; i < size; i++ ) {

            accum += wf * samples[index];
            swf += wf;
            wf *= weightingFactor;
            index = next( index );
        }

        return accum / swf;
    }


    /**
     * Computes the "volatility" of the currently held samples.  This is defined as the sum of the absolute values of the samples' deviation from
     * the mean, divided by the number of samples.
     *
     * @return
     *      the computed volatility
     */
    private double volatility() {

        int index = first;
        double v = 0;

        for( int i = 0; i < size; i++ ) {

            v += Math.abs( samples[index] - value );
            index = next( index );
        }

        return v / size;
    }



    private void insertSample( final double _sample ) {

        samples[next] = _sample;
        next = next( next );
        if( size >= numberOfSamples ) {
            first = next( first );
        }
        else {
            size++;
        }
    }


    private int next( final int _pointer ) {
        return (_pointer + 1) % numberOfSamples;
    }
}
