package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.InteractiveMode;
import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.validators.ParameterValidator;
import com.dilatush.util.cli.parsers.ParameterParser;

import static com.dilatush.util.Strings.isEmpty;
import static com.dilatush.util.cli.ParameterMode.MANDATORY;

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

    // these fields pertain to the argument's parameter...
    public Class<?>           type;              // the type of the option's value
    public ParameterMode      parameterMode;     // whether a parameter value is disallowed, optional, or mandatory
    public String             defaultValue;      // the value for the parameter if optional argument present and optional parameter not present
    public ParameterValidator validator;         // the validator for this argument's parameter
    public ParameterParser    parser;            // the parser for this argument's parameter



    /**
     * Creates a new instance of this class with the given parameters.  In addition:
     * <ul>
     *     <li>{@link #type} is set to {@link String}.class, consistent with no parser.</li>
     *     <li>{@link #defaultValue} is set to the empty string ("").</li>
     *     <li>{@link #parser} is set to <code>null</code>, indicating no parser will be used.</li>
     *     <li>{@link #validator} is set to <code>null</code>, indicating no validator will be used.</li>
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
     * Returns {@code true} if this definition is for a positional argument that must appear exactly once on the command line.
     *
     * @return {@code true} if this definition is for a positional argument that must appear exactly once on the command line
     */
    public boolean isUnitary() {

        return (this instanceof APositionalArgDef) && (maxAllowed == 1) && (parameterMode == MANDATORY);
    }
}
