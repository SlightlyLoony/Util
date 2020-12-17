package com.dilatush.util.noisefilter;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SampleError {
    public final Sample sample;
    public final float error;


    public SampleError( final Sample _sample, final float _error ) {
        sample = _sample;
        error = _error;
    }
}
