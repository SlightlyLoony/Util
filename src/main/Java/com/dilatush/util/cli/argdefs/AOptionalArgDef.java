package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.InteractiveMode;
import com.dilatush.util.cli.ParameterMode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides an abstract base class with a convenience constructor and defaults for optional argument definitions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AOptionalArgDef extends ArgDef {

    private final static Pattern TEST_LONG_NAME = Pattern.compile( "[^\\s^=]+" );

    public String[]           shortNames;        // all the short (one character) names for this optional argument
    public String[]           longNames;         // all the long (one or more characters) names for this optional argument
    public String             absentValue;       // the value for the parameter if optional argument not present and parameter not disallowed
    public InteractiveMode    interactiveMode;   // whether a prompt for this parameter value is disallowed, plain text, or obscured text
    public String             prompt;            // if interactive is allowed, the prompt for the value


    // TODO: default and absent values need to be checked here, so they should be in the constructor
    // TODO: absent value MUST be present
    // TODO: default value is only required if parameter mode is OPTIONAL
    /**
     * a short name of hyphen is the special case of a standalone hyphen
     *
     * @param _referenceName
     * @param _summary
     * @param _detail
     * @param _maxAllowed
     * @param _parameterMode
     * @param _shortNames
     * @param _longNames
     */
    public AOptionalArgDef( final String _referenceName, final String _summary, final String _detail,
                            final int _maxAllowed, final ParameterMode _parameterMode, final String _shortNames, final String _longNames ) {
        super( _referenceName, _summary, _detail, _maxAllowed, _parameterMode );

        shortNames = processShortNames( _shortNames );
        longNames  = processLongNames( _longNames );

        // if we have no names at all, barf...
        if( (shortNames.length + longNames.length) == 0 )
            throw new IllegalArgumentException( "No names (short or long) were supplied for argument definition: " + referenceName );
    }


    /**
     * Convert the given string of short (one character) names to a character array.  If an invalid character is used for a short name, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param _shortNames  The string containing the short names.
     * @return the validated short names in a character array
     */
    protected String[] processShortNames( final String _shortNames ) {

        // handle the case of no short names...
        if( isEmpty( _shortNames ) )
            return new String[0];

        // otherwise convert the string to an array of single character strings...
        String[] shortNames = _shortNames.split( "" );

        // then check the validity of each short name...
        for( String shortName : shortNames ) {

            if( Character.isWhitespace( shortName.charAt( 0 ) ) || "=".equals( shortName ) )
                throw new IllegalArgumentException( "Invalid short optional argument name for " + referenceName + ": '-" + shortName + "'" );
        }

        // if we survived validation, return the short names...
        return shortNames;
    }


    /**
     * Convert the given list of space-separated long option names into a string array.  If any of the names contain invalid characters, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param _longNames The string containing the space-separated list of long names.
     * @return the validated long names in a string array
     */
    protected String[] processLongNames( final String _longNames ) {

        // handle the case of no long names...
        if( (_longNames == null) || (_longNames.length() == 0) )
            return new String[0];

        // make our array of long names...
        String[] longNames = _longNames.split( "\\s+" );

        // then check the validity of each name...
        for( String longName : longNames ) {

            Matcher matcher = TEST_LONG_NAME.matcher( longName );
            if( !matcher.matches() )
                throw new IllegalArgumentException( "Invalid long optional argument name for " + referenceName + ": '--" + longName + "'" );
        }

        // if we survived validation, return the long names...
        return longNames;
    }


    /**
     * Sets {@link #interactiveMode} to {@link InteractiveMode#PLAIN}, and {@link #prompt} to the given prompt.  This is the ordinary use of the
     * interactive capability, with the characters the user types echoed on his screen.
     *
     * @param _prompt The prompt to use when capturing this argument interactively.
     */
    public void setInteractiveMode( final String _prompt ) {

        interactiveMode = InteractiveMode.PLAIN;
        prompt = _prompt;

        if( isEmpty( _prompt ) )
            throw new IllegalArgumentException( "No prompt for interactive mode supplied for: " + referenceName );
    }


    /**
     * Sets {@link #interactiveMode} to {@link InteractiveMode#HIDDEN}, and {@link #prompt} to the given prompt.  This is normally used for passwords
     * or other secret information.  The characters the user types are echoed as asterisks so that someone looking over his or her shoulder cannot
     * see what is being typed.
     *
     * @param _prompt The prompt to use when capturing this argument interactively.
     */
    @SuppressWarnings( "unused" )
    public void setHiddenInteractiveMode( final String _prompt ) {

        interactiveMode = InteractiveMode.HIDDEN;
        prompt = _prompt;

        if( isEmpty( _prompt ) )
            throw new IllegalArgumentException( "No prompt for interactive mode supplied for: " + referenceName );
    }
}
