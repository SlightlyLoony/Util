package com.dilatush.util.cli;

/**
 * Implemented by classes that provide an argument parameter parser.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ParameterParser {


    /**
     * Translates the given string parameter into an object of the given target class.  Returns <code>null</code> if the translation could not
     * be performed for any reason.  Implementations should avoid throwing exceptions, instead relying on {@link #getErrorMessage()} to inform the
     * caller about <i>why</i> parsing failed.
     *
     * @param _parameter The string parameter to parse and translate.
     * @return an object of the target class, translated from the given string parameter, or <code>null</code> if the translation failed.
     */
    Object parse( final String _parameter );


    /**
     * Return a descriptive error message if the parsing and translation failed for any reason (i.e., {@link #parse(String)} returned
     * <code>null</code>.
     *
     * @return a descriptive error message after parsing and translation failed
     */
    String getErrorMessage();
}
