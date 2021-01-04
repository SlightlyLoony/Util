package com.dilatush.util.cli;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Abstract base class for all argument definitions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class ArgDef {

    // these fields apply to argument definitions of any type...
    public String             referenceName;     // the internal reference name for this argument
    public String             summary;           // a short detail of this option
    public String             detail;            // an arbitrary length detail of this option
    public int                maxAllowed;        // maximum number of appearances allowed; zero means infinity

    // these fields pertain to the argument's value...
    public Class<?>           type;              // the type of the option's value
    public Object             defaultValue;      // the default value for the parameter
    public String             environVariable;   // name of environment variable as alternate parameter value source, or null if none
    public ParameterMode      parameterMode;     // whether a parameter value is disallowed, optional, or mandatory
    public ParameterValidator validator;         // the validator for this argument's parameter
    public ParameterParser    parser;            // the parser for this argument's parameter
    public InteractiveMode    interactiveMode;   // whether a prompt for this parameter value is disallowed, plain text, or obscured text
    public String             prompt;            // if interactive is allowed, the prompt for the value


    /**
     * Creates a new instance of this class with the given parameters.  In addition:
     * <ul>
     *     <li>{@link #type} is set to {@link String}.class, consistent with no parser.</li>
     *     <li>{@link #defaultValue} is set to the empty string ("").</li>
     *     <li>{@link #parser} is set to <code>null</code>, indicating no parser will be used.</li>
     *     <li>{@link #validator} is set to <code>null</code>, indicating no validator will be used.</li>
     *     <li>{@link #interactiveMode} is set to {@link InteractiveMode#DISALLOWED}, indicating that the parameter value will not be captured
     *     interactively.</li>
     *     <li>{@link #prompt} is set to {@code true}, consistent with interactive mode being disallowed.</li>
     *     <li>{@link #environVariable} is set to {@code null}, indicating that no environment variable will be used for a parameter value source</li>
     * </ul>
     * @param _referenceName The reference name for this argument.
     * @param _summary The summary help for this argument.
     * @param _detail The detailed help for this argument.
     * @param _maxAllowed The maximum number of appearances for this argument (or zero for any number of appearances).
     * @param _parameterMode The mode for the parameter of this argument (disallowed, optional, or mandatory).
     */
    protected ArgDef( final String _referenceName, final String _summary, final String _detail, final int _maxAllowed,
                      final ParameterMode _parameterMode ) {

        referenceName   = _referenceName;
        summary         = _summary;
        detail          = _detail;
        maxAllowed      = _maxAllowed;
        parameterMode   = _parameterMode;

        type            = String.class;
        defaultValue    = "";
        validator       = null;
        parser          = null;
        interactiveMode = InteractiveMode.DISALLOWED;
        prompt          = null;
        environVariable = null;

        if( isEmpty( _referenceName ) )
            throw new IllegalArgumentException( "No reference name supplied for argument definition" );
        if( isEmpty( _summary ) )
            throw new IllegalArgumentException( "No summary help supplied for argument definition: " + referenceName );
        if( isEmpty( _detail ) )
            throw new IllegalArgumentException( "No detail help supplied for argument definition: " + referenceName );
        if( _maxAllowed < 0 )
            throw new IllegalArgumentException( "Invalid maximum allowed value for argument definition '" + referenceName + "': " + _maxAllowed );
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
