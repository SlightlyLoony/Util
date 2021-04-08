package com.dilatush.util;

import static com.dilatush.util.Strings.isEmpty;

/**
 * <p>Encapsulates the notion of the outcome of an operation.</p>
 * <p>If the ok flag is true, then the message field will be {@code null}, the cause field will be {@code null}, and the optional info field may
 * contain additional information about the outcome of an operation that produces some information (other than simply success or failure).</p>
 * <p>If the ok flag is false, then the info field will be {@code null}, the message field will contain some explanatory text about the reason for
 * failure, and the cause field may optionally contain a {@link Throwable} related to the failure.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public record Outcome<T>( boolean ok, String msg, Throwable cause, T info ) {


    /** Convenience instance of {@link Outcome} with OK results and no info */
    public static final Outcome<?> OK = new Outcome<>();


    /**
     * Compact canonical constructor that verifies the correct parameters are supplied.  While this constructor is public, using one of the other
     * constructors is probably more convenient.
     *
     * @param ok  The overall outcome of an operation, {@code true} if it was successful, and {@code false} if it was not.
     * @param msg When the overall outcome of an operation is unsuccessful, this is a mandatory explanatory message about why the operation failed.
     *            If the overall outcome was successful, this must be {@code null}.
     * @param cause When the overall outcome of an operation is unsuccessful, this optional {@link Throwable} may supply some additional information
     *              about the cause of a failure.  If the overall outcome was successful, this must be {@code null}.
     * @param info If the overall outcome of an operation was successful, this field can optionally contain some additional information.  The intent
     *             of this field is to contain any information (other than simply success or failure) produced by the operation.  If the overall
     *             outcome of an operation was unsuccessful, this field must be {@code null}.
     */
    public Outcome {

        // fail fast if we've got any illegal state...
        if( ok ) {
            Checks.isTrue( msg == null, "Message must not be supplied with ok outcome" );
            Checks.isTrue( cause == null, "Cause must not be supplied with ok outcome" );
        } else {
            Checks.isTrue( info == null, "Info must not be supplied with not ok outcome" );
            Checks.isTrue( !isEmpty( msg ), "Message must be supplied with not ok outcome" );
        }
    }


    /**
     * Creates a new instance of {@link Outcome} that is ok and has no info.
     */
    public Outcome() {
        this( true, null, null, null );
    }


    /**
     * Creates a new instance of {@link Outcome} that is ok and has the given additional info.
     *
     * @param info If the overall outcome of an operation was successful, this field can optionally contain some additional information.  The intent
     *             of this field is to contain any information (other than simply success or failure) produced by the operation.  If the overall
     *             outcome of an operation was unsuccessful, this field must be {@code null}.
     */
    public Outcome( final T info ) {
        this( true, null, null, info );
    }


    /**
     * Creates a new instance of {@link Outcome} that is not ok and has the given explanatory message.  Note that if the generic type of this class
     * is {@link String} then this constructor will have the same signature as the {@link Outcome(T)} constructor.  In this case, use the
     * {@link Outcome(String,Boolean)} constructor to disambiguate the two constructors.
     *
     * @param msg When the overall outcome of an operation is unsuccessful, this is a mandatory explanatory message about why the operation failed.
     *            If the overall outcome was successful, this must be {@code null}.
     */
    public Outcome( final String msg ) {
        this( false, msg, null, null );
    }


    /**
     * Creates a new instance of {@link Outcome} that is not ok and has the given explanatory message.  See {@link Outcome(String)} for an explanation
     * of why this constructor exists.
     *
     * @param msg When the overall outcome of an operation is unsuccessful, this is a mandatory explanatory message about why the operation failed.
     *            If the overall outcome was successful, this must be {@code null}.
     * @param nothing This parameter is ignored; it exists only to provide this constructor with a different signature.
     */
    public Outcome( final String msg, final boolean nothing ) {
        this( false, msg, null, null );
    }


    /**
     * Creates a new instance of {@link Outcome} that is not ok and has the given explanatory message and {@link Throwable} cause.
     *
     * @param msg When the overall outcome of an operation is unsuccessful, this is a mandatory explanatory message about why the operation failed.
     *            If the overall outcome was successful, this must be {@code null}.
     * @param cause When the overall outcome of an operation is unsuccessful, this optional {@link Throwable} may supply some additional information
     *              about the cause of a failure.  If the overall outcome was successful, this must be {@code null}.
     */
    public Outcome( final String msg, final Throwable cause ) {
        this( false, msg, cause, null );
    }
}
