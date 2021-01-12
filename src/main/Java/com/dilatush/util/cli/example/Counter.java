package com.dilatush.util.cli.example;

import com.dilatush.util.Files;
import com.dilatush.util.cli.CommandLine;
import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.ParsedCommandLine;
import com.dilatush.util.cli.argdefs.OptArgDef;
import com.dilatush.util.cli.argdefs.OptArgNames;
import com.dilatush.util.cli.argdefs.PosArgDef;
import com.dilatush.util.cli.parsers.BooleanParser;

import java.io.File;
import java.util.List;

import static com.dilatush.util.General.isNotNull;

/**
 * Command line application that provides an example of using the {@link CommandLine} argument interpreter.  This application accepts one or more
 * file paths and performs various counting operations on them, as specified by various optional arguments.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Counter {


    public static void main( final String[] _args ) {

        // creating an optional argument definition directly...
        OptArgDef wordsDef = new OptArgDef(
                "words",
                "Count the words in the specified files.",
                "Count all the words (defined as whitespace-separated strings of non-whitespace characters) in the specified files.",
                1,
                null,
                Boolean.class,
                ParameterMode.DISALLOWED,
                null,
                new BooleanParser(),
                null,
                new OptArgNames( "w;words" ),
                "false",
                null,
                null
        );

        // creating a binary optional argument definition using an OptArgDef helper method...
        OptArgDef summaryDef = OptArgDef.getSingleBinaryOptArgDef(
                "summary",
                "Displays a summary description of this command.",
                "Displays a summary description of this command, with all the arguments it accepts.",
                new OptArgNames( "?;summary" )
        );

        // creating a multiple readable file positional argument definition using a PosArgDef helper method...
        PosArgDef filesDef = PosArgDef.getMultiReadableFilePosArgDef(
                "files",
                "Files to count things in.",
                "Files to count things in.",
                "files"
        );

        // creating a binary optional argument definition using a OptArgDef subclass...
        OptArgDef linesDef = new LinesOptArgDef();


        CommandLine commandLine = new CommandLine( "counter", "Counts things in files.", "Counts lots of different things in files.", 80, 4 );
        commandLine.add( wordsDef );
        commandLine.add( summaryDef );
        commandLine.add( filesDef );
        commandLine.add( linesDef );

        ParsedCommandLine result = commandLine.parse( _args );

        if( !result.isValid() ) {
            out( "Invalid command line" );
            out( result.getErrorMsg() );
            out( commandLine.getSummaryHelp() );
            System.exit( 1 );
        }

        if( (Boolean) result.getValue( "summary" ) ) {
            out( commandLine.getSummaryHelp() );
            System.exit( 1 );
        }

        // figure out what we're going to count
        What count = null;
        if( (Boolean) result.getValue( "words" ) ) {
            if( isNotNull( count ) ) {
                out( "Multiple types of count specified" );
                System.exit( 1 );
            }
            count = What.WORDS;
        }
        if( (Boolean) result.getValue( "lines" ) ) {
            if( isNotNull( count ) ) {
                out( "Multiple types of count specified" );
                System.exit( 1 );
            }
            count = What.LINES;
        }

        // if we didn't specify what to count, barf...
        if( count == null ) {
            out( "No type of counting was specified!" );
            System.exit( 1 );
        }

        // initialize our counter...
        int counter = 0;

        // iterate over all the specified files...
        for( Object obj  : (List<?>) result.getValues( "files" ) ) {

            // get our file's text...
            File file = (File) obj;
            String text = Files.readToString( file );

            // do the right kind of counting...
            switch( count ) {

                case WORDS:
                    counter += (text == null) ? 0 : text.split( "\\s+" ).length;
                    break;

                case LINES:
                    counter += (text == null) ? 0 : text.split( "\\R" ).length;
                    break;

                default:
                    break;
            }
        }

        // output our results...
        out( "Count: " + counter );

        commandLine.hashCode();
    }


    private enum What {
        WORDS,LINES
    }


    public static void out( final String _msg ) {
        System.out.println( _msg );
    }
}
