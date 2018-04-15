package com.dilatush.util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implements a JSON object that optionally uses dotted identifiers to allow flat access to a hierarchy of JSON objects.  This class extends
 * {@link JSONObject} to provide getters, setters, and testers that use dotted identifiers to represent elements within a hierarchy.  For example,
 * "app.name" refers to the "name" attribute within the top-level "app" object.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class HJSONObject extends JSONObject {


    public HJSONObject() {
        super();
    }


    public HJSONObject( final String source ) throws JSONException {
        super( source );
    }


    public Object getDotted( final String _key ) throws JSONException {
        return new DottedRef( _key ).get();
    }


    public boolean getBooleanDotted( final String _key ) throws JSONException {
        return (boolean) getDotted( _key );
    }


    public double getDoubleDotted( final String key ) throws JSONException {
        return ((Number) getDotted( key )).doubleValue();
    }


    public float getFloatDotted( final String key ) throws JSONException {
        return ((Number) getDotted( key )).floatValue();
    }


    public Number getNumberDotted( final String key ) throws JSONException {
        return (Number) getDotted( key );
    }


    public int getIntDotted( final String key ) throws JSONException {
        return ((Number) getDotted( key )).intValue();
    }


    public long getLongDotted( final String key ) throws JSONException {
        return ((Number) getDotted( key )).longValue();
    }


    public String getStringDotted( final String key ) throws JSONException {
        return (String) getDotted( key );
    }


    public boolean hasDotted( final String key ) {
        return new DottedRef( key ).has();
    }


    public Object optDotted( final String key ) {
        return new DottedRef( key ).opt();
    }


    public boolean optBooleanDotted( final String key ) {
        return (boolean) optDotted( key );
    }


    public boolean optBooleanDotted( final String key, final boolean defaultValue ) {
        return new DottedRef( key ).optBoolean( defaultValue );
    }


    public double optDoubleDotted( final String key ) {
        return ((Number) optDotted( key )).doubleValue();
    }


    public double optDoubleDotted( final String key, final double defaultValue ) {
        return new DottedRef( key ).optDouble( defaultValue );
    }


    public float optFloatDotted( final String key ) {
        return ((Number) optDotted( key )).floatValue();
    }


    public float optFloatDotted( final String key, final float defaultValue ) {
        return new DottedRef( key ).optFloat( defaultValue );
    }


    public int optIntDotted( final String key ) {
        return ((Number) optDotted( key )).intValue();
    }


    public int optIntDotted( final String key, final int defaultValue ) {
        return new DottedRef( key ).optInt( defaultValue );
    }


    public long optLongDotted( final String key ) {
        return ((Number) optDotted( key )).longValue();
    }


    public long optLongDotted( final String key, final long defaultValue ) {
        return new DottedRef( key ).optLong( defaultValue );
    }


    public Number optNumberDotted( final String key ) {
        return (Number) optDotted( key );
    }


    public Number optNumberDotted( final String key, final Number defaultValue ) {
        return new DottedRef( key ).optNumber( defaultValue );
    }


    public String optStringDotted( final String key ) {
        return (String) optDotted( key );
    }


    public String optStringDotted( final String key, final String defaultValue ) {
        return new DottedRef( key ).optString( defaultValue );
    }


    public JSONObject putDotted( final String key, final boolean value ) throws JSONException {
        return putDotted( key, (Boolean) value );
    }


    public JSONObject putDotted( final String key, final double value ) throws JSONException {
        return putDotted( key, (Double) value );
    }


    public JSONObject putDotted( final String key, final float value ) throws JSONException {
        return putDotted( key, (Float) value );
    }


    public JSONObject putDotted( final String key, final int value ) throws JSONException {
        return putDotted( key, (Integer) value );
    }


    public JSONObject putDotted( final String key, final long value ) throws JSONException {
        return putDotted( key, (Long) value );
    }


    public JSONObject putDotted( final String key, final Object value ) throws JSONException {
        return new DottedRef( key ).put( value );
    }


    public JSONObject putOptDotted( final String key, final Object value ) throws JSONException {
        return new DottedRef( key ).putOpt( value );
    }


    public Object removeDotted( final String key ) {
        return new DottedRef( key ).remove();
    }


    private class DottedRef {

        private JSONObject parent;
        private String key;

        private DottedRef( final String _key ) {

            // get the parent object of the one we're going to set a value in, creating them as necessary...
            String[] parts = _key.split( "\\." );
            JSONObject current = HJSONObject.this;
            for( int i = 0; i < parts.length - 1; i++ ) {

                // if we don't have a key with this name, create a JSONObject under that name...
                if( !current.has( parts[i] ) ) current.put( parts[i], new JSONObject() );

                // update our current...
                current = current.getJSONObject( parts[i] );
            }

            // save our reference parts...
            key = parts[ parts.length - 1 ];
            parent = current;
        }


        private Object get() {
            return parent.get( key );
        }


        private boolean has() {
            return parent.has( key );
        }


        private Object opt() {
            return parent.opt( key );
        }


        private boolean optBoolean( final boolean defaultValue ) {
            return parent.optBoolean( key, defaultValue );
        }


        private double optDouble( final double defaultValue ) {
            return parent.optDouble( key, defaultValue );
        }


        private float optFloat( final float defaultValue ) {
            return parent.optFloat( key, defaultValue );
        }


        private int optInt( final int defaultValue ) {
            return parent.optInt( key, defaultValue );
        }


        private long optLong( final long defaultValue ) {
            return parent.optLong( key, defaultValue );
        }


        private Number optNumber( final Number defaultValue ) {
            return parent.optNumber( key, defaultValue );
        }


        private String optString( final String defaultValue ) {
            return parent.optString( key, defaultValue );
        }


        private HJSONObject put( final Object value ) {
            parent.put( key, value );
            return HJSONObject.this;
        }


        private HJSONObject putOpt( final Object value ) {
            parent.putOpt( key, value );
            return HJSONObject.this;
        }


        private Object remove() {
            return parent.remove( key );
        }
    }
}
