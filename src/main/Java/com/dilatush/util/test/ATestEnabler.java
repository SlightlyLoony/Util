package com.dilatush.util.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;

/**
 * The abstract base class for all {@link TestEnabler} implementations.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ abstract class ATestEnabler implements TestEnabler {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // the map of this instance's properties...
    private final Map<String, Object> properties;


    /* package-private */ ATestEnabler( final Map<String, Object> _properties ) {

        // make a copy of the given map, for safety...
        properties = new ConcurrentHashMap<>( _properties );
    }


    public boolean isEnabled() {
        return enabled();
    }


    /* package-private */ abstract boolean enabled();


    /**
     * Returns <code>true</code> if this instance has a property with the given name.
     *
     * @param _name The name of the property to test.
     * @return <code>true</code> if this instance has a property with the given name
     */
    @Override
    public boolean has( final String _name ) {
        if( isNull( _name ) )
            return false;
        return properties.containsKey( _name );
    }


    /**
     * Returns the value of the property with the given name.  If the property does not exist, a <code>null</code> is returned and a warning is
     * logged.
     *
     * @param _name the name of the property to retrieve.
     * @return the value of the property with the given name
     */
    @Override
    public Object get( final String _name ) {
        if( isNull( _name ) ) {
            LOGGER.warning( "Attempted to get value of property with a null name" );
            return null;
        }
        Object result = properties.get( _name );
        if( isNull( result ) )
            LOGGER.warning( "Property \"" + _name + " has a null value" );
        return result;
    }


    /**
     * Returns the string value of the property with the given name.  If the property's value is not a <code>String</code>, then the value's
     * <code>toString()</code> method is used to get the string.  If the property does not exist, an empty string ("") is returned and a warning
     * is logged.
     *
     * @param _name The name of the property to retrieve.
     * @return the string value of the property with the given name
     */
    @Override
    public String getAsString( final String _name ) {
        if( isNull( _name ) ) {
            LOGGER.warning( "Attempted to get string value of property with a null name" );
            return "";
        }
        Object result = properties.get( _name );
        if( isNull( result ) ) {
            LOGGER.warning( "String property \"" + _name + " has a null value" );
            return "";
        }
        if( result instanceof String )
            return (String) result;
        return result.toString();
    }


    /**
     * Returns the boolean value of the property with the given name.  If the property's value is a <code>Boolean</code>, the value is returned
     * directly.  If it is a <code>String</code>, the result of <code>Boolean.parseBoolean(String)</code> is returned.  If the value returned is an
     * instance of <code>Number</code>, the result of <code>doubleValue() != 0</code> is returned.  If the property's value is any other type, or does
     * not exist, then <code>false</code> is returned and a warning is logged.
     *
     * @param _name The name of the property to retrieve.
     * @return the boolean value of the property with the given name
     */
    @Override
    public boolean getAsBoolean( final String _name ) {
        if( isNull( _name ) ) {
            LOGGER.warning( "Attempted to get boolean value of property with a null name" );
            return false;
        }
        Object result = properties.get( _name );
        if( isNull( result ) ) {
            LOGGER.warning( "Boolean property \"" + _name + " has a null value" );
            return false;
        }
        if( result instanceof Boolean )
            return (boolean) result;
        if( result instanceof String )
            return Boolean.parseBoolean( (String) result );
        if( result instanceof Number )
            return ((Number) result).doubleValue() != 0;
        LOGGER.warning( "Property " + _name + " cannot be converted to Boolean" );
        return false;
    }


    /**
     * Returns the integer value of the property with the given name.  If the property's value is an <code>Integer</code>, <code>Short</code>, or
     * <code>Byte</code>, the value is returned directly.  If it is a <code>Boolean</code>, then a 1 or 0 is returned as the value is
     * <code>true</code> or <code>false</code>.  If the property's value is a <code>String</code>, then the successful result of
     * <code>Integer.parseInt(String)</code> is returned.  If the <code>parseInt(String)</code> failed, or if the property's value is any other type,
     * or does not exist, then 0 is returned and a warning is logged.
     *
     * @param _name The name of the property to retrieve.
     * @return the integer value of the property with the given name
     */
    @Override
    public int getAsInt( final String _name ) {
        if( isNull( _name ) ) {
            LOGGER.warning( "Attempted to get int value of property with a null name" );
            return 0;
        }
        Object result = properties.get( _name );
        if( isNull( result ) ) {
            LOGGER.warning( "Integer property \"" + _name + " has a null value" );
            return 0;
        }
        if( (result instanceof Integer) || (result instanceof Short) || (result instanceof Byte)  )
            return ((Number) result).intValue();
        if( result instanceof Boolean )
            return ((Boolean) result) ? 1 : 0;
        if( result instanceof String ) {
            try {
                return Integer.parseInt( (String) result );
            }
            catch( NumberFormatException _e ) {
                LOGGER.warning( "Property " + _name + " with value '" + ((String) result) + "' could not be parsed as an integer" );
                return 0;
            }
        }
        LOGGER.warning( "Property " + _name + " cannot be converted to integer" );
        return 0;
    }


    /**
     * Returns the double value of the property with the given name.  If the property's value is an instance of <code>Number</code>, the value is
     * returned directly.  Note that for a code>Long</code> it is possible that some precision will be lost.  If it is a <code>Boolean</code>, then a
     * 1 or 0 is returned as the value is <code>true</code> or <code>false</code>.  If the property's value is a <code>String</code>, then the
     * successful result of <code>Double.parseDouble(String)</code> is returned.  If the <code>parseDouble(String)</code> failed, or if property's
     * value is any other type, or does not exist, then 0 is returned and a warning is logged.
     *
     * @param _name The name of the property to retrieve.
     * @return the double value of the property with the given name
     */
    @Override
    public double getAsDouble( final String _name ) {
        if( isNull( _name ) ) {
            LOGGER.warning( "Attempted to get double value of property with a null name" );
            return 0;
        }
        Object result = properties.get( _name );
        if( isNull( result ) ) {
            LOGGER.warning( "Double property \"" + _name + " has a null value" );
            return 0;
        }
        if( result instanceof Number )
            return ((Number) result).doubleValue();
        if( result instanceof Boolean )
            return ((Boolean) result) ? 1 : 0;
        if( result instanceof String ) {
            try {
                return Double.parseDouble( (String) result );
            }
            catch( NumberFormatException _e ) {
                LOGGER.warning( "Property " + _name + " with value '" + ((String) result) + "' could not be parsed as a double" );
                return 0;
            }
        }
        LOGGER.warning( "Property " + _name + " cannot be converted to double" );
        return 0;
    }
}
