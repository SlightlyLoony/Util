package com.dilatush.util;

import static com.dilatush.util.Strings.isEmpty;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class General {


    /**
     * Returns true if <i>any</i> of the given objects are null.
     *
     * @param _obj the objects to test for nullness
     * @return {@code true} if any of the specified objects are null
     */
    public static boolean isNull( final Object... _obj ) {

        if( (_obj == null) || (_obj.length == 0) )
            return true;

        for( Object object : _obj ) {
            if( object == null )
                return true;
        }
        return false;
    }


    /**
     * Returns true if <i>none</i> of the given objects are null.
     *
     * @param _obj the objects to test for nullness
     * @return {@code true} if none of the specified objects are null
     */
    public static boolean isNotNull( final Object... _obj ) {
        return !isNull( _obj );
    }


    /**
     * Attempts to parse the given string into an integer.  Returns null if there are any problems, the parsed integer otherwise.  It will
     * not throw any exceptions.
     *
     * @param _str the string to parse into an integer
     * @return the integer, or {@code null} if there was any problem parsing
     */
    public static Integer parseInt( final String _str ) {
        if( isEmpty( _str) )
            return null;
        try {
            return Integer.parseInt( _str );
        }
        catch( Exception _e ) {
            return null;
        }
    }
}


