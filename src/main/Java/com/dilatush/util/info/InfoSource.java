package com.dilatush.util.info;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Instances of this class are the actual source of information for classes that implement {@link Info}; they are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class InfoSource<T> implements Info<T> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "MMM dd, uuuu hh:mm:ss.SSS" );

    private final T       info;
    private final Instant timestamp;


    /**
     * Creates a new instance of this class with the given information.  The timestamp is created automagically.
     *
     * @param _info the information for this instance.
     */
    public InfoSource( final T _info ) {
        info = _info;
        timestamp = Instant.now( Clock.systemUTC() );
    }


    /**
     * Returns the information held by this information source, or {@code null} if none is available.
     *
     * @return the information held by this information source
     */
    @Override
    public T getInfo() {
        return info;
    }


    /**
     * Returns the timestamp for the information held by this information provider, which is the time that the information was recorded.  Note that a
     * timestamp will be returned even if there is no information available, in which case it is the time that the unavailability was recorded.  It is
     * possible that the timestamp returned by his method is from a different record than the information returned by {@link #getInfo}.  To get the
     * timestamp with the correct information, use {@link #getInfoSource} to get the {@link InfoSource} instance, then use {@code getInfo()} and
     * {@code getInfoTimestamp()} on it.
     *
     * @return the timestamp for the
     */
    @Override
    public Instant getInfoTimestamp() {
        return timestamp;
    }


    /**
     * Returns the {@link InfoSource} instance that is the source of information for this provider.
     *
     * @return the {@link InfoSource} instance that is the source of information for this provider
     */
    @Override
    public InfoSource<T> getInfoSource() {
        return this;
    }


    /**
     * Returns {@code true} if information is available from this information source.  If {@link #getInfo} returns {@code null}, this method will
     * return {@code false}.  Otherwise, if the information class does not implement {@link IsAvailable}, returns {@code true}.  If the information
     * class does implement {@link IsAvailable}, returns the result of {@link IsAvailable#isAvailable}.
     *
     * @return {@code true} if information is available from this information source
     */
    @Override
    public boolean isInfoAvailable() {
        if( info == null ) return false;
        return !(info instanceof IsAvailable) || (((IsAvailable) info).isAvailable());
    }


    /**
     * Returns a string representation of this instance.
     *
     * @return a string representation of this instance
     */
    @Override
    public String toString() {
        ZonedDateTime stamp = ZonedDateTime.ofInstant( timestamp, ZoneId.systemDefault() );
        return "InfoSource: " + formatter.format( stamp ) + ": " + ((info == null) ? "null" : info.toString() );
    }
}
