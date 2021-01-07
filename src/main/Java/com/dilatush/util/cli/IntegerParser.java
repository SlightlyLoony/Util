package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parser that translates an argument parameter string into an {@link Integer} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class IntegerParser extends AParameterParser implements ParameterParser {


    /**
     * Translates the given string argument parameter into an {@link Integer} instance with the equivalent value.
     *
     * @param _parameter The string argument parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {

        if( isEmpty( _parameter ) )
            return error( "Expected integer string was not supplied." );

        try {
            int value = Integer.parseInt( _parameter );
            return result( value );
        }
        catch( NumberFormatException _e ) {
            return error( "Problem parsing integer: " + _e.getMessage() );
        }
    }
}
