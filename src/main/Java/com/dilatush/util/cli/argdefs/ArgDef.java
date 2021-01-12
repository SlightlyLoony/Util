package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.parsers.ParameterParser;
import com.dilatush.util.cli.validators.ParameterValidator;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Abstract base class for all argument definitions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class ArgDef {

    /**
     * The reference name for this argument; it must be unique across all the arguments defined for a particular command line.  This name is used
     * mostly in code (especially to retrieve the result of parsing the command line), but it can also be visible to the user in error messages for
     * positional arguments - so care should be taken to use a name that's meaningful to the program's user.
     */
    public final String             referenceName;     // the internal reference name for this argument


    /**
     * A short description of this argument's purpose.  The argument's names and parameter should <i>not</i> be included here, as those are
     * automatically generated when getting summary help for this argument.  Conventionally this summary description should fit on a line, but that
     * is not a requirement.
     */
    public final String             summary;           // a short detail of this option


    /**
     * A detailed description of this argument's purpose.  The argument's names and parameter should <i>not</i> be included here, as those are
     * automatically generated when getting detailed help for this argument.  This description should be as complete as possible, ideally similar
     * to what one might find in a man page.
     */
    public final String             detail;            // an arbitrary length detail of this option


    /**
     * <p>The maximum number of appearances this argument is allowed on the command line.  Zero is a special value that indicates an unlimited number
     * of appearances are allowed.  Negative values are not allowed.</p>
     * <p>For optional arguments, the value of this field will most commonly be one.  However, certain arguments (for example, -v for verbosity) might
     * be allowed to appear multiple times to indicate "more".  It is also possible to have optional arguments with parameters that appear multiple
     * times.  For example, you could define an optional argument for the IP address of a DNS server, and allow several to be specified.</p>
     * <p>For positional arguments, the value of this field might also be one, but often it will be zero.  A good example of the latter case would
     * be an argument that accepted globbed file names, where there might be hundreds of values on the command line.</p>
     */
    public final int                maxAllowed;        // maximum number of appearances allowed; zero means infinity


    /**
     * The user-visible name to use when describing the parameter of an argument.
     */
    public final String             helpName;          // the name to use in help references for this argument's parameter

    /**
     * The {@code Class} object for the type of the parameter.  For optional arguments with a disallowed parameter (aka a binary optional argument),
     * this must be set to the {@link Class} object for {@link Boolean} (because the value is implicitly {@code true} if the argument appears on the
     * command line).
     */
    public final Class<?>           type;              // the type of the argument's parameter value


    /**
     * The {@link ParameterMode} for this argument, which is one of {@link ParameterMode#DISALLOWED}, {@link ParameterMode#OPTIONAL}, or
     * {@link ParameterMode#MANDATORY}.  Note that positional arguments may not have a parameter mode of {@link ParameterMode#DISALLOWED}.
     */
    public final ParameterMode      parameterMode;     // whether a parameter value is disallowed, optional, or mandatory


    /**
     * The default value string for this argument's parameter.  This value must be non-empty when the parameter mode is
     * {@link ParameterMode#OPTIONAL}.  It is the parameter's value when the parameter is not present on the command line.
     */
    public final String             defaultValue;      // the value for the parameter


    /**
     * The optional {@link ParameterParser} for this argument's parameter value.  The parser, if present, will be used to translate the parameter
     * string from the command line into an instance of the parameter type specified in {@link #type}.
     */
    public final ParameterParser    parser;            // the parser for this argument's parameter


    /**
     * The optional {@link ParameterValidator} for this argument's parameter value.  The validator, if present, will be used to validate the parameter
     * value (after parsing, if a parser was also specified) for this argument.
     */
    public final ParameterValidator validator;         // the validator for this argument's parameter


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
     */
    protected ArgDef( final String _referenceName, final String _summary, final String _detail, final int _maxAllowed,
                      final String _helpName, final Class<?> _type, final ParameterMode _parameterMode, final String _defaultValue,
                      final ParameterParser _parser, final ParameterValidator _validator ) {

        referenceName = _referenceName;
        summary = _summary;
        detail = _detail;
        maxAllowed = _maxAllowed;
        helpName = _helpName;
        type = _type;
        parameterMode = _parameterMode;
        defaultValue = _defaultValue;
        parser = _parser;
        validator = _validator;

        // some validation that applies to any kind of argument...
        if( isEmpty( _referenceName ) )
            throw new IllegalArgumentException( "No reference name supplied for argument definition" );
        if( isEmpty( _summary ) )
            throw new IllegalArgumentException( "No summary help supplied for argument definition: " + referenceName );
        if( isEmpty( _detail ) )
            throw new IllegalArgumentException( "No detail help supplied for argument definition: " + referenceName );
        if( isEmpty( _helpName ) && (parameterMode != ParameterMode.DISALLOWED) )
            throw new IllegalArgumentException( "No help name supplied for argument definition: " + referenceName );
        if( _maxAllowed < 0 )
            throw new IllegalArgumentException( "Invalid maximum allowed value for argument definition '" + referenceName + "': " + _maxAllowed );
        if( isNull( type ) )
            throw new IllegalArgumentException( "No parameter type supplied for argument definition: " + referenceName );
        if( isNull( parameterMode ) )
            throw new IllegalArgumentException( "No parameter mode supplied for argument definition: " + referenceName );
        if( isEmpty( defaultValue ) && (parameterMode == ParameterMode.OPTIONAL) )
            throw new IllegalArgumentException( "No parameter mode supplied for argument with optional parameter definition: " + referenceName );
    }
}
