package com.dilatush.util.noisefilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an error calculator based on the median value of the raw input data.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MedianErrorCalc implements ErrorCalc {


    /**
     * On entry, the given raw data is a simple list of {@link Sample} instances, in chronological order.  On exit, the given list of processed data
     * is the same length as the given list of raw data, containing {@link SampleError} instances in value order with absolute error values
     * calculated with the median value of the given raw data as the basis. The sum of all errors is returned.
     *
     * @param _rawData the raw data to calculate errors on
     * @param _processedData the processed data with the errors calculated
     * @return the absolute sum of all errors
     */
    @Override
    public float calculateErrors( final List<Sample> _rawData, final List<SampleError> _processedData ) {

        // get a list of our raw data, sorted by value...
        List<Sample> sorted = new ArrayList<>( _rawData );
        sorted.sort( (_sample1, _sample2) -> (int) Math.signum( _sample1.value - _sample2.value ) );

        // calculate the median value from the sorted list...
        int mi = sorted.size() / 2;  // the nominally middle sample's index...
        boolean isOdd = ((sorted.size() & 1) == 1);
        float median = isOdd ? sorted.get( mi ).value : (sorted.get( mi ).value - sorted.get( mi - 1).value) / 2;

        // calculate all the error values...
        _processedData.clear();
        float totalErrors = 0;
        for( Sample sample : _rawData ) {
            float error = Math.abs( sample.value - median );
            totalErrors += error;
            _processedData.add( new SampleError( sample, error ) );
        }

        return totalErrors;
    }
}
