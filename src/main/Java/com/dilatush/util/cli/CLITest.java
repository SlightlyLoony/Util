package com.dilatush.util.cli;

import java.io.File;
import java.net.InetAddress;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CLITest {

    public static void main( final String[] _args ) {

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
                new InetAddressByNameParser(),
                new ObjectPresentValidator( InetAddress.class )
        );

        ArgDef fileDef = new SinglePositionalArgDef(
                "config_file",
                "The path to the configuration file.",
                "The path (with file name) for the configuration file.",
                File.class,
                new PathParser(),
                new ReadableFileValidator()
        );
        fileDef.setInteractiveMode( "Enter the configuration file path" );

        CommandLine commandLine = new CommandLine( CL_SUMMARY, CL_DETAIL );
        commandLine.add( verbosityDef );
        commandLine.add( hostDef      );
        commandLine.add( fileDef      );

        ParsedCLI cli = commandLine.parse( new String[] { "-vvv", "-h=foxnews.com", "TestTest.js" });

        //noinspection ResultOfMethodCallIgnored
        cli.hashCode();
    }


    private final static String CL_SUMMARY = "Connect to a console server.";

    private final static String CL_DETAIL =
            "Connect to a console server over the network.  The console server listens on a particular port.  Once the client " +
                    "makes a TCP connection to the server, the two of them establish an encrypted \"tunnel\" that is encrypted " +
                    "with a shared secret (key).  The console server may have multiple console providers, in which case the console " +
                    "client may choose which one to use.";
}
