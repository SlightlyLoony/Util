package com.dilatush.util.cli.parsers;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Provides a parameter parser that translates a parameter string into an {@link InetAddress} instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class InetAddressParser extends AParameterParser implements ParameterParser {


    /**
     * Translates a string parameter containing either a host name or dotted-form IP address into an {@link InetAddress} instance.
     *
     * @param _parameter The string parameter to parse and translate.
     * @return a {@link Result} object containing the results of the parsing operation
     */
    @Override
    public Result parse( final String _parameter ) {

        if( isEmpty( _parameter ) )
            return error( "Expected host name (or dotted-form IP address) was not supplied." );

        try {
            InetAddress addr = InetAddress.getByName( _parameter );
            return result( addr );
        }
        catch( UnknownHostException _e ) {
            return error( "Unknown host or invalid IP address: " + _parameter );
        }
    }
}
