package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parser that translates an argument parameter string into an {@link Boolean} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class BooleanParser implements ParameterParser {

    private String errorMsg;

    /**
     * Translates the given string argument parameter into an {@link Boolean} with the equivalent value.  Parameter values of (case-insensitive)
     * "true", "yes", "y", and "1" translate to {@code true} and all other parameter values to {@code false}.
     *
     * @param _parameter The string argument parameter to parse and translate.
     * @return an {@link Boolean} instance with the equivalent value, or {@code null} if there was a translation problem.
     */
    @Override
    public Object parse( final String _parameter ) {

        if( isEmpty( _parameter ) ) {
            errorMsg = "Expected boolean string was not supplied.";
            return null;
        }

        return
                "true".equalsIgnoreCase( _parameter ) ||
                "yes".equalsIgnoreCase( _parameter ) ||
                "y".equalsIgnoreCase( _parameter ) ||
                "1".equalsIgnoreCase( _parameter );
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
