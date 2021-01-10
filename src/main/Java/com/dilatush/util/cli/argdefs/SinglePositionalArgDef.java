package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.validators.ParameterValidator;
import com.dilatush.util.cli.parsers.ParameterParser;

/**
 * Instances of this class create a mutable argument definition for a positional argument that is allowed one appearance, has a mandatory parameter,
 * has a parameter parser, and has a parameter validator.
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
        parser          = _parser;
        validator       = _validator;

        if( type == null )
            throw new IllegalArgumentException( "No parameter type supplied for argument: " + referenceName );
    }
}
