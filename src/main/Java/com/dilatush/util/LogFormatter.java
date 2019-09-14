package com.dilatush.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Implements a simple log formatter for use with {@link java.util.logging.Logger}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class LogFormatter extends Formatter {

    final static private String BOS = "                                                                                                                        ";

    final private int messageWidth;
    final private int sourceWidth;
    final private DateTimeFormatter dateTimeFormatter;


    /**
     * Creates a new instance of this class.
     */
    public LogFormatter() {
        LogManager man = LogManager.getLogManager();
        String mw = man.getProperty( "com.dilatush.util.LogFormatter.messageWidth" );
        String sw = man.getProperty( "com.dilatush.util.LogFormatter.sourceWidth" );
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
        dateTimeFormatter = DateTimeFormatter.ofPattern( "yyyy/MM/dd HH:mm:ss.SSS" );
    }

    /**
     * Format the given log record and return the formatted string.  The general form of the message is as follows:
     * <p>
     * (timestamp) (level) (source) (message) (stack trace)
     * <p>
     * The stack trace only appears when there is a throwable in the log record.  The length of the message is configurable with the property
     * com.dilatush.util.LogFormatter.messageWidth, and the length of the source with com.dilatush.util.LogFormatter.sourceWidth.
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

        // now the level, left justified in 8 character field...
        sb.append( left( _record.getLevel().toString(), 7 ) );
        sb.append( ' ' );

        // now the source, right justified in the selected length...
        sb.append( right( safe( _record.getSourceClassName() ), sourceWidth ) );
        sb.append( ' ' );

        // now the message, left truncated...
        sb.append( getMessage( _record ) );

        // if we have a throwable, add the stack trace...
        if( _record.getThrown() != null )
            sb.append( getStackTrace( _record.getThrown() ) );

        // our terminal line separator...
        sb.append( System.lineSeparator() );

        return sb.toString();
    }


    private String safe( final String _arg ) {
        return (_arg == null) ? "" : _arg;
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


    private String getStackTrace( final Throwable _thrown ) {
        StringBuilder result = new StringBuilder( 500  );
        result.append( System.lineSeparator() );

        Throwable current = _thrown;
        String reason = "Threw: ";
        while( current != null ) {
            result.append( "    " );
            result.append( reason );
            result.append( current.getClass().getCanonicalName() );
            result.append( System.lineSeparator() );
            if( Strings.isNonEmpty( current.getMessage() ) ) {
                result.append( "    " );
                result.append( "Message: " );
                result.append( safe( current.getMessage() ) );
                result.append( System.lineSeparator() );
            }
            StackTraceElement[] elements = current.getStackTrace();
            for( StackTraceElement element : elements ) {
                result.append( "      " );
                result.append( element.toString() );
                result.append( System.lineSeparator() );
            }

            current = current.getCause();
            reason = "Caused by: ";
        }
        return result.toString();
    }


    private String left( final String _field, final int _width ) {
        if( _field.length() == _width )
            return _field;
        if( _field.length() > _width )
            return _field.substring( 0, _width - 1 ) + "…";
        return _field + BOS.substring( 0, _width - _field.length() );
    }


    private String leftTrunc( final String _field, final int _width ) {
        if( _field.length() <= _width )
            return _field;
        return _field.substring( 0, _width - 1 ) + "…";
    }


    private String right( final String _field, final int _width ) {
        if( _field.length() == _width )
            return _field;
        if( _field.length() > _width )
            return "…" + _field.substring( 1 + _field.length() - _width );
        return BOS.substring( 0, _width - _field.length() ) + _field;
    }
}
