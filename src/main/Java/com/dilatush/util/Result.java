package com.dilatush.util;

/**
 * Instances of this class represent the typed result of a function, along with an indication of the validity of the result, the exactness of the
 * result, and an optional string message (mainly for explanatory messages about errors).  Instances of this class are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Result<T> {

    /**
     * The value of the result, or {@code null} if there was no result.
     */
    public final T       value;

    /**
     * {@code True} if the operation that produced this result was successful.  If this value is {@code true}, then the {@link #value} field will
     * not be {@code null}.  The value of this field is derived from {@link #type} as a convenience to the programmer.
     */
    public final boolean valid;

    /**
     * {@code True} if the operation that produced this result was successful, and the result was an exact result.  If this value is {@code true},
     * then the {@link #value} will not be {@code null}.  The value of this field is derived from the {@link #type} as a convenience to the
     * programmer.
     */
    public final boolean exact;

    /**
     * The optional message associated with this result.  Generally there will only be a message in the case of an invalid result, but there is
     * no actual prohibition on messages for valid result.
     */
    public final String  message;

    /**
     * The type of result, indicating {@link Type#VALID_EXACT}, {@link Type#VALID_INEXACT}, or {@link Type#INVALID}.
     */
    public final Type    type;


    /**
     * Create a new instance of this class with the given value, type, and message.  The {@link #exact} and {@link #valid} fields are derived from
     * the given type as a convenience to the programmer.
     *
     * @param _value The value of this result.
     * @param _type The type of this result.
     * @param _message The message associated with this result.
     */
    public Result( final T _value, final Type _type, final String _message ) {
        value   = _value;
        type    = _type;
        message = _message;
        valid   = (type != Type.INVALID );
        exact   = (type == Type.VALID_EXACT);
    }


    /**
     * Creates a new instance of this class with the given value, a type of {@link Type#VALID_EXACT}, and a {@code null} message.
     *
     * @param _value The value of this result.
     */
    public Result( final T _value ) {
        this( _value, Type.VALID_EXACT, null );
    }


    /**
     * Creates a new instance of this class with the given message, a type of {@link Type#INVALID}, and a {@code null} value.
     *
     * @param _message The message of this result.
     */
    public Result( final String _message ) {
        this( null, Type.INVALID, _message );
    }


    /**
     * The type of a result: VALID_EXACT, VALID_INEXACT, or INVALID.
     */
    public enum Type {

        /**
         * The result is valid and exact.
         */
        VALID_EXACT,

        /**
         * The result is valid and inexact.
         */
        VALID_INEXACT,

        /**
         * The result is invalid.
         */
        INVALID
    }
}
