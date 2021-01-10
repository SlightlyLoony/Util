package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.parsers.ParameterParser;
import com.dilatush.util.cli.validators.ParameterValidator;

/**
 * Instances of this class create a mutable argument definition for an optional argument that is allowed multiple appearances, has a mandatory
 * parameter, an absent parameter value, does not allow interactive parameters, has a value type, has a parameter parser, and has a parameter
 * validator.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MultipleOptionalArgDef extends AOptionalArgDef {


    public MultipleOptionalArgDef( final String _referenceName, final String _summary, final String _detail,
                                   final String _shortNames, final String _longNames,
                                   final int _maxAllowed, final Class<?> _type, final String _absentValue,
                                   final ParameterParser _parser, final ParameterValidator _validator ) {

        super( _referenceName, _summary, _detail, _maxAllowed, ParameterMode.MANDATORY, _shortNames, _longNames );

        type            = _type;
        defaultValue    = null;
        absentValue     = _absentValue;
        parser          = _parser;
        validator       = _validator;

        if( type == null )
            throw new IllegalArgumentException( "No parameter type supplied for argument: " + referenceName );
    }
}
