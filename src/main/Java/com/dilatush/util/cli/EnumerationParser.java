package com.dilatush.util.cli;

import java.util.HashMap;
import java.util.Map;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a parameter parser that translates a parameter string to an enumerated value.  The translation is insensitive to case.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EnumerationParser implements ParameterParser {

    private final Map<String, Enum<?>> names;

    private String errorMsg;


    public EnumerationParser( final Class<?> _template ) {

        if( isNull( _template ) )
            throw new IllegalArgumentException( "Enum class is required" );
        if( !_template.isEnum() )
            throw new IllegalArgumentException( "Enum class is required" );

        names = new HashMap<>();
        Enum<?>[] instances = (Enum<?>[]) _template.getEnumConstants();
        for( Enum<?> e : instances ) {
            names.put( e.toString().toUpperCase(), e );
        }
    }

    /**
     * Translates the given string parameter into an object of the given target class.  Returns <code>null</code> if the translation could not be
     * performed for any reason.  Implementations should avoid throwing exceptions, instead relying on {@link #getErrorMessage()} to inform the caller
     * about <i>why</i> parsing failed.
     *
     * @param _parameter The string parameter to parse and translate.
     * @return an object of the target class, translated from the given string parameter, or <code>null</code> if the translation failed.
     */
    @Override
    public Object parse( final String _parameter ) {

        if( isEmpty( _parameter ) ) {
            errorMsg = "Parameter is empty";
            return null;
        }
        Enum<?> result = names.get( _parameter.toUpperCase() );
        if( isNull( result ) ) {
            errorMsg = "Invalid value: " + _parameter;
            return null;
        }

        return result;
    }


    /**
     * Return a descriptive error message if the parsing and translation failed for any reason (i.e., {@link #parse(String)} returned
     * <code>null</code>.
     *
     * @return a descriptive error message after parsing and translation failed
     */
    @Override
    public String getErrorMessage() {
        return errorMsg;
    }
}
