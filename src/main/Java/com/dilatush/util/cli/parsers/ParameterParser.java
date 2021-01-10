package com.dilatush.util.cli.parsers;

import com.dilatush.util.cli.argdefs.ArgDef;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Implemented by classes that provide an argument parameter parser.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ParameterParser {


    /**
     * <p>Translates the given string parameter into an object of a target class.  <i>Which</i> class is determined by the particular implementation.
     * For example, the {@link IntegerParser} translates the parameter string into an {@link Integer} instance.  This method returns a {@link Result}
     * instance that indicates whether the parsing succeeded (the {@link Result#valid} field), what the resulting value was (the {@link Result#value}
     * field), and if there was a parsing problem, an explanatory message (the {@link Result#message} field).</p>
     * <p>Implementations of this interface should <i>not</i> throw exceptions on a parsing error.  Instead, implementations should catch any
     * exceptions and return an invalid result.</p>
     *
     * @param _parameter The string parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    Result parse( final String _parameter );


    /**
     * Instances of this class contain the results of a {@link ParameterParser}.  All of the fields are {@code public final} and may be safely
     * accessed directly.
     */
    class Result {

        /**
         * Set to {@code true} if the {@link ParameterParser} successfully parsed the parameter string and there is a valid {@link #value};
         * {@code false} otherwise.
         */
        public final boolean valid;

        /**
         * If the {@link ParameterParser} successfully parsed the parameter string, this is set to the resulting value (which could be an object of
         * any type).  The type of this result <i>must</i> be assignable to the class {@link ArgDef#type} in the argument definition for the
         * argument using this parser.
         */
        public final Object  value;

        /**
         * If {@link #valid} is {@code false}, this field contains an explanatory message for why the parameter parsing failed.  Otherwise this
         * field is {@code null}.
         */
        public final String  message;


        /**
         * Creates a new instance of this class with the given values.
         *
         * @param _valid {@code true} if this result is valid.
         * @param _value If this result is valid, contains the result of the {@link ParameterParser}.
         * @param _message If this result is invalid, contains an explanatory message.
         */
        public Result( final boolean _valid, final Object _value, final String _message ) {
            valid = _valid;
            value = _value;
            message = _message;

            if( _valid ) {
                if( isNull( _value) )
                    throw new IllegalArgumentException( "Valid result must have a non-null value" );
                if( !isNull( _message ) )
                    throw new IllegalArgumentException( "Valid result must have a null message" );
            }
            else {
                if( !isNull( _value ) )
                    throw new IllegalArgumentException( "Invalid result must have a null value" );
                if( isEmpty( _message ) )
                    throw new IllegalArgumentException( "Invalid result must have a non-empty message" );
            }
        }
    }
}
