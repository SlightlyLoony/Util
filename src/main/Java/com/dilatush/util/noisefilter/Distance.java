package com.dilatush.util.noisefilter;

/**
 * <p>Implemented by functions that compute the distance for a {@link NoiseFilter}.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface Distance {

    /**
     * <p>Returns the computed distance between the given samples.  There are no units or absolute values for this computed distance; it is simply
     * a relative measure of the notional "distance" between the two given samples, where zero means the values are coincident and larger values
     * means more distance.</p>
     * <p>Generally there are two kinds of "distance" that the computation must consider:</p>
     * <ul>
     *     <li>The difference between the values of the two samples.  Generally a smaller difference would mean less "distance"</li>
     *     <li>The time between the timestamps of the two samples.  Generally a smaller amount of time would mean less "distance".</li>
     * </ul>
     * <p>See {@link NoiseFilterTests} for an example implementation.</p>
     *
     * @param _newSample the new sample being added to the noise filter
     * @param _existingSample an existing sample already in the noise filter
     * @return the computed distance value between the given samples, zero or larger, where a larger value means the samples are more distant
     */
    float calculate( final Sample _newSample, final Sample _existingSample );
}
