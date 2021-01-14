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

        // get our command line interpreter...
        CommandLine commandLine = CounterCommandLine.get();

        // parse our argument...
        ParsedCommandLine result = commandLine.parse( _args );

        if( !result.isValid() )
            handleInvalid( commandLine, result );

        if( (Boolean) result.getValue( "summary" ) )
            handleSummary( commandLine );

        if( (Boolean) result.getValue( "detail" ) )
            handleDetail( commandLine );

        // get the kind of count we're going to make...
        CountType countType = (CountType) result.getValue( "countType" );

        // get any specified words..
        // noinspection unchecked
        List<String> words = (List<String>) result.getValues( "word" );
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
        // noinspection unchecked
        for( File file  : (List<File>) result.getValues( "files" ) ) {

            // get our file's text...
            String text = Files.readToString( file );
            if( text == null ) continue;

            // do the right kind of counting...
            switch( countType ) {

                case WORDS:
                    if( !haveWords )
                        counter += text.split( "\\s+" ).length;
                    else
                        for( String word : words ) { counter += countSpecificWords( word, text, caseSensitive ); }
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
        out( "Count: " + counter );
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


    private static void handleSummary( final CommandLine _commandLine ) {
        out( _commandLine.getSummaryHelp() );
        System.exit( 0 );
    }


    private static void handleDetail( final CommandLine _commandLine ) {
        out( _commandLine.getDetailedHelp() );
        System.exit( 0 );
    }


    private static void handleInvalid( final CommandLine commandLine, final ParsedCommandLine result ) {
        out( "Invalid command line" );
        out( result.getErrorMsg() );
        out( commandLine.getSummaryHelp() );
        System.exit( 1 );
    }


    private static void out( final String _msg ) {
        System.out.println( _msg );
    }
}
