package com.dilatush.util.info;

import java.time.Instant;

/**
 * Implemented by classes that provide information of the given type T.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface Info<T> {


    /**
     * Returns the information held by this information provider, or {@code null} if none is available.  It is possible that the information returned
     * by this method is from a different record than the timestamp returned by {@link #getInfoTimestamp}.  To get the information with the correct
     * timestamp, use {@link #getInfoSource} to get the {@link InfoSource} instance, then use {@code getInfo()} and {@code getInfoTimestamp()} on it.
     *
     * @return the information held by this information source
     */
    T getInfo();


    /**
     * Returns the timestamp for the information held by this information provider, which is the time that the information was recorded.  Note that a
     * timestamp will be returned even if there is no information available, in which case it is the time that the unavailability was recorded.  It is
     * possible that the timestamp returned by his method is from a different record than the information returned by {@link #getInfo}.  To get the
     * timestamp with the correct information, use {@link #getInfoSource} to get the {@link InfoSource} instance, then use {@code getInfo()} and
     * {@code getInfoTimestamp()} on it.
     *
     * @return the timestamp for the
     */
    Instant getInfoTimestamp();


    /**
     * Returns the {@link InfoSource} instance that is the source of information for this provider.
     *
     * @return the {@link InfoSource} instance that is the source of information for this provider
     */
    InfoSource<T> getInfoSource();


    /**
     * Returns {@code true} if information is available from this information source.
     *
     * @return {@code true} if information is available from this information source
     */
    boolean isInfoAvailable();
}
