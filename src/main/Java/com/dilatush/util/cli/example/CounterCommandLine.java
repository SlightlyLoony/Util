package com.dilatush.util.cli.example;

import com.dilatush.util.cli.CommandLine;
import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.argdefs.OptArgDef;
import com.dilatush.util.cli.argdefs.OptArgNames;
import com.dilatush.util.cli.argdefs.PosArgDef;
import com.dilatush.util.cli.parsers.EnumerationParser;

/**
 * Helper class to build a command line interpreter for the counter program.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CounterCommandLine {

    /**
     * Returns a new, initialized instance of {@link CommandLine} for the counter program.
     */
    public static CommandLine get( ) {

        // get a fresh, new command line instance...
        CommandLine commandLine = new CommandLine( NAME, SUMMARY, DETAIL, 80, 4 );

        // creating an optional enumeration argument definition directly...
        OptArgDef wordsDef = new OptArgDef(
                "countType",
                "The type of counting to do: WORDS, LINES, LOC, or REGEX.",
                "The type of counting to do.  The parameter defaults to LINES if the argument is absent or if the parameter is missing, and must" +
                        "be one of WORDS, LINES, LOC or REGEX.  The parameter is not case sensitive",
                1,
                "type",
                CountType.class,
                ParameterMode.OPTIONAL,
                "LINES",
                new EnumerationParser( CountType.class, false ),
                null,
                new OptArgNames( "c;count" ),
                "LINES",
                null,
                null
        );
        commandLine.add( wordsDef );

        // creating a binary optional argument definition using an OptArgDef helper method...
        OptArgDef summaryDef = OptArgDef.getSingleBinaryOptArgDef(
                "summary",
                "Displays a summary description of this command.",
                "Displays a summary description of this command, with all the arguments it accepts.",
                new OptArgNames( "?;summary" )
        );
        commandLine.add( summaryDef );

        // creating a multiple readable file positional argument definition using a PosArgDef helper method...
        PosArgDef filesDef = PosArgDef.getMultiReadableFilePosArgDef(
                "files",
                "Files to count things in.",
                "Files to count things in.",
                "files"
        );
        commandLine.add( filesDef );

        // creating some optional argument definitions in (almost) one-liners...
        commandLine.add( new WordsOptArgDef() );
        commandLine.add( OptArgDef.getSingleBinaryOptArgDef( "case", "Make tests case-sensitive.", "Make tests case-sensitive.",
                new OptArgNames( "s;case_sensitive" ) ) );
        commandLine.add( new RegexOptArgDef() );
        commandLine.add( OptArgDef.getSingleBinaryOptArgDef( "detail", "Get detailed help.", "Get detailed help on the counter command",
                new OptArgNames( "h;help" ) ) );

        return commandLine;
    }

    public enum CountType {
        WORDS, LINES, LOC, REGEX
    }


    private static final String NAME = "counter";

    private static final String SUMMARY = "Counter counts things like words, lines, lines of code, and more in text files.";

    private static final String DETAIL = "Counter counts various things in text files.  It can accept any number of globbed files, so it's " +
            "very convenient to use from the command line.  The kinds of things it can count includes: words (all or specific), lines, lines " +
            "of code, files, or a regular expression match.";
}
