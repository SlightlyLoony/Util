package com.dilatush.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import static com.dilatush.util.General.isNotNull;

/**
 * Singleton class that implements a simple logger.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Logger {

    private static final Logger INSTANCE = new Logger();

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.SSS" ).withZone( ZoneId.of( "GMT-6" ) );
    private static final int MAX_RECENT_ENTRIES = 500;

    private List<String> recents = new LinkedList<>();



    private Logger() {}


    public static void log( final Object _toBeLogged ) {
        INSTANCE.logInt( _toBeLogged );
    }


    public static String[] getMostRecentEntries( final int _numberOfEntries ) {
        return INSTANCE.getMostRecentEntriesInt( _numberOfEntries );
    }


    private synchronized void logInt( final Object _toBeLogged ) {

        StringBuilder sb = new StringBuilder();
        if( _toBeLogged instanceof Throwable )
            sb.append( getExceptionString( (Throwable) _toBeLogged) );
        else if( isNotNull( _toBeLogged ) )
            sb.append( "   " ).append( _toBeLogged.toString() );

        if( sb.length() > 0 ) {
            String logMessage = dtf.format( Instant.now() ) + ' ' + sb.toString();
            System.out.println( logMessage );
            while( recents.size() >= MAX_RECENT_ENTRIES )
                recents.remove( 0 );
            recents.add( logMessage );
        }
    }


    private String getExceptionString( final Throwable _throwable ) {

        StringBuilder sb = new StringBuilder();

        sb.append( _throwable.toString() ).append( '\n' );

        StackTraceElement[] elements = _throwable.getStackTrace();
        for( StackTraceElement element : elements )
            sb.append( "   " ).append( element.toString() ).append( '\n' );

        return sb.toString();
    }


    private synchronized String[] getMostRecentEntriesInt( final int _numberOfEntries ) {
        String[] result = new String[ Math.min( _numberOfEntries, recents.size() ) ];
        List<String> desired = recents.subList( recents.size() - result.length, recents.size() );
        result = desired.toArray( result );
        return result;
    }
}
