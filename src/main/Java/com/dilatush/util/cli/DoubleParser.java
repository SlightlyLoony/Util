package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parser that translates an argument parameter string into a {@link Double} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DoubleParser extends AParameterParser implements ParameterParser {

    /**
     * Translates the given string argument parameter into a {@link Double} with the equivalent value.
     *
     * @param _parameter The string argument parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {

        if( isEmpty( _parameter ) )
            return error( "Expected floating point string was not supplied." );

        try {
            double value = Double.parseDouble( _parameter );
            return result( value );
        }
        catch( NumberFormatException _e ) {
            return error( "Problem parsing floating point: " + _e.getMessage() );
        }
    }
}
