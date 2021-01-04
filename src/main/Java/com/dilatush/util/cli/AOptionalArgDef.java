package com.dilatush.util.cli;

/**
 * Provides an abstract base class with a convenience constructor and defaults for optional argument definitons.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class AOptionalArgDef extends ArgDef {

    public char[]             shortNames;        // all the short (one character) names for this option, concatenated into a single string
    public String[]           longNames;         // all the long (one or more characters) names for this option, space separated


    public AOptionalArgDef( final String _referenceName, final String _summary, final String _detail,
                            final int _maxAllowed, final ParameterMode _parameterMode, final String _shortNames, final String _longNames ) {

        super( _referenceName, _summary, _detail, _maxAllowed, _parameterMode );

        shortNames      = toCharArray( _shortNames );
        longNames       = toStringArray( _longNames );

        if( (shortNames.length + longNames.length) == 0 )
            throw new IllegalArgumentException( "No names (short or long) were supplied for argument definition: " + referenceName );
    }


    protected char[] toCharArray( final String _shortNames ) {
        if( (_shortNames == null) || (_shortNames.length() == 0) )
            return new char[0];
        return _shortNames.toCharArray();
    }


    protected String[] toStringArray( final String _longNames ) {
        if( (_longNames == null) || (_longNames.length() == 0) )
            return new String[0];
        return _longNames.split( " +" );
    }
}
