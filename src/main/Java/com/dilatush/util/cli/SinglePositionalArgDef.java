package com.dilatush.util.cli;

/**
 * Instances of this class create a mutable argument definition for a positional argument that is allowed one appearance, has a mandatory parameter,
 * does not allow interactive parameters, has a parameter parser, and a parameter validator.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SinglePositionalArgDef extends APositionalArgDef {


    @SuppressWarnings("rawtypes")
    public SinglePositionalArgDef( final String _referenceName, final String _summary, final String _detail,
                                   final Class _type,
                                   final ParameterParser _parser, final ParameterValidator _validator ) {

        super( _referenceName, _summary, _detail, 1 );

        type            = _type;
        parser          = _parser;
        validator       = _validator;
        parameterMode   = ParameterMode.MANDATORY;
        interactiveMode = InteractiveMode.DISALLOWED;

        if( type == null )
            throw new IllegalArgumentException( "No parameter type supplied for argument: " + referenceName );
        if( parser == null )
            throw new IllegalArgumentException( "No parameter parser supplied for argument: " + referenceName );
        if( validator == null )
            throw new IllegalArgumentException( "No parameter validator supplied for argument: " + referenceName );
    }
}
