package com.dilatush.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File-related utilities in a static method container class.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Files {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );


    /**
     * Returns the contents of the given text file as a string.  Returns null if there was any problem reading the file.
     *
     * @param _file the file to read
     * @return the string containing the file
     */
    public static String readToString( final File _file ) {
        try {
            return String.join( "\n",
                    java.nio.file.Files.readAllLines( Paths.get( _file.getPath() ), StandardCharsets.UTF_8).toArray( new String[0] ) );
        }
        catch( IOException _e ) {
            return null;
        }
    }


    /**
     * Writes the text in the given string to the given file.  Returns true on success, false if there was any problem.
     *
     * @param _file the file to write the text to
     * @param _data the text to write
     * @return true on success, false if there was any problem
     */
    public static boolean writeToFile( final File _file, final String _data ) {
        try {
            java.nio.file.Files.writeString( Paths.get( _file.getPath() ), _data );
            return true;
        }
        catch( IOException _e ) {
            LOGGER.log( Level.SEVERE, "Problem writing to text file", _e );
            return false;
        }
    }
}
