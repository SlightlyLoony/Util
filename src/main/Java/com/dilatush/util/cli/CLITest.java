package com.dilatush.util.cli;

import java.io.File;
import java.util.HashMap;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CLITest {

    public static void main( final String[] _args ) {

        ParsedCLI parsedCLI = new ParsedCLI( new HashMap<>() );
        if( parsedCLI.isValid() ) {
            File configFile = (File) parsedCLI.getValue( "configFile" );
        }
    }
}
