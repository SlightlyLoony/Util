package com.dilatush.util.dns.resolver;

import java.util.function.Consumer;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent cancellable timeouts that call a {@link Consumer Consumer&lt;Object&gt;} if the timeout actually occurs.  The
 * companion {@link Timeouts} class manages a collection of these timeouts.
 */
@SuppressWarnings( "unused" )
public class Timeout {

    private final long             expiration;       // the system time that this timeout expires...
    private boolean                cancelled;        // true if this timeout has been cancelled...
    private boolean                done;             // true if this timeout has expired or has been cancelled...
    private final Consumer<Object> timeoutHandler;   // called upon expiration...
    private final Object           attachment;       // an optional attachment to the timeout...


    /**
     * Create a new instance of this class that will expire at the given number of milliseconds from now, and will call the given
     * {@link Consumer Consumer&lt;Object&gt;} upon timeout expiration with the given attachment.
     *
     * @param _timeoutMS      The time (in milliseconds) from now that this timeout should expire.
     * @param _timeoutHandler The {@link Consumer Consumer&lt;Object&gt;} handler to call upon expiration; the argument is the optional attachment.
     * @param _attachment     The optional attachment, provided when the {@link Consumer Consumer&lt;Object&gt;} is called upon timeout expiration.
     */
    public Timeout( final long _timeoutMS, final Consumer<Object> _timeoutHandler, final Object _attachment ) {

        if( isNull( _timeoutHandler ) )
            throw new IllegalArgumentException( "Missing method to call on timeout");

        expiration     = System.currentTimeMillis() + _timeoutMS;  // calculating the system time at timeout expiration...
        timeoutHandler = _timeoutHandler;
        cancelled      = false;
        done           = false;
        attachment     = _attachment;
    }


    /**
     * Create a new instance of this class that will expire at the given number of milliseconds from now, and will call the given
     * {@link Consumer Consumer&lt;Object&gt;} upon timeout expiration with no attachment.
     *
     * @param _timeoutMS      The time (in milliseconds) from now that this timeout should expire.
     * @param _timeoutHandler The {@link Consumer Consumer&lt;Object&gt;} handler to call upon expiration; the argument is the optional attachment.
     */
    public Timeout( final long _timeoutMS, final Consumer<Object> _timeoutHandler ) {

        this( _timeoutMS, _timeoutHandler, null );
    }


    /**
     * Check to see if this timeout has expired, and if it has expired then call the timeout handler.  The timeout handler will be called no more
     * than once for each timeout.  Returns {@code true} if the timeout has expired, and {@code false} otherwise.
     *
     * @return {@code true} if this timeout has expired.
     */
    public synchronized boolean hasExpired() {

        // if we haven't yet reached our expiration time, then leave with a negative...
        if( System.currentTimeMillis() < expiration )
            return false;

        // if we're already done somehow or if we've been cancelled, return positive without doing anything...
        if( done || cancelled )
            return true;

        // looks like we actually have to expire - so call our handler with the attachment (if we have one)...
        timeoutHandler.accept( attachment );

        // mark us as done (so we don't do this again), and leave with a positive...
        done = true;
        return true;
    }


    /**
     * Attempt to cancel this timeout, returning {@code true} if the cancellation was successful and the timeout handler will not be called, or
     * {@code false} if the cancellation failed (because the timeout has already been cancelled or because the timeout handler has already been
     * called).
     *
     * @return the boolean
     */
    public synchronized boolean cancel() {

        // if we've already been cancelled or expired, just return false...
        if( done )
            return false;

        // otherwise, cancel and return true...
        cancelled = true;
        done = true;
        return true;
    }


    /**
     * Returns {@code true} if this timeout has expired or has been cancelled.
     *
     * @return {@code true} if this timeout has expired or has been cancelled.
     */
    public synchronized boolean isDone() {
        return done;
    }


    /**
     * Returns the system time (the result of {@link System#currentTimeMillis()}) at which this timeout will expire.
     *
     * @return the system time (the result of {@link System#currentTimeMillis()}) at which this timeout will expire.
     */
    public long getExpiration() {
        return expiration;
    }


    /**
     * Returns {@code true} if this timeout has been cancelled.
     *
     * @return {@code true} if this timeout has been cancelled.
     */
    public synchronized boolean isCancelled() {
        return cancelled;
    }


    /**
     * Returns the attachment on this timeout.  The attachment may be of any type, or {@code null}.
     *
     * @return the attachment on this timeout.
     */
    public Object getAttachment() {
        return attachment;
    }
}
