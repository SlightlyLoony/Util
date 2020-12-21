package com.dilatush.util;

import static com.dilatush.util.General.isNull;

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
    @SuppressWarnings( "unused" )
    public static String stripTrailingNewlines( final String _str ) {
        if( isEmpty( _str ) ) return _str;
        int i = _str.length() - 1;
        while( (_str.charAt( i ) == '\n') || (_str.charAt( i ) == '\r') ) {
            i--;
        }
        return _str.substring( 0, i + 1 );
    }


    /**
     * Returns <code>true</code> if the given test string exactly matches one of the given ok strings, <code>false</code> otherwise.  If the given
     * test string is <code>null</code> or zero-length, or if the ok strings are not supplied, then it also returns <code>false</code>.
     *
     * @param _testStr The string to test.
     * @param _okStrs The strings representing ok values.
     * @return <code>true</code> if the test string matches one of the ok strings.
     */
    @SuppressWarnings( "unused" )
    public static boolean isOneOf( final String _testStr, final String... _okStrs ) {
        if( isEmpty( _testStr ) || isNull( (Object) _okStrs ) || (_okStrs.length == 0) )
            return false;
        for( String ok : _okStrs ) {
            if( _testStr.equals( ok ) )
                return true;
        }
        return false;
    }
}
