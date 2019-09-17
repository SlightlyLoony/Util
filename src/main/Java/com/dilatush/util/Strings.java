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
     * Returns the specified string after stripping off any trailing newline character.  Note that if the specified string is {@code null} or empty
     * (zero length), then the specified string is returned unchanged.
     *
     * @param _str the string to strip a trailing newline character from
     * @return the specified string less any trailing newline
     */
    public static String stripTrailingNewline( final String _str ) {
        if( isEmpty( _str ) || ('\n' != _str.charAt( _str.length() - 1 )) )
            return _str;
        return _str.substring( 0, _str.length() - 1 );
    }
}
