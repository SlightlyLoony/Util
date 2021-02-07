package com.dilatush.util.info;

import java.time.Clock;
import java.time.Instant;

/**
 * <p>Instances of this class represent a piece of information (an instance of any class) with a timestamp.  The information stored may implement the
 * {@link IsAvailable} interface to provide a different definition of availability from that provided by this class (where any non-{@code null} value
 * is considered available).</p>
 * <p>While instances of <i>any</i> class may be used as information, by convention any class used as information should have all of its information
 * in {@code public final} fields, including any contained classes.  The objective with this convention is three-fold: (1) to maintain thread safety,
 * (2) to maintain deep immutability, and (3) to eliminate the need for getters.</p>
 * <p>A particular use pattern is worth a bit of discussion: using instances of this class to represent data with any mix of varying precision,
 * differing reliability, or other secondary concerns.  This sort of data is particularly common with sensor data.  For example, perhaps you want
 * to publish a temperature reading for some location, but the actual data might come from any of several sensors, each with a different precision.
 * Furthermore, the sensors are in different locations, with only one being actually <i>at</i> the location in question.  That's a lot of information
 * beyond simple temperature - but you could easily design a record to hold it all: perhaps a lower bound, an upper bound, a location enum, and the
 * estimated temperature.  That record's fields could all be {@code public final}, thus maintaining the immutability, thread safety, and no-getter
 * model for instances of this class.</p>
 * <p>By convention, the class whose instances are being stored in this class should have "sensible" {@code toString()} implementations.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Info<T> {

    /**
     * The information.
     */
    public final T info;

    /**
     * The timestamp (the time that the information was stored in this instance).
     */
    public final Instant timestamp;


    /**
     * Create a new instance of this class containing the given information, which may be {@code null} to indicate that the information is not
     * available.  The timestamp is generated automagically using {@link Clock#systemUTC()}.
     *
     * @param _info The information to store in the new instance, or {@code null} if the information is not available.
     */
    public Info( final T _info ) {
        info = _info;
        timestamp = Instant.now( Clock.systemUTC() );
    }


    /**
     * Return {@code true} if the information is available.  The return value is determined by one of three possibilities:
     * <ul>
     *     <li><b>If {@link #info} is {@code null}</b>: returns {@code false}.</li>
     *     <li><b>If {@link #info} is not {@code null}, and {@link #info} does not implement {@link IsAvailable}</b>: returns {@code true}.</li>
     *     <li><b>If {@link #info} is not {@code null}, and {@link #info} does implement {@link IsAvailable}</b>: returns the result of
     *     {@link IsAvailable#isAvailable()}.</li>
     * </ul>
     *
     * @return {@code true} if the information is available
     */
    public boolean isAvailable() {
        return (info instanceof IsAvailable)
                ? ((IsAvailable) info).isAvailable()
                : (info != null);
    }


    /**
     * Returns a string representing this instance, including the timestamp.
     *
     * @return a string representing this instance, including the timestamp
     */
    @Override
    public String toString() {

        if( info == null )
            return "Unavailable at " + timestamp.toString();

        return (isAvailable() ? "Available" : "Unavailable") + ": " + info.toString() + " at " + timestamp.toString();
    }
}
