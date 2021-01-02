package com.dilatush.util.cli;

/**
 * Implemented by classes that provide parameter validators.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ParameterValidator {


    /**
     * Returns <code>true</code> if the given parameter value (after parsing and translation) is valid for the associated argument.
     *
     * @param _parameterValue The value to validate.
     * @return <code>true</code> if the given parameter value (after parsing and translation) is valid for the associated argument
     */
    boolean validate( final Object _parameterValue );


    /**
     * Returns an error message describing the reason why validation failed if {@link #validate(Object)} returns <code>false</code>.
     *
     * @return an error message describing the reason why validation failed
     */
    String getErrorMessage();
}
