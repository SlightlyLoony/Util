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
 * Abstract base class for scriptable and verifiable configuration POJOs.  Subclasses should store configuration information in public, mutable
 * fields, and implement the {@link #verify} method.  Subclasses intended to be initialized via JavaScript <i>must</i> have no-args constructors
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
     * <p>The JavaScript script should assume that the variable "config" is the freshly created configuration object, and initialize its fields.</p>
     *
     * @param _class The class of the configuration object to be created.  This class <i>must</i> be a subclass of {@link AConfig}, and it <i>must</i>
     *               have a no-args constructor.
     * @param _initializationScriptPath The path of the JavaScript file that will initialize the newly created configuration object.  This script
     *                                  <i>must</i> define a function {@code init( config )}, where {@code config} is the instantiated, but
     *                                  uninitialized {@link AConfig} subclass, and the function does the initialization.
     * @return the {@link InitResult} instance with the results of this operation
     */
    public static InitResult init( final Class< ? extends AConfig> _class, final String _initializationScriptPath ) {

        try {

            // if we didn't get both parameters, raise a ruckus...
            if( isNull( _class, _initializationScriptPath ) )
                return error(  "Arguments to AConfig.init() may not be null", null );

            // read our JavaScript initialization script...
            String script = Files.readToString( new File( _initializationScriptPath ) );
            if( isNull( script ) )
                return error( "Could not read JavaScript configuration initialization file: " + _initializationScriptPath, null  );

            // create an instance of the specified class...
            AConfig config = _class.getConstructor().newInstance();

            // get a Nashorn engine and invoke our function...
            ScriptEngine engine = new ScriptEngineManager().getEngineByName( "nashorn" );
            engine.eval( script );
            ((Invocable) engine).invokeFunction( "init", config );

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


    /**
     * Log an appropriate error and return an {@link InitResult} instance indicating invalid, with an explanatory message.
     *
     * @param _message The explanatory message to log, and return in an {@link InitResult} instance.
     * @param _e The exception associated with the problem, if there is one (may be {@code null}).
     * @return an {@link InitResult} indicating invalid, with an explanatory message
     */
    private static InitResult error( final String _message, final Exception _e ) {
        if( isNull( _e ) )
            LOGGER.log( Level.SEVERE, _message );
        else
            LOGGER.log( Level.SEVERE, _message + _e.getMessage(), _e );
        return new InitResult( false, null, _message );
    }


    /**
     * Simple POJO containing the results of an {@link #init(Class, String)} invocation.
     */
    public static class InitResult {

        /**
         * Set to {@code true} if the configuration was successfully instantiated, initialized, and validated; {@code false} otherwise.
         */
        public final boolean valid;

        /**
         * If {@link #valid} is {@code true}, set to the instantiated, initialized, and validated configuration; {@code null} otherwise.
         */
        public final AConfig config;

        /**
         * If {@link #valid} is {@code false}, set to a message explaining why the operation failed; {@code null} otherwise.
         */
        public final String  message;


        /**
         * Creates a new instance of this class with the given values.
         *
         * @param _valid  {@code True} if the configuration was successfully instantiated, initialized, and validated; {@code false} otherwise.
         * @param _config The instantiated, initialized, and validated configuration if {@code _valid} is {@code true}.
         * @param _message The message explaining any problem if {@code _valid} is false.
         */
        public InitResult( final boolean _valid, final AConfig _config, final String _message ) {
            valid = _valid;
            config = _config;
            message = _message;
        }
    }


    /**
     * Returns an instance of {@link ValidationResult} with {@link ValidationResult#valid} set to {@code true} if this configuration object is valid
     * (as indicated by the result of {@link #verify(List)}).  If {@link ValidationResult#valid} is {@code false}, then there were one or more
     * problems found during validation, and {@link ValidationResult#message} is set to a message explaining what those problems were.
     *
     * @return an instance of {@link ValidationResult}
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


    /**
     * Simple POJO containing the result of an {@link #isValid()} invocation.
     */
    public static class ValidationResult {

        /**
         * Set to {@code true} if the validation succeeded.
         */
        public final boolean valid;

        /**
         * Set to a message explaining what problems were encountered during validation, if {@link #valid} is {@code false}.
         */
        public final String  message;


        /**
         * Create a new instance of this class with the given values.
         *
         * @param _valid {@code True} if the validation succeeded.
         * @param _message A message explaining what problems were encountered during validation, if {@code _valid} is {@code false}.
         */
        public ValidationResult( final boolean _valid, final String _message ) {
            valid   = _valid;
            message = _message;
        }
    }


    /**
     * Implemented by subclasses to verify that their fields are valid.  When possible, this should be accomplished by a series of invocations
     * of {@link #validate(Validator, List, String)}, one or more times for each field in the configuration.  If a field in the configuration is
     * itself an instance of an {@link AConfig} subclass, then its {@code verify(List)} method should be called.
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

        /**
         * Return {@code true} if the validation test succeeded; {@code false} if the test failed.
         *
         * @return {@code True} if the validation test succeeded
         */
        boolean verify();
    }
}
