package com.dilatush.util.test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a {@link TestEnabler} that is implemented in JavaScript, with the script determined and compiled at instantiation time.  The provided
 * script <i>must</i> provide three functions:
 * <ul>
 *     <li><code>enabled()</code>, which returns <code>true</code> if this instance is currently enabled.</li>
 *     <li><code>init()</code>, which initializes the instance and has no return value.</li>
 *     <li><code>set( properties )</code>, the argument of which is the properties of this instance, and there is no return value.  This function
 *     is invoked <i>only</i> at instantiation time.</li>
 * </ul>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class JavaScriptTestEnabler extends ATestEnabler {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private Invocable invocable;


    /**
     * Creates a new instance of this class with the given properties and script.  None of the given properties are used by this class, but all are
     * available to the software being tested.  The provided script <i>must</i> provide three functions:
     * <ul>
     *     <li><code>enabled()</code>, which returns <code>true</code> if this instance is currently enabled.</li>
     *     <li><code>init()</code>, which initializes the instance and has no return value.</li>
     *     <li><code>set( properties )</code>, the argument of which is the properties of this instance, and there is no return value.  This function
     *     is invoked <i>only</i> at instantiation time.</li>
     * </ul>
     *
     * @param _properties the properties for this instance.
     * @param _script the JavaScript program for this instance.
     */
    public JavaScriptTestEnabler( final Map<String, Object> _properties, String _script ) {
        super( _properties );

        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName( "nashorn" );
            engine.eval( _script );
            invocable = (Invocable) engine;

            invocable.invokeFunction( "set", properties );
        }
        catch( ScriptException | NoSuchMethodException _e ) {
            LOGGER.log( Level.WARNING, "Problem with JavaScript", _e );
        }
    }


    /**
     * Initializes this test enabler.  This happens at two different times:
     * <ul>
     *     <li>Upon instantiation.</li>
     *     <li>When this instance is one component of a {@link CompositeTestEnabler} instance, and the preceding component's enabled state changes
     *     from enabled to disabled.</li>
     * </ul>
     */
    @Override
    public void init() {
        try {
            invocable.invokeFunction( "init" );
        }
        catch( ScriptException | NoSuchMethodException _e ) {
            LOGGER.log( Level.WARNING, "Problem with init() function in JavaScript", _e );
        }
    }


    /**
     * Returns <code>true</code> if this instance is currently enabled.
     *
     * @return <code>true</code> if this instance is currently enabled
     */
    @Override
    protected boolean enabled() {
        try {
            return (boolean) invocable.invokeFunction( "enabled" );
        }
        catch( ScriptException | NoSuchMethodException _e ) {
            LOGGER.log( Level.WARNING, "Problem with enabled() function in JavaScript", _e );
            return false;
        }
        catch( ClassCastException _e ) {
            LOGGER.log( Level.WARNING, "Return value of enabled() function in JavaScript is not a boolean", _e );
            return false;
        }
    }
}
