package com.dilatush.util.noisefilter;

import java.util.List;

/**
 * Provides an error calculator based on the mean (average) value of the raw input data.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MeanErrorCalc implements ErrorCalc {


    /**
     * On entry, the given raw data is a simple list of {@link Sample} instances, in chronological order.  On exit, the given list of processed data
     * is the same length as the given list of raw data, containing {@link SampleError} instances in chronological order with absolute error values
     * calculated with the mean value of the given raw data as the basis.  The sum of all errors is returned.
     *
     * @param _rawData the raw data to calculate errors on
     * @param _processedData the processed data with the errors calculated
     * @return the absolute sum of all errors
     */
    @Override
    public float calculateErrors( final List<Sample> _rawData, final List<SampleError> _processedData ) {

        // calculate the mean value of the raw data...
        float mean = 0;
        for( Sample _sample : _rawData ) {
            mean += _sample.value;
        }
        mean /= _rawData.size();

        // calculate all the error values...
        _processedData.clear();
        float totalErrors = 0;
        for( Sample sample : _rawData ) {
            float error = Math.abs( sample.value - mean );
            totalErrors += error;
            _processedData.add( new SampleError( sample, error ) );
        }

        return totalErrors;
    }
}
