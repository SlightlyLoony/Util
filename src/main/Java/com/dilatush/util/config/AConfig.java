package com.dilatush.util.config;

import com.dilatush.util.Outcome;
import com.dilatush.util.Strings;
import com.dilatush.util.compiler.MemoryClassLoader;
import com.dilatush.util.compiler.MemoryFileObject;
import com.dilatush.util.compiler.RuntimeCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for configuration classes that can be initialized by a Java 'script' (ordinary Java code that is compiled at runtime).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AConfig {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Outcome.Forge<?> OUTCOME = new Outcome.Forge<>();


    /**
     * <p>Initialize the superclass by compiling and executing the Java configuration script with the given binary name, with its source code contained
     * in the file with the given path.  Following the execution of the Java script, the superclass is validated.  The resulting outcome is ok if the
     * script compiled successfully, executed without error, and the resulting values in the superclass were validated.  Otherwise, the outcome is
     * not ok, and {@link Outcome#msg()} will return a string with an explanatory message.</p>
     * <p>Note that the configuration script's class may be in any package, including the default package.  The class must implement the
     * {@link Configurator} interface, but has no other restrictions - it may have other functions, inner classes, and so on.</p>
     *
     * @param _binaryName The binary name of the Java configuration script's class
     * @param _configFilePath The file system path to the file containing the Java configuration script.
     * @return an {@link Outcome} with the results of the initialization
     */
    public Outcome<?> init( final  String _binaryName, final String _configFilePath ) {
        return init( _binaryName, _configFilePath, null );
    }


    /**
     * <p>Initialize the superclass by compiling and executing the Java configuration script with the given binary name, with its source code contained
     * in the file with the given path.  Following the execution of the Java script, the superclass is validated.  The resulting outcome is ok if the
     * script compiled successfully, executed without error, and the resulting values in the superclass were validated.  Otherwise, the outcome is
     * not ok, and {@link Outcome#msg()} will return a string with an explanatory message.</p>
     * <p>Note that the configuration script's class may be in any package, including the default package.  The class must implement the
     * {@link Configurator} interface, but has no other restrictions - it may have other functions, inner classes, and so on.</p>
     * <p>The optional substitutions document (see {@link Strings#substitute(String,String)}) provides a convenient mechanism for storing secrets
     * (passwords, user names, keys, etc.) outside of the configuration file.  This mechanism allows different security for those secrets, and also
     * allows them to be stored by any means that can be resolved to a string.  This could be as simple as a file with different permissions, or
     * as complex as a managed key store.</p>
     *
     * @param _binaryName The binary name of the Java configuration script's class
     * @param _configFilePath The file system path to the file containing the Java configuration script.
     * @param _substitutionsDoc The optional substitutions document ({@code null} if not used).
     * @return an {@link Outcome} with the results of the initialization
     */
    public Outcome<?> init( final String _binaryName, final String _configFilePath, final String _substitutionsDoc  ) {

        // get the source file...
        Outcome<MemoryFileObject> sourceResult = MemoryFileObject.fromFile(
                _binaryName,
                _configFilePath,
                _substitutionsDoc
        );
        if( !sourceResult.ok() )
            return OUTCOME.notOk( sourceResult.msg() );

        // compile it...
        RuntimeCompiler compiler = new RuntimeCompiler();
        compiler.addSource( sourceResult.info() );
        Outcome<MemoryClassLoader> compilerResult = compiler.compile();
        if( !compilerResult.ok() )
            return OUTCOME.notOk( compilerResult.msg() );
        MemoryClassLoader loader = compilerResult.info();

        // get the configurator...
        Configurator configurator;
        try {
            Class<?> klass = loader.findClass( _binaryName );
            if( !Configurator.class.isAssignableFrom( klass ) )
                throw new IllegalArgumentException( "Configuration script class " + _binaryName + " does not implement Configurator" );
            configurator = (Configurator) klass.getConstructor().newInstance();   // get instance via no-args constructor...
        }
        catch( Exception _e ) {
            return OUTCOME.notOk( "Could not instantiate configurator", _e );
        }

        // run the configurator and validate the result...
        return init( configurator );
    }


    /**
     * <p>Initialize the superclass with the given {@link Configurator}.  Following the configuration, the superclass is validated.  The resulting
     * outcome is ok if the configurator executed without error, and the resulting values in the superclass were validated.  Otherwise, the outcome is
     * not ok, and {@link Outcome#msg()} will return a string with an explanatory message.</p>
     *
     * @param _configurator The {@link Configurator} instance to use.
     * @return an {@link Outcome} with the results of the initialization
     */
    public Outcome<?> init( final Configurator _configurator ) {

        // run the configurator...
        try {
            _configurator.config( this );
        }
        catch( Exception _e ) {
            return OUTCOME.notOk( "Problem running configurator", _e );
        }

        // validate the result...
        Outcome<?> validateResult = isValid();

        // and we're done...
        return validateResult.ok()
                ? OUTCOME.ok()
                : OUTCOME.notOk( "Configuration is not valid\n" + validateResult.msg() );
    }


    /**
     * Returns an ok {@link Outcome} if this configuration object is valid
     * (as indicated by the result of {@link #verify(List)}).  Otherwise, there were one or more
     * problems found during validation, and {@link Outcome#msg()} is set to a message explaining what those problems were.
     *
     * @return an instance of {@link Outcome}
     */
    public final synchronized Outcome<?> isValid() {

        // initialize our messages list; if the length of this becomes non-zero during validation, then we have an invalid result...
        ArrayList<String> messages = new ArrayList<>();

        // run our verification...
        verify( messages );

        // if we had no problem messages posted, we have a valid result...
        return (messages.size() == 0)
                ? OUTCOME.ok()
                : OUTCOME.notOk( String.join( "\n", messages.toArray( new String[0] ) ) );
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


    /**
     * Verify the given sub-configuration.
     *
     * @param _subConfig The sub-configuration to verify.
     * @param _messages The list of messages for verification problems.
     * @param _subConfigName The human-readable name of the sub-configuration.
     */
    protected void verifySubConfig( final AConfig _subConfig, final List<String> _messages, final String _subConfigName ) {

        // first make sure we actually HAVE a sub-configuration...
        validate( () -> _subConfig != null, _messages, _subConfigName + " must be present" );

        // if it IS present, verify it...
        if( _subConfig != null )
            _subConfig.verify( _messages );
    }
}
