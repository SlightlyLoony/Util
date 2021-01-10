package com.dilatush.util.cli.parsers;

import com.dilatush.util.Files;

import java.io.File;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a parser that takes the path name of a readable file and returns the contents of that file as a string.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class TextFileParser extends AParameterParser implements ParameterParser {


    /**
     * Using the given parameter string as the file path, this parser returns the contents of that file as a string.
     *
     * @param _parameter The string parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {

        if( isEmpty( _parameter ) )
            return error( "Expected file path was not supplied." );

        File file = new File( _parameter );

        if( !file.isFile() )
            return error( "File path does not resolve to a file: " + file.getAbsolutePath() );

        if( !file.canRead() )
            return error( "Can not read the specified file: " + file.getAbsolutePath() );

        String contents = Files.readToString( file );

        if( contents == null )
            return error( "Problem while reading the specified file: " + file.getAbsolutePath() );

        return result( contents );
    }
}
