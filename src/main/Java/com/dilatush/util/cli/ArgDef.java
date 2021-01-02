package com.dilatush.util.cli;

/**
 * Instances of this class describe a command line optional parameter.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings("rawtypes")
public class ArgDef {

    // these fields apply to argument definitions of any type...
    public final ArgumentType       argType;           // the type of argument (optional or positional)
    public final String             referenceName;     // the internal reference name for this argument
    public final String             summary;           // a short description of this option
    public final String             description;       // an arbitrary length description of this option
    public final ArgumentArity      arity;             // the (allowable) arity of this argument

    // these fields pertain to the argument's value...
    public final Class              type;              // the type of the option's value
    public final ParameterValidator validator;         // the validator for this argument's parameter
    public final ParameterParser    parser;            // the parser for this argument's parameter
    public final Object             defaultValue;      // the default value for the parameter
    public final ParameterAllowed   parameterAllowed;  // whether a parameter value is disallowed, optional, or mandatory
    public final InteractiveMode    interactiveMode;   // whether a prompt for this parameter value is disallowed, plain text, or obscured text
    public final String             prompt;             // if interactive is allowed, the prompt for the value

    // these fields apply only to Optional argument definitions...
    public final char[]             shortOptionNames;  // all the short (one character) names for this option
    public final String[]           longOptionNames;   // all the long (one or more characters) names for this option

    // these fields apply only to Positional argument definitions...


    /**
     * Create a new instance of this class that defines an optional argument.
     *
     * @param _referenceName The name used internally to refer to this argument.  The name must be unique across all arguments.
     * @param _summary A summary description of this argument (used in the summary help).
     * @param _description A detailed description of this argument (used in the detailed help).
     * @param _arity The arity of this argument.
     * @param _parameterAllowed Whether a parameter value is disallowed, optional, or mandatory for this argument.
     * @param _interactiveMode Whether a prompt for this parameter value is disallowed, plain text, or obscured text.
     * @param _prompt The interactive prompt, if allowed.
     * @param _type The class object for the type of this argument's value.
     * @param _validator The validator for this argument.
     * @param _parser The parser for this argument, which translates the command line string parameter to a Java value.
     * @param _defaultValue The default value for this argument (used if it is not present on the command line).
     * @param _shortOptionNames The short (single character) names of this argument on the command line.
     * @param _longOptionNames The long (string) names of this argument on the command line.
     */
    public ArgDef( final String _referenceName, final String _summary, final String _description, final ArgumentArity _arity,
                   final ParameterAllowed _parameterAllowed, final InteractiveMode _interactiveMode, final String _prompt, final Class _type,
                   final ParameterValidator _validator, final ParameterParser _parser,
                   final Object _defaultValue, final char[] _shortOptionNames, final String[] _longOptionNames ) {

        argType          = ArgumentType.OPTIONAL;
        referenceName    = _referenceName;
        summary          = _summary;
        description      = _description;
        arity            = _arity;
        parameterAllowed = _parameterAllowed;
        interactiveMode  = _interactiveMode;
        prompt           = _prompt;
        type             = _type;
        validator        = _validator;
        parser           = _parser;
        defaultValue     = _defaultValue;
        shortOptionNames = _shortOptionNames;
        longOptionNames  = _longOptionNames;
    }


    /**
     * Create a new instance of this class that defines an optional counting argument with no parameters.  This might be used, for example, with
     * a <code>-vv</code> to define increasing levels of verbosity.
     *
     * @param _referenceName The name used internally to refer to this argument.  The name must be unique across all arguments.
     * @param _summary A summary description of this argument (used in the summary help).
     * @param _description A detailed description of this argument (used in the detailed help).
     * @param _maxArity The maximum arity of this argument (i.e., the maximum number of times it may appear).
     * @param _shortOptionNames The short (single character) names of this argument on the command line.
     * @param _longOptionNames The long (string) names of this argument on the command line.
     */
    public ArgDef( final String _referenceName, final String _summary, final String _description, final int _maxArity,
                   final char[] _shortOptionNames, final String[] _longOptionNames ) {

        argType          = ArgumentType.OPTIONAL;
        referenceName    = _referenceName;
        summary          = _summary;
        description      = _description;
        arity            = new ArgumentArity( 0, _maxArity );
        parameterAllowed = ParameterAllowed.DISALLOWED;
        interactiveMode  = InteractiveMode.DISALLOWED;
        prompt           = null;
        type             = Integer.class;
        validator        = null;
        parser           = null;
        defaultValue     = 0;
        shortOptionNames = _shortOptionNames;
        longOptionNames  = _longOptionNames;
    }


    /**
     * Create a new instance of this class that defines an optional binary argument (an optional argument where a parameter value is disallowed, has
     * a boolean value where the presence of the argument is <code>true</code> and the absence of the argument is <code>false</code>.
     *
     * @param _referenceName The name used internally to refer to this argument.  The name must be unique across all arguments.
     * @param _summary A summary description of this argument (used in the summary help).
     * @param _description A detailed description of this argument (used in the detailed help).
     * @param _shortOptionNames The short (single character) names of this argument on the command line.
     * @param _longOptionNames The long (string) names of this argument on the command line.
     */
    public ArgDef( final String _referenceName, final String _summary, final String _description,
                   final char[] _shortOptionNames, final String[] _longOptionNames ) {

        argType          = ArgumentType.OPTIONAL;
        referenceName    = _referenceName;
        summary          = _summary;
        description      = _description;
        arity            = ArgumentArity.OPTIONAL_SINGLE;
        parameterAllowed = ParameterAllowed.DISALLOWED;
        interactiveMode  = InteractiveMode.DISALLOWED;
        prompt           = null;
        type             = Boolean.class;
        validator        = null;
        parser           = null;
        defaultValue     = Boolean.FALSE;
        shortOptionNames = _shortOptionNames;
        longOptionNames  = _longOptionNames;
    }


    /**
     * Create a new instance of this class that defines a positional argument.
     *
     * @param _referenceName The name used internally to refer to this argument.  The name must be unique across all arguments.
     * @param _summary A summary description of this argument (used in the summary help).
     * @param _description A detailed description of this argument (used in the detailed help).
     * @param _arity The arity of this argument.
     * @param _parameterAllowed Whether a parameter value is disallowed, optional, or mandatory for this argument.
     * @param _interactiveMode Whether a prompt for this parameter value is disallowed, plain text, or obscured text.
     * @param _prompt The interactive prompt, if allowed.
     * @param _type The class object for the type of this argument's value.
     * @param _validator The validator for this argument.
     * @param _parser The parser for this argument, which translates the command line string parameter to a Java value.
     * @param _defaultValue The default value for this argument (used if it is not present on the command line).
     */
    public ArgDef( final String _referenceName, final String _summary, final String _description, final ArgumentArity _arity,
                   final ParameterAllowed _parameterAllowed, final InteractiveMode _interactiveMode, final String _prompt, final Class _type,
                   final ParameterValidator _validator, final ParameterParser _parser, final Object _defaultValue ) {

        argType          = ArgumentType.POSITIONAL;
        referenceName    = _referenceName;
        summary          = _summary;
        description      = _description;
        arity            = _arity;
        parameterAllowed = _parameterAllowed;
        interactiveMode  = _interactiveMode;
        prompt           = _prompt;
        type             = _type;
        validator        = _validator;
        parser           = _parser;
        defaultValue     = _defaultValue;
        shortOptionNames = null;
        longOptionNames  = null;
    }
}
