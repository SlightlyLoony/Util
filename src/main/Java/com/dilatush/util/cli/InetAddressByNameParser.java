package com.dilatush.util.cli;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.dilatush.util.Strings.isEmpty;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class InetAddressByNameParser implements ParameterParser {

    private String errorMsg = "";


    /**
     * Translates the given string parameter into an object of the given target class.  Returns <code>null</code> if the translation could not be
     * performed for any reason.  Implementations should avoid throwing exceptions, instead relying on {@link #getErrorMessage()} to inform the caller
     * about <i>why</i> parsing failed.
     *
     * @param _parameter The string parameter to parse and translate.
     * @param _target    The type (class) of the result of parsing.
     * @return an object of the target class, translated from the given string parameter, or <code>null</code> if the translation failed.
     */
    @Override
    public Object parse( final String _parameter, @SuppressWarnings("rawtypes") final Class _target ) {

        if( isEmpty( _parameter ) ) {
            errorMsg = "Expected host name (or dotted-form IP address) was not supplied.";
            return null;
        }

        if( !_target.equals( InetAddress.class ) ) {
            errorMsg = "Expected InetAddress.class, but got " + _target.getCanonicalName();
            return null;
        }

        InetAddress addr;
        try {
            addr = InetAddress.getByName( _parameter );
        }
        catch( UnknownHostException _e ) {
            errorMsg = "Unknown host or invalid IP address: " + _parameter;
            return null;
        }
        return addr;
    }


    /**
     * Return a descriptive error message if the parsing and translation failed for any reason (i.e., {@link #parse(String, Class)} returned
     * <code>null</code>.
     *
     * @return a descriptive error message after parsing and translation failed
     */
    @Override
    public String getErrorMessage() {
        return errorMsg;
    }
}
