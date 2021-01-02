package com.dilatush.util.cli;

import java.net.InetAddress;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CLITest {

    public static void main( final String[] _args ) {

        CLDef clDef = new CLDef( CL_SUMMARY, CL_DETAIL );
        clDef.add( new ArgDef(
                "verbosity",
                "Increase verbosity of output (1..5 times)",
                "Increase verbosity of output.  May be repeated up to five times to get more and more detailed output.",
                5,
                new char[] {'v'},
                null
        ) );
        clDef.add( new ArgDef(
                "host",
                "The host to connect to.",
                "The fully-qualified domain name, or dotted-form IP address, of the host to connect to.",
                ArgumentArity.MANDATORY_SINGLE,
                ParameterAllowed.MANDATORY,
                InteractiveMode.PLAIN,
                "Enter the host: ",
                InetAddress.class,
                null,
                new InetAddressByNameParser(),
                null,
                new char[] { 'h' },
                new String[] { "host" }
        ) );
        clDef.add( new ArgDef(
                "otherhost",
                "The second host to connect to.",
                "The fully-qualified domain name, or dotted-form IP address, of the second host to connect to.",
                ArgumentArity.MANDATORY_SINGLE,
                ParameterAllowed.MANDATORY,
                InteractiveMode.PLAIN,
                "Enter the second host: ",
                InetAddress.class,
                null,
                new InetAddressByNameParser(),
                null
        ) );

        ParsedCLI cli = clDef.parse( new String[] { "-vvv", "--host" , "paradiseweather.info" });

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
