package com.dilatush.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.Strings.isNonEmpty;

/**
 * Implements a generalized persistent JSON object, stored as a string in a file on the file system.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class PersistentJSONObject extends JSONObject {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final File file;


    private PersistentJSONObject( final String _json, final File _file ) {
        super( _json );
        file = _file;
    }

    /**
     * Creates a new instance of this class associated with the given file, and loads it from the given file, which must contain a valid JSON string
     * in UTF-8 encoding.
     *
     * @param _file
     *      the JSON file to load the new instance from.
     * @return
     *      the newly created instance of this class, or null if there was a problem.
     */
    public static PersistentJSONObject load( final File _file ) {

        String json = Files.readToString( _file );
        return (isNonEmpty( json ) && isValid( json )) ? new PersistentJSONObject( json, _file ) : null;
    }


    /**
     * Creates a new instance of this class from the given JSON string, and associates it with the given file.  Note that the file is neither read
     * nor written.  See the {@link #save()} method for writing to the file.
     *
     * @param _json
     *      the JSON string to load the new instance from.
     * @param _file
     *      the file to associate with this instance.
     * @return
     *      the newly created instance of this class, or null if there was a problem.
     */
    public static PersistentJSONObject create( final String _json, final File _file ) {
        return (isNonEmpty( _json ) && isValid( _json )) ? new PersistentJSONObject( _json, _file ) : null;
    }

    /**
     * Creates a new instance of this class associated with the given file, and loads it from the given file, which must contain a valid JSON string
     * in UTF-8 encoding.
     *
     * @param _filePath
     *      the path for the JSON file to load the new instance from.
     * @return
     *      the newly created instance of this class, or null if there was a problem.
     */
    public static PersistentJSONObject load( final String _filePath ) {
        return load( new File( _filePath ) );
    }


    /**
     * Creates a new instance of this class from the given JSON string, and associates it with the given file.  Note that the file is neither read
     * nor written.  See the {@link #save()} method for writing to the file.
     *
     * @param _json
     *      the JSON string to load the new instance from.
     * @param _filePath
     *      the path to the file to associate with this instance.
     * @return
     *      the newly created instance of this class, or null if there was a problem.
     */
    public static PersistentJSONObject create( final String _json, final String _filePath ) {
        return create( _json, new File( _filePath ) );
    }


    /**
     * Saves the current state of this instance to the associated file, creating it if it does not already exist.
     */
    public void save() {

        Files.writeToFile( file, toString() );
    }


    private static boolean isValid( final String _json ) {
        try {
            JSONObject jo = new JSONObject( _json );
            return true;
        }
        catch( JSONException _e ) {
            LOGGER.log( Level.SEVERE, "Problem in JSON string: " + _json, _e );
            return false;
        }

    }
}
