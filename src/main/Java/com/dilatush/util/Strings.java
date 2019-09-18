package com.dilatush.util;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Strings {


    /**
     * Returns true if the given {@link String} is non-null and is not empty.
     *
     * @param _str
     *      The {@link String} instance to check.
     * @return
     *      true if the given {@link String} is non-null and is not empty.
     */
    public static boolean isNonEmpty( final String _str ) {
        return (_str != null) && (_str.length() > 0);
    }


    /**
     * Returns true if the given {@link String} is null or empty.
     *
     * @param _str
     *      The {@link String} instance to check.
     * @return
     *      true if the given {@link String} is null or empty.
     */
    public static boolean isEmpty( final String _str ) {
        return (_str == null) || (_str.length() == 0);
    }


    /**
     * Returns the specified string after stripping off any trailing newline or return characters.  Note that if the specified string is {@code null}
     * or empty (zero length), then the specified string is returned unchanged.
     *
     * @param _str the string to strip trailing newline or return characters from
     * @return the specified string less any trailing newline or return characters
     */
    public static String stripTrailingNewlines( final String _str ) {
        if( isEmpty( _str ) ) return _str;
        int i = _str.length() - 1;
        while( (_str.charAt( i ) == '\n') || (_str.charAt( i ) == '\r') ) {
            i--;
        }
        return _str.substring( 0, i + 1 );
    }
}
