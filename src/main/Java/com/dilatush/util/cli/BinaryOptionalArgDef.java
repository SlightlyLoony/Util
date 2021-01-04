package com.dilatush.util.cli;

/**
 * Instances of this class create a mutable definition for a binary optional argument, with no argument parameter allowed and a single occurrence
 * allowed.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class BinaryOptionalArgDef extends AOptionalArgDef {

    public BinaryOptionalArgDef( final String _referenceName, final String _summary, final String _detail,
                                 final String _shortNames, final String _longNames ) {

        super( _referenceName, _summary, _detail, 1, ParameterMode.DISALLOWED, _shortNames, _longNames );

        type            = Boolean.class;
        defaultValue    = false;
    }
}
