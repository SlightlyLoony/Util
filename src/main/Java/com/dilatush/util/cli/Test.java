package com.dilatush.util.cli;

import com.dilatush.util.AConfig;
import com.dilatush.util.cli.argdefs.*;
import com.dilatush.util.cli.parsers.AConfigParser;
import com.dilatush.util.cli.parsers.PathParser;
import com.dilatush.util.cli.validators.CreatableFileValidator;
import com.dilatush.util.cli.validators.ReadableFileValidator;

import java.io.File;
import java.util.List;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Test {

    public static void main( final String[] _args ) {

        ArgDef fileDef = new MultiplePositionalArgDef(
                "files",
                "The files to count occurrences in.",
                "The text files to count occurrences in.",
                0,                                          // allow any number of files...
                File.class,
                new PathParser(),
                new ReadableFileValidator()
        );

        ArgDef outFileDef = new SinglePositionalArgDef(
                "outFile",
                "The file to write our output in.",
                "The file to write our output in.",
                File.class,
                new PathParser(),
                new CreatableFileValidator()
        );

        ArgDef wordsDef = new MultipleOptionalArgDef(
                "words",
                "Count specified words.",
                "Count occurrences of specified words.  Any number may be specified with separate options.",
                "w",
                "word",
                0,                                          // allow any number of words...
                String.class,
                null,
                null,
                null
        );

        ArgDef configDef = new SingleOptionalArgDef(
                "config",
                "JavaScript configuration file",
                "Specify a JavaScript configuration file.  The default is 'TestCLI.js'.",
                "c", "config configuration",
                TestConfig.class,
                "TestCLI.js",
                "TestCLI.js",
                new AConfigParser( TestConfig.class ),
                null
        );

        ArgDef allWordsDef = new BinaryOptionalArgDef(
                "allWords",
                "Count all words.",
                "Count all words (any strings separated by whitespace).",
                "a",
                "all"
        );

        CommandLine commandLine = new CommandLine( CL_SUMMARY, CL_DETAIL );
        commandLine.add( fileDef         );
        commandLine.add( allWordsDef     );
        commandLine.add( wordsDef        );
        commandLine.add( outFileDef      );
        commandLine.add( configDef       );

        String[] args = new String[] {
                "--all",
                "src/main/Java/com/dilatush/util/cli/CommandLine.java",
                "src/main/Java/com/dilatush/util/cli/InteractiveMode.java",
                "src/main/Java/com/dilatush/util/cli/ParameterMode.java",
                "src/main/Java/com/dilatush/util/cli/ParsedArg.java",
                "src/main/Java/com/dilatush/util/cli/ParsedCommandLine.java",
                "src/main/Java/com/dilatush/util/cli/Test.java",
                "src/main/Java/com/dilatush/util/cli/package-info.java",
                "-w=hamburger",
                "--config=TestCLI2.js",
                "-c",
                "--word=sandwich",
                "test.txt"
        };

        ParsedCommandLine cli = commandLine.parse( args );

        //noinspection ResultOfMethodCallIgnored
        cli.hashCode();

        if( cli.isValid() ) {
            ParsedArg ans = cli.get( "password" );
            System.out.println( (String) cli.get( "password" ).value );
        }
        else {
            System.out.println( cli.getErrorMsg() );
        }
    }


    private final static String CL_SUMMARY = "Count occurrences in files.";

    private final static String CL_DETAIL =
            "Count occurrences of lines, all words, specific words, or regular expression matches in files.";

    /**
     * @author Tom Dilatush  tom@dilatush.com
     */
    public static class TestConfig extends AConfig {

        public int count;
        public String name;


        /**
         * Verify the validity of this object.  Each error found adds an explanatory message to the given list of messages.
         *
         * @param _messages The list of messages explaining the errors found.
         */
        @Override
        public void verify( final List<String> _messages ) {
        }
    }
}
