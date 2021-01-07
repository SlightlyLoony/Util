package com.dilatush.util.cli;

import java.io.File;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parser that translates a string into a {@link File} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class PathParser extends AParameterParser implements ParameterParser {


    /**
     * Translates the given string parameter into a {@link File} instance.
     *
     * @param _parameter The string parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {
        return isEmpty( _parameter ) ? error( "Expected file path was not supplied." ) : result( new File( _parameter ) );
    }
}
