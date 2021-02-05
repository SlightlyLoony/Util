package com.dilatush.util.cli.example;

import com.dilatush.util.Files;
import com.dilatush.util.cli.CommandLine;
import com.dilatush.util.cli.ParsedCommandLine;
import com.dilatush.util.cli.example.CounterCommandLine.CountType;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Command line application that provides an example of using the {@link CommandLine} argument interpreter.  This application accepts one or more
 * file paths and performs various counting operations on them, as specified by various optional arguments.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Counter {

    public static void main( final String[] _args ) {

        // parse our command line...
        ParsedCommandLine result = CounterCommandLine.getParseAndHandle( _args );

        // get the kind of count we're going to make...
        CountType countType = (CountType) result.getValue( "countType" );

        // get any specified words..
        List<?> words = (List<?>) result.getValues( "word" );
        boolean haveWords = result.isPresent( "word" );

        // get our case sensitivity...
        boolean caseSensitive = (Boolean) result.getValue( "case" );

        // get our regular expression...
        String regex = (String) result.getValue( "regex" );

        // if we're counting regexes, then we better have a regex!
        if( (countType == CountType.REGEX) && (regex == null) ) {
            out( "Counting regex hits, but no regex was supplied.  Specify a -r or --regex option." );
            System.exit( 1 );
        }

        // if we have a regex, compile it...
        Pattern pattern = null;
        if( countType == CountType.REGEX ) {
            try {
                pattern = Pattern.compile( regex );
            }
            catch( PatternSyntaxException _e ) {
                out( "Invalid regex: " + regex );
                out( _e.getMessage() );
                System.exit( 1 );
            }
        }

        // initialize our counter...
        int counter = 0;

        // iterate over all the specified files...
        for( Object fileObj  : (List<?>) result.getValues( "files" ) ) {
            File file = (File) fileObj;

            // get our file's text...
            String text = Files.readToString( file );
            if( text == null ) continue;

            // do the right kind of counting...
            switch( countType ) {

                case WORDS:
                    if( !haveWords )
                        counter += text.split( "\\s+" ).length;
                    else
                        for( Object wordObj : words ) { counter += countSpecificWords( (String) wordObj, text, caseSensitive ); }
                    break;

                case LINES:
                    counter += text.split( "\\R" ).length;
                    break;

                case LOC:
                    counter += countLinesOfCode( text );
                    break;

                case REGEX:
                    counter += countRegexHits( pattern, text );
                    break;
            }
        }

        // output our results...
        if( result.isPresent( "quiet" ) )
            out( "" + counter );
        else {
            switch( countType ) {
                case LOC:
                    out( counter + " lines of code" );
                    break;
                case LINES:
                    out( counter + " lines" );
                    break;
                case REGEX:
                    out( counter + " matches to regular expression \"" + regex + "\"" );
                    break;
                case WORDS:
                    if( haveWords )
                        out( counter + " matches to the words " + wordsToString( words ) );
                    else
                        out( counter + " words" );
                    break;
            }
        }
    }


    private static String wordsToString( final List<?> _words ) {
        if( _words.size() == 0 ) return "";
        if( _words.size() == 1 ) return (String) _words.get( 0 );
        Object[] wordsObj = _words.toArray( new Object[0] );
        String[] words = (String[])wordsObj;
        String joined = String.join( ", ", words );
        int index = joined.lastIndexOf( ", " );
        joined = joined.substring( 0, index ) + ", or " + joined.substring( index + 2 );
        return joined;
    }


    private static int countRegexHits( final Pattern _pattern, final String _text ) {

        // count the occurrences of our regex...
        Matcher mat = _pattern.matcher( _text );
        int count = 0;
        while( mat.find() ) {
            count++;
        }
        return count;
    }


    private static final Pattern LOCFinder = Pattern.compile( "^[ \\t]+([a-zA-Z])" );

    private static int countLinesOfCode( final String _text ) {

        // first we split our text into lines...
        String[] lines = _text.split( "\\R" );

        // now we count those lines whose first non-whitespace character is a letter...
        int count = 0;
        for( String line : lines ) {
            Matcher mat = LOCFinder.matcher( line );
            if( mat.find() )
                count++;
        }

        return count;
    }


    private static int countSpecificWords( final String _word, final String _text, final boolean _caseSensitive ) {

        // first we split our text into words...
        String[] words = _text.split( "[ ,;.!?:]+" );

        // now iterate over all those words, counting the matches...
        int count = 0;
        for( String word : words ) {
            if( _caseSensitive ? word.equals( _word ) : word.equalsIgnoreCase( _word ) ) {
                count++;
            }
        }

        return count;
    }


    private static void out( final String _msg ) {
        System.out.println( _msg );
    }
}
