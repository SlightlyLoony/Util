package com.dilatush.util.cli;

import com.dilatush.util.cli.ParameterParser.Result;

/**
 * Abstract base class for {@link ParameterParser} implementations.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AParameterParser {


    /**
     * Returns a {@link Result} instance that indicates an invalid result with the given explanatory message.
     *
     * @param _message The message explaining why there was a parsing problem.
     * @return a {@link Result} instance that indicates an invalid result with the given explanatory message
     */
    protected Result error( final String _message ) {
        return new Result( false, null, _message );
    }


    /**
     * Returns a {@link Result} instance that indicates an invalid result with the given explanatory message.
     *
     * @param _value The message explaining why there was a parsing problem.
     * @return a {@link Result} instance that indicates an invalid result with the given explanatory message
     */
    protected Result result( final Object _value ) {
        return new Result( true, _value, null );
    }
}
