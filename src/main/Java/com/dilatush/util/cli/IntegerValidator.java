package com.dilatush.util.cli;

/**
 * Implements a parameter validator that checks for an integer result whose value is between the given limits.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class IntegerValidator implements ParameterValidator {

    private final int min;
    private final int max;

    private String errorMsg;


    public IntegerValidator( final int _min, final int _max ) {
        min = _min;
        max = _max;
    }


    /**
     * Returns <code>true</code> if the given parameter value (after parsing and translation) is an instance of {@link Integer} and has a value in
     * the range [min..max], where min and max are the paramters supplied when constructing this instance.
     *
     * @param _parameterValue The value to validate.
     * @return {@code true} if the parameter is an integer in the range [min..max]
     */
    @Override
    public boolean validate( final Object _parameterValue ) {

        if( !(_parameterValue instanceof Integer) ) {
            errorMsg = "Parameter is not an integer.";
            return false;
        }

        int value = (Integer) _parameterValue;

        if( (value < min) || (value > max) ) {
            errorMsg = "Value " + value + " is outside the allowed range [" + min + ".." + max + "]";
            return false;
        }

        return true;
    }


    /**
     * Returns an error message describing the reason why validation failed if {@link #validate(Object)} returns <code>false</code>.
     *
     * @return an error message describing the reason why validation failed
     */
    @Override
    public String getErrorMessage() {
        return errorMsg;
    }
}
