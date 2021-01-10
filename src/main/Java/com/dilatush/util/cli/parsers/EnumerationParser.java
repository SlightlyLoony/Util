package com.dilatush.util.cli.parsers;

import java.util.HashMap;
import java.util.Map;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a parameter parser that translates a parameter string to an enumerated value, with configurable case sensitivity.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EnumerationParser extends AParameterParser implements ParameterParser {

    private final Map<String, Enum<?>> names;
    private final boolean              caseSensitive;


    /**
     * Creates a new instance of this class with the given target class and case sensitivity.
     *
     * @param _target the target class for this {@link ParameterParser}.
     * @param _caseSensitive {@code true} if this {@link ParameterParser} is case sensitive.
     */
    public EnumerationParser( final Class<?> _target, final boolean _caseSensitive ) {

        if( isNull( _target ) )
            throw new IllegalArgumentException( "Enum class is required" );
        if( !_target.isEnum() )
            throw new IllegalArgumentException( "Enum class is required" );

        caseSensitive = _caseSensitive;

        names = new HashMap<>();
        Enum<?>[] instances = (Enum<?>[]) _target.getEnumConstants();
        for( Enum<?> e : instances ) {
            names.put( caseSensitive ? e.toString() : e.toString().toUpperCase(), e );
        }
    }


    /**
     * Creates a new instance of this class with the given target class and that is not case sensitive.
     *
     * @param _target the target class for this {@link ParameterParser}.
     */
    public EnumerationParser( final Class<?> _target ) {
        this( _target, false );
    }


    /**
     * Translates the given string parameter into an instance of the {@link Enum} subclass specified when this {@link ParameterParser} was
     * constructed.  Whether this translation is case sensitive was also specified when this {@link ParameterParser} was constructed.
     *
     * @param _parameter The string parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {

        if( isEmpty( _parameter ) )
            return error( "Parameter is empty" );

        Enum<?> value = names.get( caseSensitive ? _parameter : _parameter.toUpperCase() );
        if( isNull( value ) )
            return error( "Invalid value: " + _parameter );

        return result( value );
    }
}
