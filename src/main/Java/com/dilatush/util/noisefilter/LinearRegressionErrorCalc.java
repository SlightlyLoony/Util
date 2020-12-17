package com.dilatush.util.noisefilter;

import java.util.List;

/**
 * Provides an error calculator based on a linear (least mean squares) regression of the raw input data.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class LinearRegressionErrorCalc implements ErrorCalc {


    /**
     * On entry, the given raw data is a simple list of {@link Sample} instances, in chronological order.  On exit, the given list of processed data
     * is the same length as the given list of raw data, containing {@link SampleError} instances in chronological order with absolute error values
     * calculated with a linear regression of the given raw data as the basis.
     *
     * @param _rawData the raw data to calculate errors on
     * @param _processedData the processed data with the errors calculated
     * @return the absolute sum of all errors
     */
    @Override
    public float calculateErrors( final List<Sample> _rawData, final List<SampleError> _processedData ) {


        // do the initial regression...
        NoiseFilter.RegressionResult irr = NoiseFilter.linearLMSRegression( _rawData.iterator(), _rawData.get( 0 ).timestamp.toEpochMilli() );

        // calculate the errors and make a list with the results...
        _processedData.clear();
        float totalErrors = 0;
        for( Sample sample : _rawData ) {
            float x = sample.timestamp.toEpochMilli() - _rawData.get( 0 ).timestamp.toEpochMilli();
            float error = Math.abs( sample.value - (x * irr.m + irr.b) );
            totalErrors += error;
            _processedData.add( new SampleError( sample, error ) );
        }

        return totalErrors;
    }
}
