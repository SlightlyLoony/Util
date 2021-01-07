package com.dilatush.util;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;

/**
 * Abstract base class for scriptable and validatable configuration POJOs.  Subclasses should store configuration information in public, mutable
 * fields, and implement the {@link #validate} method.  Subclasses intended to be initialized via JavaScript <i>must</i> have no-args constructors
 * (even if just the default constructor).  Create and initialize instances of those subclasses with the {@link #init(Class,String)} method of this
 * class.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AConfig {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );


    /**
     * <p>Creates an instance of the given class (using an assumed no-args constructor) and then initializes and validates it.  The return value of
     * this method is an instance of {@link InitResult}. If the new {@link AConfig} instance was created, initialized, and validated without any
     * problems, it is returned in {@link InitResult#config} and {@link InitResult#valid} is {@code true}.  If there was any problem then
     * {@link InitResult#valid} is {@code false} and {@link InitResult#message} contains an explanatory message.</p>
     * <p>The JavaScript script should assume that the variable "config" is the freshly created configuration object, an initialize its fields.</p>
     *
     * @param _class The class of the configuration object to be created.
     * @param _initFunctionPath The path of the JavaScript file that will initialize the newly created configuration object.  This script <i>must</i>
     *                          define a function {@code init( config )}, where {@code config} is the instantiated, but uninitialized {@link AConfig}
     *                          subclass, and the function does the initialization.
     * @return the {@link InitResult} instance with the results of this operation
     */
    public static InitResult init( final Class< ? extends AConfig> _class, final String _initFunctionPath ) {

        try {
            if( isNull( _class, _initFunctionPath ) )
                return new InitResult( false, null, "Arguments to AConfig.init() may not be null" );

            // read our JavaScript initialization script...
            String script = Files.readToString( new File( _initFunctionPath ) );
            if( isNull( script ) )
                return error( "Could not read JavaScript configuration initialization file: " + _initFunctionPath, null  );

            // create an instance of the specified class...
            AConfig config = _class.getConstructor().newInstance();

            // get a Nashorn engine and invoke our function...
            ScriptEngine engine = new ScriptEngineManager().getEngineByName( "nashorn" );
            engine.eval( script );
            Invocable invocable = (Invocable) engine;
            invocable.invokeFunction( "init", config );

            // now validate the initialized configuration and return the appropriate result...
            ValidationResult vr = config.isValid();
            return vr.valid
                    ? new InitResult( true, config, null )
                    : error( "Configuration validation failed for the following reasons:\n" + vr.message, null );
        }
        catch( Exception _e ) {
            return error( "Problem initializing AConfig instance: ", _e );
        }
    }


    private static InitResult error( final String _message, final Exception _e ) {
        if( isNull( _e ) )
            LOGGER.log( Level.SEVERE, _message );
        else
            LOGGER.log( Level.SEVERE, _message + _e.getMessage(), _e );
        return new InitResult( false, null, _message );
    }


    public static class InitResult {

        public final boolean valid;
        public final AConfig config;
        public final String  message;


        public InitResult( final boolean _valid, final AConfig _config, final String _message ) {
            valid = _valid;
            config = _config;
            message = _message;
        }
    }


    /**
     * Returns <code>true</code> if the state of this object is valid, and <code>false</code> otherwise, after logging a detail of the invalid
     * state.
     *
     * @return <code>true</code> if the state of this object is valid.
     */
    public final synchronized ValidationResult isValid() {

        // initialize our messages list; if the length of this becomes non-zero during validation, then we have an invalid result...
        ArrayList<String> messages = new ArrayList<>();

        // run our verification...
        verify( messages );

        // if we had no problem messages posted, we have a valid result...
        if( messages.size() == 0 )
            return new ValidationResult( true, null );

        // otherwise, we had problems...
        return new ValidationResult( false, String.join( "\n", messages.toArray( new String[0] ) ) );
    }


    public static class ValidationResult {

        public final boolean valid;
        public final String  message;


        public ValidationResult( final boolean _valid, final String _message ) {
            valid   = _valid;
            message = _message;
        }
    }


    /**
     * Implemented by subclasses to verify that their fields are valid.  When possible, this should be accomplished by a series of invocations
     * of {@link #validate(Validator, List, String)}, one or more times for each field in the configuration.
     *
     * @param _messages The list of messages explaining configuration errors.
     */
    public abstract void verify( final List<String> _messages );


    /**
     * If the parameter {@code _valid} evaluates to {@code true}, this method just returns.  Otherwise it adds the given explanatory message to the
     * given list of explanatory messages, and logs a SEVERE log entry with the given message.
     *
     * @param _valid The validator function.
     * @param _messages The list of message explaining configuration value errors.
     * @param _msg The message to log if the validator function returns <code>false</code>.
     */
    protected void validate( final Validator _valid, final List<String> _messages, final String _msg ) {
        if( _valid.verify() )
            return;
        _messages.add( _msg );
        LOGGER.log( Level.SEVERE, _msg );
    }


    /**
     * A simple functional interface to report the results of a validation.
     */
    @FunctionalInterface
    protected interface Validator {
        boolean verify();
    }
}
