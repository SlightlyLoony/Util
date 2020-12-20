package com.dilatush.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Instances of this class read configuration information from a JavaScript file into a simple Java object that can be directly used by the
 * program needing the configuration information.  Note that the configuration information is simply <i>read</i>, and not validated.  Such validation
 * is the duty of the caller.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class JSConfig<T> {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );


    /**
     * Get configuration information from the given JavaScript file into a class of the given Java type.  The JavaScript code sees a freshly
     * instantiated class (using a no-arguments constructor) of the given Java type, in a variable named <code>config</code>.  The JavaScript code
     * must initialize the fields in the <code>config</code>.  Note that a field in the given Java type may itself be declared as any Java type.  If
     * that child type is a POJO (plain old Java object), then <i>it's</i> fields must similarly be initialized.  For example, if the field
     * <code>config.address</code> was a POJO, then the JavaScript code might initialize the city and state like this:
     * <code>config.address.city = 'Provo'; config.address.state = 'Utah';</code>.
     *
     * @param _filename the name (including any non-default path) of the file containing the JavaScript configuration information.
     * @param _class the Java class object for the desired result type, which <i>must</i> match the generic type T.
     * @return the configuration object after initialization by the JavaScript file, or <code>null</code> if there was a problem parsing or executing
     * the JavaScript configuration information.  If there was a problem, then the error will be logged.
     */
    public T get( final String _filename, final Class<T> _class ) {

        try {

            // get the Nashorn JavaScript engine...
            ScriptEngine engine = new ScriptEngineManager().getEngineByName( "nashorn" );

            /*
             * Construct the JavaScript string to evaluate.  For example, with a file name of 'config.js' and a class of 'com.config.Config', this
             * string would be:
             *
             *    var Config = Java.type( 'com.config.Config' );
             *    var config = new Config();
             *    load( 'config.js' );
             *    config;
             */
            String sb = "var " +
                    _class.getSimpleName() +
                    " = Java.type( '" +
                    _class.getCanonicalName() +
                    "' );\n" +
                    "var config = new " +
                    _class.getSimpleName() +
                    "();\n" +
                    "load( '" +
                    _filename +
                    "' );\n" +
                    "config;\n";
            return _class.cast( engine.eval( sb ) );
        }
        catch( ScriptException _e ) {
            LOGGER.log( Level.SEVERE, "Problem with JavaScript configuration", _e );
            return null;
        }
    }
}
