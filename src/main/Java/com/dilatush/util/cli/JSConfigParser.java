package com.dilatush.util.cli;

import com.dilatush.util.AConfig;
import com.dilatush.util.Files;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.io.File;

import static com.dilatush.util.Strings.isEmpty;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class JSConfigParser implements ParameterParser {

    private final AConfig config;

    private String errorMsg;
    private boolean hasRun;


    public JSConfigParser( final AConfig _config ) {
        config = _config;
    }


    /**
     * Translates the JavaScript configuration file with the given file name into an initialized configuration object.
     *
     * @param _parameter The JavaScript configuration file name.
     * @return the initialized configuration object, or <code>null</code> if the initialization failed.
     */
    @Override
    public Object parse( final String _parameter ) {

        // if we got no parameter...
        if( isEmpty( _parameter ) ) {
            errorMsg = "Expected JavaScript configuration file name, got nothing.";
            return null;
        }

        // if we've already run with this configuration object, we've got a problem...
        if( hasRun ) {
            errorMsg = "Attempted to reuse parser";
            return null;
        }
        hasRun = true;

        // now we read the JavaScript file...
        File file = new File( _parameter );

        if( !file.isFile() ) {
            errorMsg = "JavaScript file path does not resolve to a file: " + file.getAbsolutePath();
            return null;
        }

        if( !file.canRead() ) {
            errorMsg = "Can not read the specified JavaScript file: " + file.getAbsolutePath();
            return null;
        }

        String jsf = Files.readToString( file );

        if( jsf == null ) {
            errorMsg = "Problem while reading the specified JavaScript file: " + file.getAbsolutePath();
            return null;
        }

        /*
         * Construct the JavaScript program we're going to run, as follows:
         *
         *      function init( config ) {
         *          load( "<config file name>" );
         *          return config;
         *      }
         */
        String js = "function init( config ) {         \n" +
                    jsf +
                    "    return config;                \n" +
                    "}                                 \n";

        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName( "nashorn" );
            engine.eval( js );
            Invocable invocable = (Invocable) engine;

            invocable.invokeFunction( "init", config );

            return config;
        }
        catch( ScriptException | NoSuchMethodException _e ) {
            errorMsg = _e.getMessage();
            return null;
        }
    }


    /**
     * Return a descriptive error message if the parsing and translation failed for any reason (i.e., {@link #parse(String)} returned
     * <code>null</code>.
     *
     * @return a descriptive error message after parsing and translation failed
     */
    @Override
    public String getErrorMessage() {
        return errorMsg;
    }
}
