package com.dilatush.util.cli.parsers;

import com.dilatush.util.AConfig;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parameter parser that treats the parameter as a file path.  The file's contents are assumed to be a script (in JavaScript) that can
 * create, initialize, and validate a configuration object of the specified class.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class AConfigParser extends AParameterParser implements ParameterParser {

    private final Class<? extends AConfig> type;


    /**
     * Creates a new instance of this class that will create and initialize an {@link AConfig} of the given type.
     *
     * @param _type The type of the configuration object to create, initialize, and validate.
     */
    public AConfigParser( final Class<? extends AConfig> _type ) {

        if( isNull( _type ) )
            throw new IllegalArgumentException( "Need a configuration object type" );

        type = _type;
    }


    /**
     * Translates the JavaScript configuration file with the given file name into an initialized configuration object.
     *
     * @param _parameter The JavaScript configuration file name.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {

        // if we got no parameter...
        if( isEmpty( _parameter ) )
            return error( "Expected JavaScript configuration file name, got nothing." );

        // try to get our initialized AConfig instance...
        AConfig.InitResult ir = AConfig.init( type, _parameter );

        // if we got invalid results, report them...
        if( !ir.valid)
            return error( ir.message );

        // otherwise, return the good stuff...
        return result( ir.config );
    }
}
