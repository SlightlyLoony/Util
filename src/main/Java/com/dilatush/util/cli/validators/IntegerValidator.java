package com.dilatush.util.cli.validators;

/**
 * Implements a parameter validator that checks for an integer result whose value is within (inclusive) the given limits.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class IntegerValidator extends AValidatorParser implements ParameterValidator {

    private final int min;
    private final int max;


    /**
     * Creates a new instance of this class with the given range limits.
     *
     * @param _min The minimum value for the acceptable range.
     * @param _max The maximum value for the acceptable range.
     */
    public IntegerValidator( final int _min, final int _max ) {
        min = _min;
        max = _max;
    }


    /**
     * Returns a {@link Result} instance with {@link Result#valid} set to {@code true} if the given parameter value (after parsing and translation)
     * is an integer within the configured range.  Otherwise a {@link Result} instance with {@link Result#valid} set to {@code false} and
     * {@link Result#message} set to an explanatory message is returned.
     *
     * @param _parameterValue The value to validate.
     * @return a {@link Result} instance containing the results of the validation operation
     */
    @Override
    public Result validate( final Object _parameterValue ) {

        if( !(_parameterValue instanceof Integer) )
            return error( "Parameter is not an integer." );

        // get our parameter...
        int value = (Integer) _parameterValue;

        // if our parameter is outside our acceptable range, we've got a problem...
        if( (value < min) || (value > max) )
            return error( "Value " + value + " is outside the allowed range [" + min + ".." + max + "]" );

        return valid();
    }
}
