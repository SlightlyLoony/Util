package com.dilatush.util.noisefilter;

import java.util.List;

/**
 * Classes implementing this interface provide an error calculator for use by an instance of {@link NoiseFilter}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ErrorCalc {

    /**
     * On entry, the given raw data is a simple list of {@link Sample} instances, in chronological order.  This list should not be modified in any
     * way.  The given processed data is a list of {@link SampleError}, which may or may not have data.  On exit, this list should be exactly the
     * same length as the raw data, may be in any order at all, and all the absolute error values will be calculated.  The sum of all calculated
     * errors is returned.
     *
     * @param _rawData the raw data to calculate errors on
     * @param _processedData the processed data with the errors calculated
     * @return the absolute sum of all calculated errors
     */
    float calculateErrors( final List<Sample> _rawData, final List<SampleError> _processedData );
}
