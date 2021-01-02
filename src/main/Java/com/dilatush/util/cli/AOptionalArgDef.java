package com.dilatush.util.cli;

/**
 * Provides an abstract base class with a convenience constructor and defaults for optional argument definitons.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AOptionalArgDef extends ArgDef {


    public AOptionalArgDef( final String _referenceName, final String _summary, final String _detail,
                            final int _maxAllowed, final String _shortNames, final String _longNames ) {

        super( _referenceName, _summary, _detail, _maxAllowed );

        shortNames      = toCharArray( _shortNames );
        longNames       = toStringArray( _longNames );

        if( (shortNames.length + longNames.length) == 0 )
            throw new IllegalArgumentException( "No names (short or long) were supplied for argument definition: " + referenceName );
    }
}
