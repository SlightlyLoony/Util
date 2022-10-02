package com.dilatush.util;

import java.util.logging.Logger;

import static com.dilatush.util.Strings.isEmpty;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class General {


    /**
     * Initializes the Java logging system with the properties contained in the file at the given path.  This method must be called before any use of the Java logging system.
     *
     * @param _path The path to the logging properties file.
     */
    @SuppressWarnings( "unused" )
    public static void initLogging( final String _path ) {
        Checks.notEmpty( _path );
        System.getProperties().setProperty( "java.util.logging.config.file", _path );
    }


    /**
     * Return a {@link Logger} instance whose name is the canonical name of the caller's class.  If the caller's class cannot be determined, a default logger is returned.
     *
     * @return a new logger.
     */
    public static Logger getLogger() {

        Logger result = null;

        // walk the stack looking for the call to this method, then the next entry is our caller...
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for( int i = 0; i < elements.length; i++ ) {
            StackTraceElement element = elements[i];
            if( "getLogger".equals( element.getMethodName() ) && "com.dilatush.util.General".equals( element.getClassName() ) ) {
                result = Logger.getLogger( elements[i+1].getClassName() );  // create a logger using the next element's class name...
            }
        }
        if( result == null)
            result = Logger.getLogger( "DEFAULT" );

        return result;
    }


    /**
     * Returns a string representing the given exception, using the exception's class name.  If the exception contains a message, it is appended.   This method is intended as
     * a convenience during logging.  If the given exception is {@code null}, an empty string is returned.
     *
     * @param _e The exception to return a string for.
     * @return A string representing the given exception.
     */
    public static String toString( final Exception _e ) {
        if( _e == null ) return "";
        if( _e.getMessage() == null ) return _e.getClass().getSimpleName();
        return _e.getClass().getSimpleName() + " " +  _e.getMessage();
    }


    /**
     * Returns true if <i>any</i> of the given objects are null.
     *
     * @param _obj the objects to test for nullness
     * @return {@code true} if any of the specified objects are null
     */
    public static boolean isNull( final Object... _obj ) {

        if( (_obj == null) || (_obj.length == 0) )
            return true;

        for( Object object : _obj ) {
            if( object == null )
                return true;
        }
        return false;
    }


    /**
     * Returns true if <i>none</i> of the given objects are null.
     *
     * @param _obj the objects to test for nullness
     * @return {@code true} if none of the specified objects are null
     */
    public static boolean isNotNull( final Object... _obj ) {
        return !isNull( _obj );
    }


    /**
     * Attempts to parse the given string into an integer.  Returns null if there are any problems, the parsed integer otherwise.  It will
     * not throw any exceptions.
     *
     * @param _str the string to parse into an integer
     * @return the integer, or {@code null} if there was any problem parsing
     */
    public static Integer parseInt( final String _str ) {
        if( isEmpty( _str) )
            return null;
        try {
            return Integer.parseInt( _str );
        }
        catch( Exception _e ) {
            return null;
        }
    }


    /**
     * Calls to this method may be placed in code to provide a place to set a breakpoint.  The call has no side effects and no return value, so the IDE and compiler won't
     * complain about them.
     */
    @SuppressWarnings( "unused" )
    public static void breakpoint() {
        // naught to do...
    }
}


