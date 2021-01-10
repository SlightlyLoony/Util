package com.dilatush.util.cli.validators;


import com.dilatush.util.cli.validators.ParameterValidator.Result;

/**
 * Abstract base class for {@link ParameterValidator} implementations.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AValidatorParser {


    /**
     * Returns a {@link Result} instance that indicates an invalid result with the given explanatory message.
     *
     * @param _message The message explaining why there was a validation problem.
     * @return a {@link Result} instance that indicates an invalid result with the given explanatory message
     */
    protected Result error( final String _message ) {
        return new Result( false, _message );
    }


    /**
     * Returns a {@link Result} instance that indicates a valid result.
     *
     * @return a {@link Result} instance that indicates a valid result
     */
    protected Result valid() {
        return new Result( true, null );
    }
}
