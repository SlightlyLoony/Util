package com.dilatush.util.cli;

import com.dilatush.util.AConfig;

import java.io.File;
import java.util.List;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Test {

    public static void main( final String[] _args ) {

        ArgDef fileDef = new SinglePositionalArgDef(
                "files",
                "The files to count occurrences in.",
                "The text files to count occurrences in.",
                String.class,
                new TextFileParser(),
                null
        );
        fileDef.absentValue = "TestTest.js";

        CommandLine commandLine = new CommandLine( CL_SUMMARY, CL_DETAIL );
        commandLine.add( fileDef      );

        ParsedCommandLine cli = commandLine.parse( new String[] { "Test.adoc" });

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


    private final static String CL_SUMMARY = "Connect to a console server.";

    private final static String CL_DETAIL =
            "Connect to a console server over the network.  The console server listens on a particular port.  Once the client " +
                    "makes a TCP connection to the server, the two of them establish an encrypted \"tunnel\" that is encrypted " +
                    "with a shared secret (key).  The console server may have multiple console providers, in which case the console " +
                    "client may choose which one to use.";

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
