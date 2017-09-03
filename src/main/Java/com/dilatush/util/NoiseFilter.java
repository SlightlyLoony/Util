package com.dilatush.util;

/**
 * Implements a filter that combines weighted average of the last five readings with 5% hysteresis.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class NoiseFilter {

    private static final int    NUMBER_OF_READINGS_TO_AVERAGE = 20;
    private static final double HYSTERESIS_AMOUNT             = 0.05d;
    private static final double WEIGHTING_FACTOR              = 1.5;

    private final double nominalThreshold;

    private double biasedThreshold;
    private boolean wasAbove;
    private Averager averager;


    public NoiseFilter( final double _threshold, final double _initialValue ) {

        biasedThreshold = nominalThreshold = _threshold;
        biasedThreshold = getBiasedThreshold( _initialValue );
        averager = new Averager( WEIGHTING_FACTOR, NUMBER_OF_READINGS_TO_AVERAGE );
        wasAbove = (biasedThreshold > nominalThreshold);
    }


    public boolean update( final double _reading ) {

        // insert our new reading and compute a new average...
        double newAverage = averager.sample( _reading );

        // test the current state of affairs...
        boolean nowAbove = (newAverage > biasedThreshold);

        // if our saved state is the same as the current state, just leave...
        if( wasAbove == nowAbove )
            return false;

        // otherwise, our saved state is different than our current state, so we need to flip our bias and indicate that things have changed...
        wasAbove = nowAbove;
        biasedThreshold = getBiasedThreshold( newAverage );
        return true;
    }


    private double getBiasedThreshold( final double _reading ) {

        return nominalThreshold + ((_reading > biasedThreshold ) ? -1 : 1) * HYSTERESIS_AMOUNT * nominalThreshold;
    }


    public boolean isAbove() {
        return wasAbove;
    }


    public boolean isBelow() {
        return !wasAbove;
    }
}
