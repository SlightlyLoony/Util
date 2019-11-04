package com.dilatush.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.dilatush.util.Bash.UnquoteState.*;

/**
 * Static container class for functions related to the BASH command shell.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Bash {


    /**
     * Returns the given string as a single-quoted string, suitable for use as a command-line argument.  Unless the given string itself contains a
     * single quote, the returned string is simply the given string surrounded with single quotes.  Single quotes in the argument are escaped with a
     * backslash and the rest of the characters single quoted.  For instance, given "abc'def", this function returns "'abc'\''def'".
     *
     * @param _str the string to single-quote
     * @return the single-quoted string
     */
    static public String singleQuote( final String _str ) {
        Checks.required( _str );
        String[] parts = _str.split( "'" );
        StringBuilder sb = new StringBuilder();
        for( String part : parts ) {
            if( sb.length() != 0 )
                sb.append( "\\'" );
            sb.append( "'" );
            sb.append( part );
            sb.append( "'" );
        }
        return sb.toString();
    }


    /**
     * Returns the given string as a double-quoted string, suitable for use as a command-line argument.  The given string is surrounded by double
     * quotes, and any backslash ("\") or dollar sign ("$") characters inside the string are escaped with backslashes.  Note that there is no
     * expansion of variable names ("$xxx") or globbing ("*").
     *
     * @param _str the string to double-quote
     * @return the double-quoted string
     */
    static public String doubleQuote( final String _str ) {
        Checks.required( _str );
        String unesc1 = _str.replaceAll( "\\\\", Matcher.quoteReplacement("\\\\" ) );
        String unesc2 = unesc1.replaceAll( "\\$", Matcher.quoteReplacement( "\\$" ) );
        return "\"" + unesc2 + "\"";
    }


    /**
     * Unquotes the given string (which is assumed to be a command-line) exactly as Bash would, returning a list of the command and its arguments
     * sans quotes, again exactly as Bash would.  If there are any unpaired quotes or a backslash at the end, throws an
     * {@link IllegalArgumentException}.
     *
     * @param _str the command line to unquote
     * @return the list of command and arguments
     */
    // TODO: this method has a bad bug - if an argument is double-quoted, the quotes get stripped - bad
    static public List<String> unquote( final String _str ) {
        Checks.required( _str );
        UnquoteState state = InSpace;
        UnquoteState backTo = InSpace;
        StringBuilder arg = new StringBuilder();
        List<String> result = new ArrayList<>();
        for( char c : _str.toCharArray() ) {

            switch( state ) {
                case InSpace:
                    switch( c ) {
                        case ' ':
                            break;
                        case '\'':
                            backTo = InText;
                            state = InSingleQuote;
                            break;
                        case '"':
                            state = InDoubleQuote;
                            break;
                        case '\\':
                            state = InBackslash;
                            break;
                        default:
                            state = InText;
                            arg.append( c );
                            break;
                    }
                    break;
                case InText:
                    switch( c ) {
                        case ' ':
                            result.add( arg.toString() );
                            arg.setLength( 0 );
                            state = InSpace;
                            break;
                        case '\'':
                            state = InSingleQuote;
                            break;
                        case '"':
                            state = InDoubleQuote;
                            break;
                        case '\\':
                            backTo = InText;
                            state = InBackslash;
                            break;
                        default:
                            arg.append( c );
                            break;
                    }
                    break;
                case InBackslash:
                    arg.append( c );
                    state = backTo;
                    break;
                case InDoubleQuote:
                    switch( c ) {
                        case '"':
                            state = InText;
                            break;
                        case '\\':
                            backTo = InDoubleQuote;
                            state = InBackslash;
                            break;
                        default:
                            arg.append( c );
                            break;
                    }
                    break;
                case InSingleQuote:
                    if( c == '\'' )
                        state = InText;
                    else
                        arg.append( c );
                    break;
            }
        }

        if( state == InSingleQuote )
            throw new IllegalArgumentException( "Unmatched single quote in argument: " + _str );
        if( state == InDoubleQuote )
            throw new IllegalArgumentException( "Unmatched double quote in argument: " + _str );
        if( state == InBackslash )
            throw new IllegalArgumentException( "Trailing backslash in argument: " + _str );

        if( arg.length() > 0 )
            result.add( arg.toString() );

        return result;
    }


    public enum UnquoteState {
        InSpace, InText, InSingleQuote, InDoubleQuote, InBackslash;
    }
}
