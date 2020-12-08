package com.dilatush.util.noisefilter;

import java.time.Instant;

/**
 * A simple tuple containing a sample's value and its timestamp.
 */
public class Sample {

    public final float   value;
    public final Instant timestamp;


    /**
     * Creates a new instance of this class with the given value and timestamp.
     *
     * @param _value the value of this sample
     * @param _timestamp the timestamp of this sample
     */
    public Sample( final float _value, final Instant _timestamp ) {
        value = _value;
        timestamp = _timestamp;
    }


    @Override
    public String toString() {
        return "Sample: (value = " + value + ", timestamp = " + timestamp + ")";
    }
}
