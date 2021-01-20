package com.dilatush.util;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for utility methods that don't fit well in any other container class.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Misc {


    /**
     * Returns a string that contains the stack trace for the given throwable, formatted as for presentation in a log or on-screen.  The
     * stack trace has a prepended line separator.
     *
     * @param _thrown The {@link Throwable} to get the stack trace for.
     * @return the formatted stack trace
     */
    public static String getStackTrace( final Throwable _thrown ) {

        // if there is no argument, return an empty string...
        if( isNull( _thrown ) )
            return "";

        // get a moderate-sized builder...
        StringBuilder result = new StringBuilder( 500  );

        // prepend a line separator so that in a log it starts on the next line...
        result.append( System.lineSeparator() );

        // The first throwable in the exception chain is the given throwable, and it's prefix will be "Threw: "...
        Throwable current = _thrown;
        String prefix = "Threw: ";

        // walk down the chain of throwables (the causes)...
        while( current != null ) {

            // indent, then show our prefix and exception class on a line...
            result.append( "    " );
            result.append( prefix );
            result.append( current.getClass().getCanonicalName() );
            result.append( System.lineSeparator() );

            // if this throwable has a message, put it on the next indented line...
            if( Strings.isNonEmpty( current.getMessage() ) ) {
                result.append( "    " );
                result.append( "Message: " );
                result.append( current.getMessage() );
                result.append( System.lineSeparator() );
            }

            // dump the stack trace for this throwable, indented a bit more...
            StackTraceElement[] elements = current.getStackTrace();
            for( StackTraceElement element : elements ) {
                result.append( "      " );
                result.append( element.toString() );
                result.append( System.lineSeparator() );
            }

            // advance to the next throwable in the exception chain, and change the prefix...
            current = current.getCause();
            prefix = "Caused by: ";
        }

        // ok, we're done - return the string...
        return result.toString();
    }
}
