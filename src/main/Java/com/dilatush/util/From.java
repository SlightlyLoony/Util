package com.dilatush.util;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Static container class for functions that convert to one type from another type, and that return a {@link Result}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class From {


    /**
     * Convert the given string to a double.
     *
     * @param _s The string to convert.
     * @return the {@link Result} with the double converted from the given string, or an explanatory message if could not be converted.
     */
    @SuppressWarnings( "unused" )
    public static Result<Double> doubleFromString( final String _s ) {

        try {
            return new Result<>( Double.parseDouble( _s ) );
        }
        catch( final Exception _e ) {
            return new Result<>( _e.getClass().getSimpleName() + " " + _e.getMessage() );
        }
    }


    /**
     * Convert the given string to a float.
     *
     * @param _s The string to convert.
     * @return the {@link Result} with the float converted from the given string, or an explanatory message if could not be converted.
     */
    @SuppressWarnings( "unused" )
    public static Result<Float> floatFromString( final String _s ) {

        try {
            return new Result<>( Float.parseFloat( _s ) );
        }
        catch( final Exception _e ) {
            return new Result<>( _e.getClass().getSimpleName() + " " + _e.getMessage() );
        }
    }


    /**
     * Convert the given string to a long.
     *
     * @param _s The string to convert.
     * @return the {@link Result} with the long converted from the given string, or an explanatory message if could not be converted.
     */
    @SuppressWarnings( "unused" )
    public static Result<Long> longFromString( final String _s ) {

        try {
            return new Result<>( Long.parseLong( _s ) );
        }
        catch( final Exception _e ) {
            return new Result<>( _e.getClass().getSimpleName() + " " + _e.getMessage() );
        }
    }


    /**
     * Convert the given string to a integer.
     *
     * @param _s The string to convert.
     * @return the {@link Result} with the integer converted from the given string, or an explanatory message if could not be converted.
     */
    @SuppressWarnings( "unused" )
    public static Result<Integer> integerFromString( final String _s ) {

        try {
            return new Result<>( Integer.parseInt( _s ) );
        }
        catch( final Exception _e ) {
            return new Result<>( _e.getClass().getSimpleName() + " " + _e.getMessage() );
        }
    }


    /**
     * Convert the given string to a short.
     *
     * @param _s The string to convert.
     * @return the {@link Result} with the short converted from the given string, or an explanatory message if could not be converted.
     */
    @SuppressWarnings( "unused" )
    public static Result<Short> shortFromString( final String _s ) {

        try {
            return new Result<>( Short.parseShort( _s ) );
        }
        catch( final Exception _e ) {
            return new Result<>( _e.getClass().getSimpleName() + " " + _e.getMessage() );
        }
    }


    /**
     * Convert the given string to a byte.
     *
     * @param _s The string to convert.
     * @return the {@link Result} with the byte converted from the given string, or an explanatory message if could not be converted.
     */
    @SuppressWarnings( "unused" )
    public static Result<Byte> byteFromString( final String _s ) {

        try {
            return new Result<>( Byte.parseByte( _s ) );
        }
        catch( final Exception _e ) {
            return new Result<>( _e.getClass().getSimpleName() + " " + _e.getMessage() );
        }
    }


    /**
     * Convert the given string to a boolean.  The following conversion rules are used:
     * <ul>
     *     <li>Returns a valid, exact result if the given string equals "true" or "false", ignoring case.</li>
     *     <li>Returns a valid, inexact result if "true" or "false" start with the given string, ignoring case.</li>
     *     <li>Returns a valid, inexact result if the given string equals "on" or "off", ignoring case.</li>
     *     <li>Returns a valid, inexact result if the given string equals "1" or "0".</li>
     *     <li>Returns an invalid result for any other given string.</li>
     * </ul>
     *
     * @param _s The string to convert.
     * @return the {@link Result} with the boolean converted from the given string, or an explanatory message if could not be converted.
     */
    @SuppressWarnings( "unused" )
    public static Result<Boolean> booleanFromString( final String _s ) {

        if( isEmpty( _s ) )
            return new Result<>( "No string to convert to boolean" );

        if( "true".equalsIgnoreCase( _s ) )
            return new Result<>( Boolean.TRUE );

        if( "false".equalsIgnoreCase( _s ) )
            return new Result<>( Boolean.FALSE );

        if( "true".startsWith( _s.toLowerCase() ) )
            return new Result<>( Boolean.TRUE, Result.Type.VALID_INEXACT, null );

        if( "false".startsWith( _s.toLowerCase() ) )
            return new Result<>( Boolean.FALSE, Result.Type.VALID_INEXACT, null );

        if( "on".equalsIgnoreCase( _s ) )
            return new Result<>( Boolean.TRUE, Result.Type.VALID_INEXACT, null );

        if( "off".equalsIgnoreCase( _s ) )
            return new Result<>( Boolean.FALSE, Result.Type.VALID_INEXACT, null );

        if( "1".equals( _s ) )
            return new Result<>( Boolean.TRUE, Result.Type.VALID_INEXACT, null );

        if( "0".equals( _s ) )
            return new Result<>( Boolean.FALSE, Result.Type.VALID_INEXACT, null );

        return new Result<>( "Cannot convert \"" + _s + "\" to boolean" );
    }
}
