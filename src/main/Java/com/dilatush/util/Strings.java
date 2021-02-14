package com.dilatush.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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


    /**
     * Parses the given string into "words", where words are defined as either substrings surrounded by double quotes ("), or substrings separated
     * by whitespace or commas (,).  Leading quotes must be preceded by whitespace or a comma; trailing quotes must be followed by whitespace, a
     * comma, or the end of the string.  When the word is quoted, a quote may be included inside the quotes by escaping it with a backslash (\").  If
     * a leading quote is detected but there is no matching ending quote, then the entire rest of the given string becomes a word.  The parsed words
     * are returned in a list ordered left-to-right.
     *
     * @param _string The string to parse into words.
     * @return A list of the words parsed, in left-to-right order.
     */
    public static List<String> parseToWords( final String _string ) {

        // if our input string is empty, return an empty list...
        if( isEmpty( _string ) )
            return new ArrayList<>( 0 );

        // some setup...
        List<String> words = new ArrayList<>( 20 );
        StringBuilder word = new StringBuilder( _string.length() );
        boolean lastCharSeparator = true;
        boolean insideQuotes = false;

        // iterate over all the characters we were given...
        char[] chars = _string.toCharArray();
        for( int i = 0; i < chars.length; i++ ) {

            char c = chars[i];
            boolean thisCharSeparator = (SEPARATORS.indexOf( c ) >= 0);

            // if we're inside quotes, we've got some special handling...
            if( insideQuotes ) {

                // if we've got a backslash and we're not at the end of the string, then peek to see if the next character is a double quote...
                if( (c == '\\') && ((i+1) < chars.length) ) {
                    if( chars[i+1] == '"' ) {
                        word.append( '"' );
                        i++;
                    }
                }

                // otherwise, see if we've got a double quote followed by a separator or end of string...
                else if( (c == '"') && (((i+1) >= chars.length) || ((SEPARATORS.indexOf( chars[i+1])) >= 0) ) ) {
                    words.add( word.toString() );
                    word.setLength( 0 );
                    thisCharSeparator = true;
                    insideQuotes = false;
                }

                // otherwise, add the character to our quoted word...
                else
                    word.append( c );
            }

            // if we just terminated a word, handle it...
            else if( !lastCharSeparator && thisCharSeparator ) {
                words.add( word.toString() );
                word.setLength( 0 );
            }

            // if we've detected a leading quote, note that fact...
            else if( lastCharSeparator && (c == '"') )
                insideQuotes = true;

            // otherwise, if we don't have a separator, add the character to our word...
            else if( !thisCharSeparator )
                word.append( c );

            lastCharSeparator = thisCharSeparator;
        }

        // if we had a word before the end of string, save it...
        if( word.length() > 0 )
            words.add( word.toString() );

        return words;
    }

    private final static String SEPARATORS = " \t\n\r,";


    /**
     * Returns the index of the choice made using the given key.  This method first filters the given list of choices by those that start with the
     * given key (with the given case sensitivity).  If exactly one choice matched the key, or if multiple choices match the key and one matches it
     * exactly, its index is returned.  If zero choices matched the given key, then {@code NO_KEY_MATCH_FOR_CHOOSER} (-1) is returned.  If more than
     * one choice matched the filter, then {@code AMBIGUOUS_KEY_MATCH_FOR_CHOOSER} (-2) is returned.
     *
     * @param _choices The list of strings to choose from.
     * @param _key The key to use when selecting a string.
     * @param _caseSensitive  True if the key filter operation should be case-sensitive.
     * @return the index of the unique matching choice, or -1 for no match, or -2 for multiple matches
     */
    public static int chooseFrom( final List<String> _choices, final String _key, final boolean _caseSensitive ) {

        // fail fast if we have an argument problem...
        if( _choices == null )
            throw new IllegalArgumentException( "Missing list of choices" );
        if( isEmpty( _key ) )
            throw new IllegalArgumentException( "Missing key for choosing" );

        int hits = 0;     // number of key matches...
        int lastHit = 0;  // index of the last key match...

        // iterate over our choices, looking for matches...
        for( int i = 0; i < _choices.size(); i++ ) {

            String choice = _choices.get( i );

            // is this choice exactly the same as the key?
            boolean same = _caseSensitive
                    ? choice.equals( _key )
                    : choice.equalsIgnoreCase( _key );

            // if it's the same, we've got a winner; just leave with it...
            if( same )
                return i;

            // does this choice match the key?
            boolean matched = _caseSensitive
                    ? choice.startsWith( _key )
                    : choice.toUpperCase().startsWith( _key.toUpperCase() );

            // if we matched, record it...
            if( matched ) {
                hits++;
                lastHit = i;

                // if this was our second match, then we've got an ambiguous result...
                if( hits >= 2 )
                    return AMBIGUOUS_KEY_MATCH_FOR_CHOOSER;
            }
        }

        // return either our match or none...
        return (hits == 1) ? lastHit : NO_KEY_MATCH_FOR_CHOOSER;
    }

    public static int NO_KEY_MATCH_FOR_CHOOSER = -1;
    public static int AMBIGUOUS_KEY_MATCH_FOR_CHOOSER = -2;


    /**
     * Returns the length of the longest string in the given list of strings.  If the list is {@code null} or empty, returns 0.
     *
     * @param _strings The list of strings to examine.
     * @return the length of the longest string in the given list of strings
     */
    public static int longest( final List<String> _strings ) {

        // if we got nothing, return zero...
        if( isNull( _strings ) || _strings.isEmpty() )
            return 0;

        // otherwise, scan all given strings to find the longest one...
        return _strings.stream().max( Comparator.comparing( String::length ) ).get().length();
    }
}
