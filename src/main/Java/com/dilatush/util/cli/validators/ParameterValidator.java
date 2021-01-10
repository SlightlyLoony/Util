package com.dilatush.util.cli.validators;

import com.dilatush.util.cli.parsers.ParameterParser;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Implemented by classes that provide parameter validators.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ParameterValidator {


    /**
     * Returns a {@link Result} instance with {@link Result#valid} set to {@code true} if the given parameter value (after parsing and translation)
     * is valid for the associated argument.  Otherwise a {@link Result} instance with {@link Result#valid} set to {@code false} and
     * {@link Result#message} set to an explanatory message is returned.
     *
     * @param _parameterValue The value to validate.
     * @return a {@link Result} instance containing the results of the validation operation
     */
    Result validate( final Object _parameterValue );


    /**
     * Instances of this class contain the results of a {@link ParameterParser}.  All of the fields are {@code public final} and may be safely
     * accessed directly.
     */
    class Result {

        /**
         * Set to {@code true} if the {@link ParameterValidator} successfully validated the parameter value, and {@code false} otherwise.
         */
        public final boolean valid;


        /**
         * If {@link #valid} is {@code false}, this field contains an explanatory message for why the parameter validation failed.  Otherwise this
         * field is {@code null}.
         */
        public final String  message;


        /**
         * Creates a new instance of this class with the given values.
         *
         * @param _valid {@code true} if this result is valid.
         * @param _message If this result is invalid, contains an explanatory message.
         */
        public Result( final boolean _valid, final String _message ) {
            valid = _valid;
            message = _message;

            if( _valid && !isNull( _message ) )
                throw new IllegalArgumentException( "Valid result must have a null message" );

            if( !_valid && isEmpty( _message ) )
                throw new IllegalArgumentException( "Invalid result must have a non-empty message" );
        }
    }
}
