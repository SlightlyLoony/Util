package com.dilatush.util.cli;

import java.io.File;

/**
 * Provides a parameter validator that tests whether the given object is present and of the correct type.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings("rawtypes")
public class ObjectPresentValidator implements ParameterValidator {

    private final Class  type;

    private String errorMsg;


    public ObjectPresentValidator( final Class _type ) {

        if( _type == null )
            throw new IllegalArgumentException( "Cannot have a null type argument" );
        type = _type;
    }


    /**
     * Returns <code>true</code> if the given {@link File} object represents a readable file.
     *
     * @param _parameterValue The {@link File} object to validate.
     * @return <code>true</code> if the given {@link File} object represents a readable file
     */
    @Override
    public boolean validate( final Object _parameterValue ) {

        if( _parameterValue == null ) {
            errorMsg = "Expected to validate a " + type.getCanonicalName() + " object, instead got a null.";
            return false;
        }

        if( !type.isInstance( _parameterValue ) ) {
            errorMsg = "Expected to validate a " + type.getCanonicalName() + " object, instead got " + _parameterValue.getClass().getCanonicalName() + " object.";
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
