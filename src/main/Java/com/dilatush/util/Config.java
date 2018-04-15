package com.dilatush.util;

import org.json.JSONException;

import java.io.File;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a generic configuration using JSON encoding for the persistent file.  This class extends {@link HJSONObject} to provide getters,
 * setters, and testers that use dotted identifiers to represent elements within a hierarchy.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Config extends HJSONObject {


    /**
     * Creates a new instance of this class initialized from the JSON file at the specified path.
     *
     * @param _configFilePath the path to the JSON file.
     * @return a new instance of this class.
     */
    public static Config fromJSONFile( final String _configFilePath ) {

        if( isEmpty( _configFilePath ) ) throw new IllegalArgumentException( "Configuration file path missing or empty" );
        String json = Files.readToString( new File( _configFilePath ) );
        if( isEmpty( json ) ) throw new IllegalArgumentException( "Configuration file missing or empty: " + _configFilePath );
        return new Config( json );
    }


    public void write( final String configFilePath ) {
        Files.writeToFile( new File( configFilePath ), toString() );
    }


    public Config( final String source ) throws JSONException {
        super( source );
    }
}
