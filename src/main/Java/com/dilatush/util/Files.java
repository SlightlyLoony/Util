package com.dilatush.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Files {

    public static String readToString( final File _file ) {
        try {
            return String.join( "\n",
                    java.nio.file.Files.readAllLines( Paths.get( _file.getPath() ), StandardCharsets.UTF_8).toArray( new String[0] ) );
        }
        catch( IOException _e ) {
            return null;
        }
    }


    public static boolean writeToFile( final File _file, final String _data ) {
        try {
            java.nio.file.Files.write( Paths.get( _file.getPath() ), _data.getBytes( StandardCharsets.UTF_8 ) );
            return true;
        }
        catch( IOException _e ) {
            Logger.log( _e );
            return false;
        }
    }
}
