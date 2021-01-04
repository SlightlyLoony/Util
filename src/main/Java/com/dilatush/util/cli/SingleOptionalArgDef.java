package com.dilatush.util.cli;

/**
 * Instances of this class create a mutable argument definition for an optional argument that is allowed one appearance, has a mandatory parameter,
 * does not allow interactive parameters, has a value type, has a parameter parser, and has a parameter validator.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SingleOptionalArgDef extends AOptionalArgDef {


    public SingleOptionalArgDef( final String _referenceName, final String _summary, final String _detail,
                                 final String _shortNames, final String _longNames,
                                 final Class<?> _type, final Object _defaultValue,
                                 final ParameterParser _parser, final ParameterValidator _validator ) {

        super( _referenceName, _summary, _detail, 1, ParameterMode.MANDATORY, _shortNames, _longNames );

        type            = _type;
        defaultValue    = _defaultValue;
        parser          = _parser;
        validator       = _validator;

        if( type == null )
            throw new IllegalArgumentException( "No parameter type supplied for argument: " + referenceName );
    }
}
