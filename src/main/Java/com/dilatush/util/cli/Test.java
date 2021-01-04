package com.dilatush.util.cli;

import com.dilatush.util.AConfig;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Test {

    public static void main( final String[] _args ) {

        ArgDef countDef = new BinaryOptionalArgDef(
                "count",
                "Output the count of occurrences.",
                "Instead of outputting the text of the occurrences, output the count of the number of occurrences.",
                "c", "count" );

        ArgDef intDef = new SingleOptionalArgDef(
                "burgers",
                "How many hamburgers are needed.",
                "The quantity of hamburgers desired by the whoever is going to eat them.",
                "b", "burgers",
                Integer.class,
                0,
                new IntegerParser(),
                new IntegerValidator( 1, 15 ) );

        ArgDef envDef = new SingleOptionalArgDef(
                "environment",
                "Testing environmental variables as source.",
                "Still testing environmental variables as sources.",
                "e", "environ",
                String.class,
                "uh oh",
                null,
                null );
        envDef.environVariable = "TDTD";

        ArgDef verbosityDef = new BinaryOptionalArgDef(
                "verbosity",
                "Increase verbosity of output (1..5 times)",
                "Increase verbosity of output.  May be repeated up to five times to get more and more detailed output.",
                "v", "verbosity" );
        verbosityDef.maxAllowed = 5;

        ArgDef hostDef = new SingleOptionalArgDef(
                "host",
                "The host to connect to.",
                "The fully-qualified domain name, or dotted-form IP address, of the host to connect to.",
                "h", "host",
                InetAddress.class,
                Inet4Address.getLoopbackAddress(),
                new InetAddressByNameParser(),
                null
        );

        ArgDef fileDef = new SinglePositionalArgDef(
                "config_file",
                "The path to the configuration file.",
                "The path (with file name) for the configuration file.",
                String.class,
                "",
                new TextFileParser(),
                null
        );
        fileDef.setInteractiveMode( "Enter the configuration file path" );

        ArgDef configDef = new SinglePositionalArgDef(
                "js_config",
                "JavaScript configuration file name",
                "JavaScript configuration file path",
                TestConfig.class,
                new TestConfig(),
                new JSConfigParser( new TestConfig() ),
                null
        );

        CommandLine commandLine = new CommandLine( CL_SUMMARY, CL_DETAIL );
        commandLine.add( countDef     );
        commandLine.add( intDef       );
        commandLine.add( envDef       );
        commandLine.add( verbosityDef );
        commandLine.add( hostDef      );
        commandLine.add( fileDef      );
        commandLine.add( configDef    );

        ParsedCommandLine cli = commandLine.parse( new String[] { "-vvv", "-h=foxnews.com", "TestTest.js", "TestJavaScriptParser.js", "--burgers", "14", "-e" });

        //noinspection ResultOfMethodCallIgnored
        cli.hashCode();
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
         * Implemented by subclasses to verify that their fields are valid.  When possible, this should be accomplished by a series of invocations of
         * {@link #validate(Validator, String)}, one or more times for each field in the configuration.
         */
        @Override
        protected void verify() {
        }
    }
}
