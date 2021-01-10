package com.dilatush.util.cli.validators;

import java.io.File;

/**
 * Provides a parameter validator that tests whether the given {@link File} object represents a file that <i>could</i> be created or written to.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class CreatableFileValidator extends AValidatorParser implements ParameterValidator {


    /**
     * Returns a {@link Result} instance with {@link Result#valid} set to {@code true} if the given parameter value (after parsing and translation)
     * is file that could be created or written to.  Otherwise a {@link Result} instance with {@link Result#valid} set to {@code false} and
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

        // if the file exists, then we return whether we can write to it...
        if (file.exists()) {
            return file.canWrite() ? valid() : error( "Cannot write to file: " + file.getAbsolutePath() );
        }

        // otherwise, we attempt to create it and delete it, and if we got an exception, we failed...
        else {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                return valid();
            }
            catch (Exception _e) {
                return error( "Cannot create file: " + file.getAbsolutePath() + " - " + _e.getMessage() );
            }
        }
    }
}
