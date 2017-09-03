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
}
