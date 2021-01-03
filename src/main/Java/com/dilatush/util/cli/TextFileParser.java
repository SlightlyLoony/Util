package com.dilatush.util.cli;

import com.dilatush.util.Files;

import java.io.File;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a parser that takes the path name of a readable file and returns the contents of that file as a string.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class TextFileParser implements ParameterParser {

    private String errorMsg;


    /**
     * Translates the given string parameter into an object of the given target class.  Returns <code>null</code> if the translation could not be
     * performed for any reason.  Implementations should avoid throwing exceptions, instead relying on {@link #getErrorMessage()} to inform the caller
     * about <i>why</i> parsing failed.
     *
     * @param _parameter The string parameter to parse and translate.
     * @return an object of the target class, translated from the given string parameter, or <code>null</code> if the translation failed.
     */
    @Override
    public Object parse( final String _parameter ) {

        if( isEmpty( _parameter ) ) {
            errorMsg = "Expected file path was not supplied.";
            return null;
        }

        File file = new File( _parameter );

        if( !file.isFile() ) {
            errorMsg = "File path does not resolve to a file: " + file.getAbsolutePath();
            return null;
        }

        if( !file.canRead() ) {
            errorMsg = "Can not read the specified file: " + file.getAbsolutePath();
            return null;
        }

        String contents = Files.readToString( file );

        if( contents == null ) {
            errorMsg = "Problem while reading the specified file: " + file.getAbsolutePath();
            return null;
        }

        return contents;
    }


    /**
     * Return a descriptive error message if the parsing and translation failed for any reason (i.e., {@link #parse(String)} returned
     * <code>null</code>.
     *
     * @return a descriptive error message after parsing and translation failed
     */
    @Override
    public String getErrorMessage() {
        return errorMsg;
    }
}
