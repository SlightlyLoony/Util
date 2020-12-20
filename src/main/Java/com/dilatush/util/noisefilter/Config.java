package com.dilatush.util.noisefilter;

import com.dilatush.util.Validatable;

import java.util.logging.Logger;

/**
 * A simple POJO that holds the configuration needed by the {@link NoiseFilter#NoiseFilter(Config)}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Config implements Validatable {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    /**
     * Determines the number of samples held in the filter.  These are the "n" most recently added samples, and
     * their values are the basis for this filter's operations.  Note that until the filter is "full" (because the
     * configured number of samples have been added), it is not possible to get an output value from it.  This value must
     * be at least 2.  If the series of samples has any known periodicity, ideally the number of samples would include at least
     * one cycle of that.
     */
    public  int       numSamples;

    /**
     * This is the error calculator instance to use with this filter (one of {@link MeanErrorCalc}, {@link MedianErrorCalc},
     * {@link LinearRegressionErrorCalc}, or a custom implementation of {@link ErrorCalc}).  This class calculates the
     * "normal" for the samples held in this filter, the absolute error for each sample's value (from the "normal"), and
     * the sum of all the samples' absolute error from the "normal".
     */
    public  ErrorCalc errorCalc;

    /**
     * The maximum number of samples that may be ignored by this filter, as a fraction of the number of samples.  For
     * example, if this value is 0.25, and the filter is configured to hold 40 samples, then at most 10 samples may be
     * ignored ("thrown out") by this filter.  This value must be in the range [0.0 .. 1.0].
     */
    public  float     maxIgnoreFraction;

    /**
     * The maximum total error of the samples that may be ignored by this filter, as a fraction of the total error
     * of all the samples in this filter.  For example, if this value is 0.9 then samples may be ignored ("thrown
     * out") only if the sum of their errors does not exceed 0.9 times the sum of the errors of <i>all</i> the
     * samples held in this filter.
     */
    public  float     maxTotalErrorIgnoreFraction;

    /**
     * The minimum value of the error for any sample to be ignored.  This value must be non-negative.
     */
    public  float     minSampleErrorIgnore;

    private boolean   valid;


    /**
     * Returns <code>true</code> if the state of this object is valid, and <code>false</code> otherwise, after logging a description of the invalid
     * state.
     *
     * @return <code>true</code> if the state of this object is valid.
     */
    @Override
    public boolean isValid() {

        // if we've already validated, just leave...
        if( valid )
            return true;

        // otherwise, trust but verify...
        valid = true;

        if( numSamples < 2 ) {
            LOGGER.severe( "NoiseFilter illegal value for number of samples: " + numSamples );
            valid = false;
        }
        if( errorCalc == null ) {
            LOGGER.severe( "NoiseFilter missing error calculator" );
            valid = false;
        }
        if( (maxIgnoreFraction < 0) || (maxIgnoreFraction > 1) ) {
            LOGGER.severe( "NoiseFilter max ignore fraction out of range ([0..1]): " + maxIgnoreFraction );
            valid = false;
        }
        if( (maxTotalErrorIgnoreFraction < 0) || (maxTotalErrorIgnoreFraction > 1) ) {
            LOGGER.severe( "NoiseFilter max total error ignore fraction out of range ([0..1]): " + maxTotalErrorIgnoreFraction );
            valid = false;
        }
        if( minSampleErrorIgnore < 0 ) {
            LOGGER.severe( "NoiseFilter min sample error ignore must be non-negative: " + minSampleErrorIgnore );
            valid = false;
        }

        return valid;
    }
}
