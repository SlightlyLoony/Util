package com.dilatush.util.cli;

/**
 * Provides an abstract base class with a convenience constructor and defaults for optional argument definitions.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class APositionalArgDef extends ArgDef {


    public APositionalArgDef( final String _referenceName, final String _summary, final String _detail,
                              final int _maxAllowed, final ParameterMode _parameterMode ) {

        super( _referenceName, _summary, _detail, _maxAllowed, _parameterMode );
    }

    // TODO: default value needs to be checked, so they need to be in the constructor; only required if parameter mode is OPTIONAL
    // TODO: absent value should be null
}
