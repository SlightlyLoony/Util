package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.InteractiveMode;
import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.ParsedArg;
import com.dilatush.util.cli.parsers.BooleanParser;
import com.dilatush.util.cli.parsers.ParameterParser;
import com.dilatush.util.cli.validators.ParameterValidator;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Defines an optional argument.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class OptArgDef extends ArgDef {


    /**
     * The short names (e.g., the "c" for "-c") that may be used on the command line for this argument.
     */
    public final String[]           shortNames;        // all the short (one character) names for this optional argument


    /**
     * The long names (e.g., the "count" for "--count") that may be used on the command line for this argument.
     */
    public final String[]           longNames;         // all the long (one or more characters) names for this optional argument


    /**
     * The optional value of this argument's parameter when the argument does not appear on the command line.  If this value is {@code null}, the
     * {@link ParsedArg#value} and {@link ParsedArg#values} will be {@code null} when the argument is absent from the command line.
     */
    public final String             absentValue;       // the value for the parameter if optional argument not present and parameter not disallowed


    /**
     * The interactive capture mode for this argument.  This field may be {@code null} unless this argument's parameter mode is
     * {@link ParameterMode#MANDATORY}, in which case it will be {@link InteractiveMode#DISALLOWED}, {@link InteractiveMode#PLAIN}, or
     * {@link InteractiveMode#HIDDEN}.  If {@link InteractiveMode#DISALLOWED}, then there will be no interactive parameter capture.  If
     * {@link InteractiveMode#PLAIN} and no parameter is specified on the command line, then the parameter will be captured interactively with
     * the user-entered text appearing in plain text.  If {@link InteractiveMode#HIDDEN} and no parameter is specified on the command line, then the
     * parameter will be captured interactively with the user-entered text hidden (generally with asterisks); this is suitable for password or other
     * secure value capture.
     */
    public final InteractiveMode    interactiveMode;   // whether a prompt for this parameter value is disallowed, plain text, or obscured text


    /**
     * The prompt for interactive capture.  This field may be {@code null} unless this argument's parameter mode is {@link ParameterMode#MANDATORY}
     * and its {@link #interactiveMode} is not {@link InteractiveMode#DISALLOWED}, in which case its value is the prompt used when capturing
     * the parameter interactively.
     */
    public final String             prompt;            // if interactive is allowed, the prompt for the value


    /**
     * Creates a new instance of this class with the given values.
     *
     * @param _referenceName The reference name for this argument.
     * @param _summary The summary description of this argument.
     * @param _detail The detailed description of this argument.
     * @param _maxAllowed The maximum number of appearances on the command line for this argument.
     * @param _helpName The name for this argument's parameter in help text.
     * @param _type The type of this argument's parameter.
     * @param _parameterMode The parameter mode for this argument's parameter.
     * @param _defaultValue The default value for this argument's parameter.
     * @param _parser The optional parser for this argument's parameter.
     * @param _validator The optional validator for this argument's parameter.
     * @param _names The short and long names for this argument.
     * @param _absentValue The parameter value if this option is not present on the command line.
     * @param _interactiveMode The interactive mode for this argument's parameter.
     * @param _prompt The prompt for this argument when its parameter is capture interactively.
     */
    public OptArgDef( final String _referenceName, final String _summary, final String _detail, final int _maxAllowed, final String _helpName,
                      final Class<?> _type, final ParameterMode _parameterMode, final String _defaultValue,
                      final ParameterParser _parser, final ParameterValidator _validator,
                      final OptArgNames _names, final String _absentValue,
                      final InteractiveMode _interactiveMode, final String _prompt ) {

        super( _referenceName, _summary, _detail, _maxAllowed, _helpName, _type, _parameterMode, _defaultValue, _parser, _validator );

        shortNames      = _names.shortNames;
        longNames       = _names.longNames;
        absentValue     = _absentValue;
        interactiveMode = _interactiveMode;
        prompt          = _prompt;

        // if we had any problems with the names, bail out...
        if( !isEmpty( _names.message ) )
            throw new IllegalArgumentException( _names.message );

        // some validation...
        if( isNull( interactiveMode ) && (parameterMode == ParameterMode.MANDATORY) )
            throw new IllegalArgumentException( "No interactive mode was supplied" );
        if( isEmpty( prompt ) && (parameterMode == ParameterMode.MANDATORY) && (interactiveMode != InteractiveMode.DISALLOWED) )
            throw new IllegalArgumentException( "No interactive prompt was supplied but interactive capture is allowed" );
    }


    /**
     * Returns an instance of {@link OptArgDef} with the given values, with no parameters allowed, a type of boolean, an absent value of {@code false},
     * and a maximum of one appearance.
     *
     * @param _referenceName The reference name for this argument.
     * @param _summary The summary help for this argument.
     * @param _detail The detailed help for this argument.
     * @param _names The short and long names for this argument.
     * @return an instance of {@link OptArgDef} with the given values
     */
    public static OptArgDef getSingleBinaryOptArgDef(final String _referenceName, final String _summary,
                                                     final String _detail, final OptArgNames _names) {

        return new OptArgDef(
                _referenceName, _summary, _detail, 1, null, Boolean.class, ParameterMode.DISALLOWED,
                "false", new BooleanParser(), null, _names, "false", null, null
        );
    }


    /**
     * Returns a string containing a representation of this argument on the command line.  First the names are listed in the same order they were
     * defined, short names first, then long names, with each name separated by a ", " sequence.  If this argument has a mandatory parameter, the
     * names are followed by an equals sign and the parameter help name in angle brackets.  If this argument has an optional parameter, the names
     * are followed by an equals sign and the parameter help name in angle brackets, all enclosed in square brackets.
     *
     * @return a string containing all the names of this argument
     */
    public String getArgumentDescription() {

        StringBuilder result = new StringBuilder();

        // first emit the short names...
        for( String shortName : shortNames ) {
            if( result.length() > 0 )
                result.append( ", " );
            result.append( '-' );
            result.append( shortName );
        }

        // then the long names...
        for( String longName : longNames ) {
            if( result.length() > 0 )
                result.append( ", " );
            result.append( "--" );
            result.append( longName );
        }

        // if we have an optional or mandatory parameter, the parameter is next...
        if( parameterMode != ParameterMode.DISALLOWED ) {
            result.append( ' ' );
            if( parameterMode == ParameterMode.OPTIONAL )
                result.append( '[' );
            result.append( "=<" );
            result.append( helpName );
            result.append( '>' );
            if( parameterMode == ParameterMode.OPTIONAL )
                result.append( ']' );
        }

        return result.toString();
    }
}
