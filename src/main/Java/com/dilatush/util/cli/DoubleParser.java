package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parser that translates an argument parameter string into a {@link Double} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DoubleParser implements ParameterParser {

    private String errorMsg;

    /**
     * Translates the given string argument parameter into a {@link Double} with the equivalent value.
     *
     * @param _parameter The string argument parameter to parse and translate.
     * @return an {@link Double} instance with the equivalent value, or {@code null} if there was a translation problem.
     */
    @Override
    public Object parse( final String _parameter ) {

        if( isEmpty( _parameter ) ) {
            errorMsg = "Expected floating point string was not supplied.";
            return null;
        }

        double result = 0;
        try {
            result = Double.parseDouble( _parameter );
        }
        catch( NumberFormatException _e ) {
            errorMsg = "Problem parsing floating point: " + _e.getMessage();
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
