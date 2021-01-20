package com.dilatush.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import static com.dilatush.util.Misc.getStackTrace;
import static com.dilatush.util.Strings.*;

/**
 * Implements a simple log formatter for use with {@link java.util.logging.Logger}.  Three of the column widths are adjustable by logging properties:
 * <li>
 *     <ul>The message column width defaults to 60 characters and can be set with {@code com.dilatush.util.LogFormatter.messageWidth}.</ul>
 *     <ul>The source (class) column width defaults to 30 characters and can be set with {@code com.dilatush.util.LogFormatter.sourceWidth}.</ul>
 *     <ul>The thread ID column width defaults to 30 characters and can be set with {@code com.dilatush.util.LogFormatter.threadIDWidth}.</ul>
 * </li>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class LogFormatter extends Formatter {

    final static private String BOS = "                                                                                                                        ";

    // maps thread IDs to thread names...
    final private Map<Long,String> threadNames = new ConcurrentHashMap<>();

    final private int messageWidth;
    final private int sourceWidth;
    final private int threadIDWidth;
    final private DateTimeFormatter dateTimeFormatter;


    /**
     * Creates a new instance of this class.
     */
    public LogFormatter() {
        LogManager man = LogManager.getLogManager();
        String mw = man.getProperty( "com.dilatush.util.LogFormatter.messageWidth" );
        String sw = man.getProperty( "com.dilatush.util.LogFormatter.sourceWidth" );
        String tw = man.getProperty( "com.dilatush.util.LogFormatter.threadIDWidth" );

        int mwa = 60;
        if( mw != null ) {
            Integer mwi = General.parseInt( mw );
            if( (mwi != null) && (mwi > 10) )
                mwa = mwi;
        }
        messageWidth = mwa;

        int swa = 30;
        if( sw != null ) {
            Integer swi = General.parseInt( sw );
            if( (swi != null) && (swi > 10) )
                swa = swi;
        }
        sourceWidth = swa;

        int twa = 30;
        if( tw != null ) {
            Integer twi = General.parseInt( tw );
            if( (twi != null) && (twi > 3) )
                twa = twi;
        }
        threadIDWidth = twa;

        dateTimeFormatter = DateTimeFormatter.ofPattern( "yyyy/MM/dd HH:mm:ss.SSS" );
    }


    /**
     * Format the given log record and return the formatted string.  The general form of the message is as follows:
     * <p>
     * (timestamp) (level) (thread ID) (source) (message) (stack trace)
     * <p>
     * The stack trace only appears when there is a throwable in the log record.
     *
     * @param _record the log record to be formatted.
     * @return the formatted log record
     */
    @Override
    public String format( final LogRecord _record ) {

        // build our output string...
        StringBuilder sb = new StringBuilder( 180 );

        // get our timestamp, formatted...
        ZonedDateTime timestamp = ZonedDateTime.ofInstant( Instant.ofEpochMilli( _record.getMillis() ), ZoneId.of( "GMT" ) );
        sb.append( dateTimeFormatter.format( timestamp ) );
        sb.append( ' ' );

        // now the level, left justified in 7 character field...
        sb.append( leftJustify( _record.getLevel().toString(), 7 ) );
        sb.append( ' ' );

        // now the thread ID, left justified in the selected length...
        sb.append( leftJustify( Threads.getThreadName( _record.getThreadID() ), threadIDWidth ) );
        sb.append( ' ' );

        // now the source, right justified in the selected length...
        sb.append( rightJustify( safe( _record.getSourceClassName() ), sourceWidth ) );
        sb.append( ' ' );

        // now the message, left justified...
        sb.append( getMessage( _record ) );

        // if we have a throwable, add the stack trace...
        if( _record.getThrown() != null )
            sb.append( getStackTrace( _record.getThrown() ) );

        // our terminal line separator...
        sb.append( System.lineSeparator() );

        return sb.toString();
    }


    /**
     * If the given log record's message contains no newlines, and is less than the max message width, then just return it.  Otherwise,
     * return the message formatted to print indented on the following lines.
     *
     * @param _record the log record in question
     * @return the formatted message
     */
    private String getMessage( final LogRecord _record ) {

        String msg = safe( _record.getMessage() );
        if( (msg.length() <= messageWidth) && !msg.contains( "\n" ) )
            return msg;

        String[] lines = msg.split( "\\R" );
        StringBuilder result = new StringBuilder( msg.length() + 2 + 2 * (6 + lines.length) );
        result.append( "Message follows: " );
        for( String line : lines ) {
            result.append( System.lineSeparator() );
            result.append( "    " );
            result.append( line );
        }
        return result.toString();
    }
}
