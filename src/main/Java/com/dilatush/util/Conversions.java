package com.dilatush.util;

/**
 * Static container class for methods that convert values from one unit to another.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Conversions {


    /**
     * Converts the given value in meters/second to the equivalent value in miles/hour.
     *
     * @param _ms the value in meters/second
     * @return the value in miles/hour
     */
    public static double fromMStoMPH( final double _ms ) {
        return _ms * 2.23694f;
    }


    /**
     * Converts the given value in miles/hour to the equivalent value in meters/second.
     *
     * @param _mph the value in miles/hour
     * @return the value in meters/second
     */
    public static double fromMPHtoMS( final double _mph ) {
        return _mph * 1 / 2.23694f;
    }


    /**
     * Converts the given value in millimeters to the equivalent value in inches.
     *
     * @param _mm the value in millimeters
     * @return the value in inches
     */
    public static double fromMMtoIN( final double _mm ) {
        return _mm * 0.0393701f;
    }


    /**
     * Converts the given value in grams per cubic meter to ounces per cubic yard.
     *
     * @param _gm3 the value in grams per cubic meter
     * @return the value in ounces per cubic yard
     */
    public static double fromGM3toOY3( final double _gm3 ) {
        return _gm3 * 0.02696892f;
    }


    /**
     * Converts the given value in kilopascals to the equivalent value in inches of mercury.
     *
     * @param _kp the value in kilopascals
     * @return the value in inches of mercury
     */
    public static double fromKPtoIHG( final double _kp ) {
        return _kp * 0.2953f;
    }


    /**
     * Converts the given value in degrees Celsius to the equivalent value in degrees Fahrenheit.
     *
     * @param _dC the value in degrees Celsius
     * @return the value in degrees Fahrenheit
     */
    public static double fromCtoF( final double _dC ) {
        return (_dC * 9f / 5f) + 32f;
    }


    /**
     * Converts the given value in Celsius degree-days to the equivalent value in Fahrenheit degree-days.
     *
     * @param _dC the value in Celsius degree-days
     * @return the value in Fahrenheit degree-days
     */
    public static double fromCDDtoFDD( final double _dC ) {
        return _dC * 9f / 5f;
    }
}
