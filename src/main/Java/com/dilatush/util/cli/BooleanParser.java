package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parser that translates an argument parameter string into an {@link Boolean} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class BooleanParser extends AParameterParser implements ParameterParser {

    /**
     * Translates the given string argument parameter into an {@link Boolean} object with the equivalent value.  Parameter values of
     * (case-insensitive) "true", "yes", "y", and "1" translate to {@code true} and all other parameter values to {@code false}.
     *
     * @param _parameter The string argument parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {

        if( isEmpty( _parameter ) )
            return error( "Expected boolean string was not supplied." );

        return result(
                "true".equalsIgnoreCase( _parameter ) ||
                "yes".equalsIgnoreCase( _parameter ) ||
                "y".equalsIgnoreCase( _parameter ) ||
                "1".equalsIgnoreCase( _parameter )
        );
    }
}
