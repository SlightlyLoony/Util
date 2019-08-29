package com.dilatush.util;

import org.json.JSONArray;

/**
 * Static container class for utility methods related to JSON.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class JSON {


    /**
     * Returns a Java string array from the given JSON array of strings.
     *
     * @param _array a JSON array of strings
     * @return the Java string array equivalent of the given array
     * @throws org.json.JSONException if the given array contains anything other than strings.
     */
    static public String[] getStringArray( final JSONArray _array ) {

        Checks.required( _array );

        String[] result = new String[ _array.length() ];

        for( int i = 0; i < _array.length(); i++ ) {
            result[i] = _array.getString( i );
        }

        return result;
    }
}
