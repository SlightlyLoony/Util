package com.dilatush.util.noisefilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Implements a filter that removes noise from a series of measurement values, where "noise" is defined as readings that are outliers from a
 * computed "normal" to the data.  Three methods are provided in this package for computing the "normal": mean {@link MeanErrorCalc}, median
 * {@link MedianErrorCalc}, and linear regression {@link LinearRegressionErrorCalc}.  Additional methods may be added by creating classes that
 * implement the {@link ErrorCalc} interface, as the supplied methods do.</p>
 * <p>This approach works well for data series where any given sample <i>should</i> have a value that's relatively close to the
 * preceding sample values, such as a series of temperature or humidity values.</p>
 * <p>The filter keeps a history of the past "n" samples, where "n" is determined at the time the filter is instantiated.  The expectation is that
 * samples will be added to the filter at intervals, which may or may not be regular.  When a filtered value is requested, following steps are
 * performed:</p><ol>
 *     <li>The "normal" described above is calculated, and then the error from the normal is calculated for each sample in the filter.</li>
 *     <li>In descending order of the absolute error magnitude, samples are excluded from consideration until one of the following occurs:<ol type="a">
 *         <li>The configured maximum fraction of samples that may be ignored is reached.</li>
 *         <li>The configured maximum fraction of the total error (sum of all the samples' errors) is reached.</li>
 *         <li>A sample with an error smaller than the configured maximum sample error to be ignored is reached.</li>
 *     </ol></li>
 *     <li>A linear least mean squares regression is computed on the samples that were not excluded.</li>
 *     <li>The value requested is computed for the specified time using the results of the preceding regression.</li>
 * </ol>
 * <p>This filtering method will be most challenged if the non-noisy part of the data stream is changing rapidly.</p>
 */
public class NoiseFilter {

    private final List<Sample> samples;                      // the samples held in this filter
    private final ErrorCalc    errorCalc;                    // the error calculator
    private final int          numSamples;                   // the maximum number of the most recent samples to hold in this filter
    private final int          firstMustKeep;                // the index of the first sample we must keep
    private final float        maxTotalErrorIgnoreFraction;  // the maximum fraction of the total error to ignore
    private final float        minSampleErrorIgnore;         // the minimum absolute value of a single sample's error to ignore

    private Sample lastSample;


    /**
     * <p>Creates a new instance of this class configured with the given arguments, explained in detail in the parameter descriptions below.
     * @param _numSamples determines the number of samples held in the filter.  These are the "n" most recently added samples, and
     *                    their values are the basis for this filter's operations.  Note that until the filter is "full" (because the
     *                    configured number of samples have been added), it is not possible to get an output value from it.  This value must
     *                    be at least 2.
     * @param _errorCalc is the error calculator instance to use with this filter (one of {@link MeanErrorCalc}, {@link MedianErrorCalc},
     *                           {@link LinearRegressionErrorCalc}, or a custom implementation of {@link ErrorCalc}).  This class calculates the
     *                           "normal" for the samples held in this filter, the absolute error for each sample's value (from the "normal"), and
     *                           the sum of all the samples' absolute error from the "normal".
     * @param _maxIgnoreFraction the maximum number of samples that may be ignored by this filter, as a fraction of the number of samples.  For
     *                           example, if this value is 0.25, and the filter is configured to hold 40 samples, then at most 10 samples may be
     *                           ignored ("thrown out") by this filter.  This value must be in the range [0.0 .. 1.0].
     * @param _maxTotalErrorIgnoreFraction the maximum total error of the samples that may be ignored by this filter, as a fraction of the total error
     *                                     of all the samples in this filter.  For example, if this value is 0.9 then samples may be ignored ("thrown
     *                                     out") only if the sum of their errors does not exceed 0.9 times the sum of the errors of <i>all</i> the
     *                                     samples held in this filter.
     * @param _minSampleErrorIgnore the minimum value of the error for any sample to be ignored.  This value must be non-negative.
     */
    public NoiseFilter( final int _numSamples, final ErrorCalc _errorCalc,
                        final float _maxIgnoreFraction, final float _maxTotalErrorIgnoreFraction, final float _minSampleErrorIgnore ) {

        // TODO: validate arguments (fail early)...
        if( _numSamples < 2 )
            throw new IllegalArgumentException( "Illegal value for number of samples: " + _numSamples );
        if( _errorCalc == null )
            throw new IllegalArgumentException( "Missing error calculator" );
        if( (_maxIgnoreFraction < 0) || (_maxIgnoreFraction > 1) )
            throw new IllegalArgumentException( "Max ignore fraction out of range ([0..1]): " + _maxIgnoreFraction);
        if( (_maxTotalErrorIgnoreFraction < 0) || (_maxTotalErrorIgnoreFraction > 1) )
            throw new IllegalArgumentException( "Max total error ignore fraction out of range ([0..1]): " + _maxTotalErrorIgnoreFraction );
        if( _minSampleErrorIgnore < 0 )
            throw new IllegalArgumentException( "Min sample error ignore must be non-negative: " + _minSampleErrorIgnore );

        numSamples                  = _numSamples;
        errorCalc                   = _errorCalc;
        samples                     = new ArrayList<>( numSamples );
        firstMustKeep               = Math.round( _maxIgnoreFraction * numSamples );
        minSampleErrorIgnore = _minSampleErrorIgnore;
        maxTotalErrorIgnoreFraction = _maxTotalErrorIgnoreFraction;
    }


    /**
     * Add the given sample to the samples currently in the filter.  If adding this sample would cause the number of samples to exceed the configured
     * maximum number of samples (see {@link NoiseFilter}, then the oldest sample in the filter will be deleted before the given sample is added.
     * If the sample being added is out of chronological order (i.e., has a timestamp earlier than the last sample added), this method throws an
     * {@link IllegalArgumentException}.
     *
     * @param _sample the sample to add
     */
    public void add( final Sample _sample ) {

        // make sure we're not trying to add samples out-of-order...
        if( (lastSample != null) && (!lastSample.timestamp.isBefore( _sample.timestamp )) )
            throw new IllegalArgumentException( "Sample out of chronological order" );

        lastSample = _sample;

        // if we already have the maximum number of samples allowed, delete the oldest one before adding the new one...
        if( samples.size() >= numSamples )
            samples.remove( 0 );

        // add our new sample to the end of the list...
        samples.add( _sample );
    }


    /**
     * Gets a {@link Sample} for the given time, as computed by this filter.  See {@link NoiseFilter} for details on how this works.
     *
     * @param _sampleTime the time for the desired sample.  This time should be reasonably close to the time of the last sample added to this filter.
     *                    If it is far away from that time, the value of the returned sample may be surprising.
     * @return the {@link Sample} computed by this filter for the given time.
     */
    public Sample getFilteredAt( final Instant _sampleTime ) {

        // if we don't have enough samples yet, just leave with a null...
        if( samples.size() < numSamples )
            return null;

        // calculate all our errors...
        List<SampleError> errors = new ArrayList<>( samples.size() );
        float totalError = errorCalc.calculateErrors( samples, errors );

        // sort the errors in descending order of error magnitude...
        errors.sort( ( o1, o2 ) -> (int) Math.signum(o2.error - o1.error) );

        // find the index of the first value we want to keep...
        float totalIgnore = (1 - maxTotalErrorIgnoreFraction) * totalError;
        int keepIndex = 0;
        SampleError se = errors.get( 0 );
        while( (keepIndex < firstMustKeep) && ((totalError - se.error) > totalIgnore) && (se.error > minSampleErrorIgnore) ) {
            keepIndex++;
            se = errors.get(keepIndex);
            totalError -= se.error;
        }

        // do a regression on the subset of samples we're keeping...
        Iterator<Sample> it = new SampleRecordIterator( errors.subList( keepIndex, errors.size() ) );
        RegressionResult srr = linearLMSRegression( it, samples.get( 0 ).timestamp.toEpochMilli() );

        // now compute the value our caller wants, based on the results of that second regression...
        float x = _sampleTime.toEpochMilli() - samples.get( 0 ).timestamp.toEpochMilli();
        float y = srr.m * x + srr.b;
        return new Sample( y, _sampleTime );
    }


    /**
     * Computes a least mean squares linear regression on the given sample source, using the given base time.  The base time is simply subtracted
     * from the time of every sample in order to keep the X (time) values within a reasonable range when squared in the course of the regression
     * calculations.
     *
     * @param _sampleSource an {@link Iterator} over the source of the samples to compute a linear regression for.
     * @param _baseTime the base time (in milliseconds) for this computation.  Generally this is the time of the oldest sample.
     * @return the {@link RegressionResult} containing the result of the linear regression.
     */
    public static RegressionResult linearLMSRegression( final Iterator<Sample> _sampleSource, final long _baseTime ) {

        float sumX  = 0;
        float sumX2 = 0;
        float sumXY = 0;
        float sumY  = 0;
        float sumY2 = 0;
        int n = 0;

        // collect our sums for all the samples...
        while( _sampleSource.hasNext() ) {

            Sample sample = _sampleSource.next();
            float x = sample.timestamp.toEpochMilli() - _baseTime;
            float y = sample.value;
            sumX  += x;
            sumY  += y;
            sumX2 += x * x;
            sumY2 += y * y;
            sumXY += x * y;
            n++;
        }

        // compute our m, b, and r values...
        float d = n * sumX2 - sumX * sumX;
        float m = (n * sumXY - sumX * sumY )/ d;
        float b = (sumY * sumX2 - sumX * sumXY) / d;
        float r = (float) Math.abs((sumXY - sumX * sumY / n) / Math.sqrt( (sumX2 - sumX * sumX / n) * (sumY2 - sumY * sumY / n ) ));
        return new RegressionResult( m, b, r );
    }


    /**
     * A simple immutable tuple that contains the results of a linear regression calculation.
     */
    public static class RegressionResult {

        /**
         * The slope of the regression line.
         */
        public final float m;

        /**
         * The Y-intercept of the regression line.
         */
        public final float b;

        /**
         * The correlation coefficient of the regression line, in the range [0..1], where values closer to 1 indicate a higher correlation.
         */
        public final float r;


        public RegressionResult( final float _m, final float _b, final float _r ) {
            m = _m;
            b = _b;
            r = _r;
        }
    }


    private static class SampleRecordIterator implements Iterator<Sample> {

        private final Iterator<SampleError> sampleRecordIterator;


        public SampleRecordIterator( final List<SampleError> _sampleErrors ) {
            sampleRecordIterator = _sampleErrors.iterator();
        }


        @Override
        public boolean hasNext() {
            return sampleRecordIterator.hasNext();
        }


        @Override
        public Sample next() {
            return sampleRecordIterator.next().sample;
        }
    }
}
