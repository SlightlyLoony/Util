package com.dilatush.util.cli.argdefs;

import java.util.ArrayList;
import java.util.List;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a convenient way to define the short and long names for an optional argument definition.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class OptArgNames {


    /**
     * The short names (e.g., the "c" for "-c") that may be used on the command line for this argument.
     */
    public final String[] shortNames;


    /**
     * The long names (e.g., the "count" for "--count") that may be used on the command line for this argument.
     */
    public final String[] longNames;


    /**
     * The explanatory message when there is a problem parsing the names in the constructor; {@code null} if there was no problem.
     */
    public final String message;


    /**
     * Creates a new instance of this class using the given string pattern.  The pattern consists of a comma-separated list of short optional
     * argument names (which must be a single character long), followed by a semicolon, and finally a comma-separated list of long optional
     * names.  For example, the pattern {@code "c,q;count,quotes"} would resolve to the short names {@code c} and {@code q}, and the long names
     * {@code count} and {@code quotes}.  If the pattern does not contain a semicolon, then all names are treated as short names.  The pattern must
     * define at least one name (either short or long).  On successful parsing, the explanatory {@link #message} will be {@code null}, and the
     * {@link #shortNames} and {@link #longNames} values will be set.  If there is any problem with parsing the pattern, an explanatory
     * {@link #message} will be set, and both {@link #shortNames} and {@link #longNames} will be {@code null}.  To include a backslash, comma or a
     * semicolon in a name, precede it with a backslash.  Argument names may not contain an equals sign ({@code =}.  Long argument names may not
     * contain a hyphen ("-").  A short option name of hyphen <i>is</i> permitted, as a special case of an optional argument that is just a hyphen.
     *
     * @param _pattern the pattern to parse
     */
    public OptArgNames( final String _pattern ) {

        // some setup...
        List<String> shorts = new ArrayList<>();
        List<String> longs  = new ArrayList<>();
        boolean inShorts    = true;
        StringBuilder name  = new StringBuilder();
        boolean inEscape    = false;
        String msg          = null;

        try {
            // if we didn't get a pattern, we've definitely got a problem!
            if( isEmpty( _pattern ) ) throw new NamesException( "Names pattern is empty" );

            // iterate over all our characters...
            for( char c : _pattern.toCharArray() ) {

                // if we're in an escape sequence, handle it...
                if( inEscape ) {

                    // if we've got a backslash, comma, or semicolon, then accumulate it...
                    if( (c == '\\') ||  (c == ',') || (c == ';') )
                        name.append( c );

                    // otherwise, it's invalid...
                    else throw new NamesException( "Invalid escape sequence ('\\" + c + "') in names pattern: " + _pattern );

                    // we're out of escape mode now...
                    inEscape = false;
                }

                // if we have a backslash, start an escape sequence...
                else if( c == '\\' ) {
                    inEscape = true;
                }

                // if we have a comma (name separator), add the accumulated name and move along...
                else if( c == ',' ) {
                    addName( _pattern, shorts, longs, inShorts, name );
                }

                // do we have a semicolon (short/long names separator)?
                else if( c == ';' ) {

                    // if we have a name accumulated, add it...
                    if( name.length() > 0 )
                        addName( _pattern, shorts, longs, inShorts, name );

                    // if we're in short names, flip to long names...
                    if( inShorts )
                        inShorts = false;

                    // otherwise we must have multiple semicolons; bail out...
                    else throw new NamesException( "More than one semicolon in names pattern: " + _pattern );
                }

                // check for = or -, which are disallowed...
                else if( ((c == '-') && !inShorts) || (c == '=') )
                    throw new NamesException( "Name patterns may not include a hyphen ('-') or equals sign ('='): " + _pattern );

                // accumulate the character into the name...
                else
                    name.append( c );
            }

            // if we're in an escape sequence, that means our pattern ended with a backslash, definitely not ok...
            if( inEscape ) throw new NamesException( "Names pattern ended with a backslash: " + _pattern );

            // if we get here, then we've successfully parsed the line, but might have a name accumulated that we need to deal with...
            if( name.length() > 0 )
                addName( _pattern, shorts, longs, inShorts, name );
        }

        catch( NamesException _e ) {
            msg = _e.getMessage();
            shorts = null;
            longs = null;
        }

        // if we make it here, we're good to go - woo hoo!
        shortNames = (shorts != null) ? shorts.toArray( new String[0] ) : null;
        longNames  = (longs != null)  ? longs.toArray( new String[0] )  : null;
        message = msg;
    }


    private void addName( final String _pattern, final List<String> _shorts, final List<String> _longs,
                          final boolean _inShorts, final StringBuilder _name ) throws NamesException {

        // if we're in short names, then we'd better have exactly one character in our name...
        if( _inShorts ) {

            // if we're good, add this name to our shorts and move along...
            if( _name.length() == 1 ) {
                _shorts.add( _name.toString() );
                _name.setLength( 0 );
            }

            // otherwise, leave a message and bail out...
            else throw new NamesException( "Illegal short optional argument name: '" + _name.toString() + "'" );
        }

        // we're in long names, so anything with at least one character is ok...
        else {
            if( _name.length() > 0 ) {
                _longs.add( _name.toString() );
                _name.setLength( 0 );
            }

            // otherwise, leave a message and bail out...
            else throw new NamesException( "Empty long optional argument name in pattern: " + _pattern );
        }
    }


    private static class NamesException extends Exception {

        public NamesException( final String message ) {
            super( message );
        }
    }
}
