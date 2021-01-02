package com.dilatush.util.cli;

import java.io.File;

/**
 * Provides a parameter validator that tests whether the given {@link File} object represents a readable file.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ReadableFileValidator implements ParameterValidator {

    private String errorMsg;


    /**
     * Returns <code>true</code> if the given {@link File} object represents a readable file.
     *
     * @param _parameterValue The {@link File} object to validate.
     * @return <code>true</code> if the given {@link File} object represents a readable file
     */
    @Override
    public boolean validate( final Object _parameterValue ) {

        if( _parameterValue == null ) {
            errorMsg = "Expected to validate a File object, instead got a null.";
            return false;
        }
        if( !(_parameterValue instanceof File) ) {
            errorMsg = "Expected to validate a File object, instead got " + _parameterValue.getClass().getCanonicalName() + " object.";
            return false;
        }
        File file = (File) _parameterValue;
        if( !file.isFile() ) {
            errorMsg = "File path does not resolve to a file: " + file.getAbsolutePath();
            return false;
        }
        if( !file.canRead() ) {
            errorMsg = "Can not read the specified file: " + file.getAbsolutePath();
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
