package com.dilatush.util.cli.validators;

import java.io.File;

/**
 * Provides a parameter validator that tests whether the given {@link File} object represents a writable file.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class WritableFileValidator extends AValidatorParser implements ParameterValidator {


    /**
     * Returns a {@link Result} instance with {@link Result#valid} set to {@code true} if the given parameter value (after parsing and translation)
     * is a file that can be written to.  Otherwise a {@link Result} instance with {@link Result#valid} set to {@code false} and
     * {@link Result#message} set to an explanatory message is returned.
     *
     * @param _parameterValue The value to validate.
     * @return a {@link Result} instance containing the results of the validation operation
     */
    @Override
    public Result validate( final Object _parameterValue ) {

        if( _parameterValue == null )
            return error( "Expected to validate a File object, instead got a null." );

        if( !(_parameterValue instanceof File) )
            return error( "Expected to validate a File object, instead got " + _parameterValue.getClass().getCanonicalName() + " object." );

        // get our parameter...
        File file = (File) _parameterValue;

        if( !file.isFile() )
            return error( "File path does not resolve to a file: " + file.getAbsolutePath() );

        if( !file.canWrite() )
            return error( "Can not write the specified file: " + file.getAbsolutePath() );

        return valid();
    }
}
