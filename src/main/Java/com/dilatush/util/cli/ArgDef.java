package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Abstract base class for all argument definitions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings("rawtypes")
public abstract class ArgDef {

    // these fields apply to argument definitions of any type...
    public String             referenceName;     // the internal reference name for this argument
    public String             summary;           // a short detail of this option
    public String             detail;            // an arbitrary length detail of this option
    public int                maxAllowed;        // maximum number of appearances allowed; zero means infinity

    // these fields pertain to the argument's value...
    public Class              type;              // the type of the option's value
    public Object             defaultValue;      // the default value for the parameter
    public ParameterMode      parameterMode;     // whether a parameter value is disallowed, optional, or mandatory
    public ParameterValidator validator;         // the validator for this argument's parameter
    public ParameterParser    parser;            // the parser for this argument's parameter
    public InteractiveMode    interactiveMode;   // whether a prompt for this parameter value is disallowed, plain text, or obscured text
    public String             prompt;            // if interactive is allowed, the prompt for the value


    protected ArgDef( final String _referenceName, final String _summary, final String _detail, final int _maxAllowed ) {

        referenceName = _referenceName;
        summary       = _summary;
        detail        = _detail;
        maxAllowed    = _maxAllowed;

        if( isEmpty( _referenceName ) )
            throw new IllegalArgumentException( "No reference name supplied for argument definition" );
        if( isEmpty( _summary ) )
            throw new IllegalArgumentException( "No summary help supplied for argument definition: " + referenceName );
        if( isEmpty( _detail ) )
            throw new IllegalArgumentException( "No detail help supplied for argument definition: " + referenceName );
        if( _maxAllowed < 0 )
            throw new IllegalArgumentException( "Invalid maximum allowed value for argument definition '" + referenceName + "': " + _maxAllowed );
    }


    public void setInteractiveMode( final String _prompt ) {

        interactiveMode = InteractiveMode.PLAIN;
        prompt = _prompt;

        if( isEmpty( _prompt ) )
            throw new IllegalArgumentException( "No prompt for interactive mode supplied for: " + referenceName );
    }


    public void setHiddenInteractiveMode( final String _prompt ) {

        interactiveMode = InteractiveMode.HIDDEN;
        prompt = _prompt;

        if( isEmpty( _prompt ) )
            throw new IllegalArgumentException( "No prompt for interactive mode supplied for: " + referenceName );
    }
}
