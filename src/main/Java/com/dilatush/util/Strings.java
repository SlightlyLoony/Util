package com.dilatush.util;

import java.util.Arrays;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for utility methods related to {@link String}s.
 *
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


    /**
     * Returns a string of the given length, consisting of the given character in every position.
     *
     * @param _char The character to repeat in the return string.
     * @param _length The length of the return string.
     * @return the string of the given length, consisting of the given character in every position
     */
    public static String getStringOfChar( final char _char, final int _length ) {

        if( _length < 0 )
            throw new IllegalArgumentException( "Length may not be negative" );

        char[] chars = new char[_length];
        Arrays.fill( chars, _char );
        return new String( chars );
    }


    /**
     * Returns the given string left-justified in a field of the given width.  If the given string is the same length as the field, it is returned
     * without change.  If the given string is shorter than the field, then spaces are appended to it to make its length the same as the field's.  If
     * the given string is longer than the field, it is truncated to a length one less than the width and an ellipsis ('…') is appended.
     *
     * @param _str The string to left-justify in a fixed length field.
     * @param _width The width of the fixed-length field, in characters.
     * @return the left-justified string
     */
    public static String leftJustify( final String _str, final int _width ) {

        // if the width is the same, just return...
        if( _str.length() == _width )
            return _str;

        // if the string is too long, truncate and append an ellipsis...
        if( _str.length() > _width )
            return _str.substring( 0, _width - 1 ) + "…";

        // otherwise, pad with spaces to get the right width...
        return _str + getStringOfChar( ' ', _width - _str.length() );
    }


    /**
     * Returns the given string right-justified in a field of the given width.  If the given string is the same length as the field, it is returned
     * without change.  If the given string is shorter than the field, then spaces are prepended to it to make its length the same as the field's.  If
     * the given string is longer than the field, it is truncated to a length one less than the width and an ellipsis ('…') is prepended.
     *
     * @param _str The string to right-justify in a fixed length field.
     * @param _width The width of the fixed-length field, in characters.
     * @return the right-justified string
     */
    public static String rightJustify( final String _str, final int _width ) {

        // if the width is the same, just return...
        if( _str.length() == _width )
            return _str;

        // if the string is too long, truncate and prepend an ellipsis...
        if( _str.length() > _width )
            return "…" + _str.substring( 1 + _str.length() - _width );

        // otherwise, pad with spaces to get the right width...
        return getStringOfChar( ' ', _width - _str.length() ) + _str;
    }


    /**
     * Returns a non-null string even if the given string is {@code null}.  If the given string is non-null already, it is returned without change.
     * If the given string is {@code null}, an empty string ("") is returned.
     *
     * @param _arg The string to make safe.
     * @return the given string, or an empty string if the given string was {@code null}
     */
    public static String safe( final String _arg ) {
        return (_arg == null) ? "" : _arg;
    }

}
