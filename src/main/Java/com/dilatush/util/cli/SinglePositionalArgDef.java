package com.dilatush.util.cli;

/**
 * Instances of this class create a mutable argument definition for a positional argument that is allowed one appearance, has a mandatory parameter,
 * does not allow interactive parameters, has a parameter parser, and has a parameter validator.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SinglePositionalArgDef extends APositionalArgDef {


    public SinglePositionalArgDef( final String _referenceName, final String _summary, final String _detail,
                                   final Class<?> _type,
                                   final ParameterParser _parser, final ParameterValidator _validator ) {

        super( _referenceName, _summary, _detail, 1, ParameterMode.MANDATORY );

        type            = _type;
        defaultValue    = null;
        absentValue     = null;
        parser          = _parser;
        validator       = _validator;

        if( type == null )
            throw new IllegalArgumentException( "No parameter type supplied for argument: " + referenceName );
    }
}
