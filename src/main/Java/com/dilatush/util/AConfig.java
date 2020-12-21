package com.dilatush.util;

import java.util.logging.Logger;

/**
 * Abstract base class for validatable configuration POJOs.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AConfig implements Validatable {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // set true if configuration state is valid...
    protected boolean valid;


    /**
     * Returns <code>true</code> if the state of this object is valid, and <code>false</code> otherwise, after logging a description of the invalid
     * state.
     *
     * @return <code>true</code> if the state of this object is valid.
     */
    @Override
    public boolean isValid() {

        // if we've already validated, just leave...
        if( valid )
            return true;

        // otherwise, trust, but verify...
        valid = true;
        verify();
        return valid;
    }


    /**
     * Implemented by subclasses to verify that their fields are valid.  When possible, this should be accomplished by a series of invocations
     * of {@link #validate(Validator, String)}, one or more times for each field in the configuration.
     */
    protected abstract void verify();


    /**
     * If the parameter <code>_valid</code> is <code>true</code>, this method just returns.  Otherwise, it sets {@link #valid} to <code>false</code>
     * and logs a SEVERE message with the given message.
     *
     * @param _valid The validator function.
     * @param _msg The message to log if the validator function returns <code>false</code>.
     */
    protected void validate( final Validator _valid, final String _msg ) {
        if( _valid.verify() ) return;
        valid = false;
        LOGGER.severe( _msg );
    }


    /**
     * A simple functional interface to report the results of a validation.
     */
    @FunctionalInterface
    protected interface Validator {
        boolean verify();
    }
}
