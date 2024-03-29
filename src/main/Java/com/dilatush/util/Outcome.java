package com.dilatush.util;

import static com.dilatush.util.Strings.isEmpty;

/**
 * <p>Encapsulates the notion of the outcome of an operation.</p>
 * <p>If the ok flag is true, then the message field will be {@code null}, the cause field will be {@code null}, and the optional info field may
 * contain additional information about the outcome of an operation that produces some information (other than simply success or failure).</p>
 * <p>If the ok flag is false, then the info field will be {@code null} or contain invalid or incomplete results, the message field will contain some explanatory text about the
 * reason for failure, and the cause field may optionally contain a {@link Throwable} related to the failure.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public record Outcome<T>( boolean ok, String msg, Throwable cause, T info ) {


    /**
     * Compact canonical constructor that verifies the correct parameters are supplied.  While this constructor is public, using one of the
     * {@link Forge} factory methods is less prone to errors and more convenient.
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
            Checks.isTrue( !isEmpty( msg ), "Message must be supplied with not ok outcome" );
        }
    }


    /**
     * Returns {@code true} if this instance is <i>not</i> ok.
     *
     * @return {@code true} if this instance is <i>not</i> ok.
     */
    public boolean notOk() {
        return !ok();
    }


    /**
     * Provides a factory class for {@link Outcome}, to provide easy and safe construction of new {@link Outcome} instances.
     *
     * @param <T> The type of information that may be contained by an Outcome.
     */
    public static class Forge<T> {


        /**
         * Creates a new instance of {@link Outcome} that is ok and has no additional info.
         */
        public Outcome<T> ok() {
            return new Outcome<>( true, null, null, null );
        }


        /**
         * Creates a new instance of {@link Outcome} that is ok and has the given additional info.
         *
         * @param _info If the overall outcome of an operation was successful, this field can optionally contain some additional information.  The
         *              intent of this field is to contain any information (other than simply success or failure) produced by the operation.
         */
        public Outcome<T> ok( final T _info ) {
            return new Outcome<>( true, null, null, _info );
        }


        /**
         * Creates a new instance of {@link Outcome} that is not ok and has the given explanatory message.
         *
         * @param _msg A mandatory explanatory message about why the operation failed.
         */
        public Outcome<T> notOk( final String _msg ) {
            return new Outcome<>( false, _msg, null, null );
        }


        /**
         * Creates a new instance of {@link Outcome} that is not ok and has the given explanatory message and invalid or incomplete results.
         *
         * @param _msg A mandatory explanatory message about why the operation failed.
         * @param _info Invalid or incomplete results.
         */
        public Outcome<T> notOk( final String _msg, final T _info ) {
            return new Outcome<>( false, _msg, null, _info );
        }


        /**
         * Creates a new instance of {@link Outcome} that is not ok and has the given explanatory message and {@link Throwable} cause.
         *
         * @param _msg A mandatory explanatory message about why the operation failed.
         * @param _cause When the overall outcome of an operation is unsuccessful, this optional {@link Throwable} may supply some additional
         *               information about the cause of a failure.
         */
        public Outcome<T> notOk( final String _msg, final Throwable _cause ) {
            return new Outcome<>( false, _msg, _cause, null );
        }


        /**
         * Creates a new instance of {@link Outcome} that is not ok and takes the explanatory message and {@link Throwable} cause from the
         * given outcome.
         *
         * @param _outcome The outcome to extract the message and cause from.
         */
        public Outcome<T> notOk( final Outcome<?> _outcome ) {
            return new Outcome<>( false, _outcome.msg, _outcome.cause, null );
        }


        /**
         * Creates a new instance of {@link Outcome} that is not ok and has the given explanatory message and {@link Throwable} cause, and invalid or incomplete results.
         *
         * @param _msg A mandatory explanatory message about why the operation failed.
         * @param _cause When the overall outcome of an operation is unsuccessful, this optional {@link Throwable} may supply some additional
         *               information about the cause of a failure.
         * @param _info Invalid or incomplete results.
         */
        public Outcome<T> notOk( final String _msg, final Throwable _cause, final T _info ) {
            return new Outcome<>( false, _msg, _cause, _info );
        }


        /**
         * Creates a new instance of {@link Outcome} that is not ok and has no explanatory message and {@link Throwable} cause.
         *
         * @param _cause When the overall outcome of an operation is unsuccessful, this optional {@link Throwable} may supply some additional
         *               information about the cause of a failure.
         */
        public Outcome<T> notOk( final Throwable _cause ) {
            return new Outcome<>( false, "See cause", _cause, null );
        }


        /**
         * Creates a new instance of {@link Outcome} that is not ok and has no explanatory message and {@link Throwable} cause, and invalid or incomplete results.
         *
         * @param _cause When the overall outcome of an operation is unsuccessful, this optional {@link Throwable} may supply some additional
         *               information about the cause of a failure.
         * @param _info Invalid or incomplete results.
         */
        public Outcome<T> notOk( final Throwable _cause, final T _info ) {
            return new Outcome<>( false, "See cause", _cause, _info );
        }
    }


    /**
     * Returns a string representing the contents of this instance.
     *
     * @return a string representing the contents of this instance
     */
    public String toString() {
        if( ok ) {
            if( info == null )
                return "OK";
            else
                return "OK: " + info;
        }
        else {
            if( cause == null )
                return "Not OK: " + msg;
            else
                return "Not OK: " + msg + "\n" + cause;
        }
    }
}
