package com.dilatush.util.test;

/**
 * Implemented by classes providing test enablers.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface TestEnabler {

    /**
     * Returns <code>true</code> if this test is currently enabled.
     *
     * @return <code>true</code> if this test is currently enabled
     */
    boolean isEnabled();


    /**
     * Returns <code>true</code> if this instance has a property with the given name.
     *
     * @param _name The name of the property to test.
     * @return <code>true</code> if this instance has a property with the given name
     */
    boolean has( final String _name );


    /**
     * Returns the value of the property with the given name.  If the property does not exist, a <code>null</code> is returned and a warning
     * is logged.
     *
     * @param _name the name of the property to retrieve.
     * @return the value of the property with the given name
     */
    Object get( final String _name );


    /**
     * Returns the string value of the property with the given name.  If the property's value is not a <code>String</code>, then the value's
     * <code>toString()</code> method is used to get the string.  If the property does not exist, an empty string ("") is returned and a warning
     * is logged.
     *
     * @param _name The name of the property to retrieve.
     * @return the string value of the property with the given name
     */
    String getAsString( final String _name );


    /**
     * Returns the boolean value of the property with the given name.  If the property's value is a <code>Boolean</code>, the value is returned
     * directly.  If it is a <code>String</code>, the result of <code>Boolean.parseBoolean(String)</code> is returned.  If the value returned is
     * an instance of <code>Number</code>, the result of <code>doubleValue() != 0</code> is returned.  If the property's value is any other type,
     * or does not exist, then <code>false</code> is returned and a warning is logged.
     *
     * @param _name The name of the property to retrieve.
     * @return the boolean value of the property with the given name
     */
    boolean getAsBoolean( final String _name );


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
    int getAsInt( final String _name );


    /**
     * Returns the double value of the property with the given name.  If the property's value is a <code>Double</code>, <code>Float</code>
     * <code>Long</code>, <code>Integer</code>, <code>Short</code>, or <code>Byte</code>, the value is returned directly.  Note that for a
     * <code>Long</code> it is possible that some precision will be lost.  If it is a <code>Boolean</code>, then a 1 or 0 is returned as the value is
     * <code>true</code> or <code>false</code>.  If the property's value is a <code>String</code>, then the successful result of
     * <code>Double.parseDouble(String)</code> is returned.  If the <code>parseDouble(String)</code> failed, or if property's value is any other type,
     * or does not exist, then 0 is returned and a warning is logged.
     *
     * @param _name The name of the property to retrieve.
     * @return the double value of the property with the given name
     */
    double getAsDouble( final String _name );
}
