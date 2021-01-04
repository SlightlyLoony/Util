package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parser that translates an argument parameter string into an {@link Integer} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class IntegerParser implements ParameterParser {

    private String errorMsg;

    /**
     * Translates the given string argument parameter into an {@link Integer} with the equivalent value.
     *
     * @param _parameter The string argument parameter to parse and translate.
     * @return an {@link Integer} instance with the equivalent value, or {@code null} if there was a translation problem.
     */
    @Override
    public Object parse( final String _parameter ) {

        if( isEmpty( _parameter ) ) {
            errorMsg = "Expected integer string was not supplied.";
            return null;
        }

        int result = 0;
        try {
            result = Integer.parseInt( _parameter );
        }
        catch( NumberFormatException _e ) {
            errorMsg = "Problem parsing integer: " + _e.getMessage();
            return null;
        }
        return result;
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
