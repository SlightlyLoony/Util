package com.dilatush.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.isNull;

/**
 * Instances of this class execute a command in the local command shell and return the results.  The command to be executed must be a command that
 * terminates the process itself.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Executor {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final List<String> elements;

    private int exitCode;


    /**
     * Creates an instance to run the given string command, which would be formatted exactly as you would run it from a Bash command line, including
     * backslashes, single quotes, and double quotes.  Note that variable names ("$xxx") and globs ("*") are <i>not</i> expanded, but dollar signs
     * <i>are</i> escaped.
     *
     * @param _command the command to execute
     */
    public Executor( final String _command ) {
        elements = Bash.unquote( _command );
    }


    /**
     * Creates an instance to run the given list of command and arguments.  Note that these must be in the same format as seen by the main method
     * of a program: unquoted.
     *
     * @param _elements the list of command and arguments.
     */
    public Executor( final List<String> _elements ) {
        elements = _elements;
    }


    /**
     * Runs this executor, returning the result (the output of the command being run) as a string.  If an error occurs, returns <code>null</code>.
     * The error stream is redirected to stdout, and is returned as part of the result.  The exit code is captured and can be read.  This method
     * will block until the command has finished executing.
     *
     * @return the result of running this executor, or <code>null</code> if there was an error.
     */
    public String run() {

        try {
            ProcessBuilder pb = new ProcessBuilder( elements );
            pb.redirectErrorStream( true );
            Process p = pb.start();
            BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream(), StandardCharsets.US_ASCII ) );
            StringBuilder sb = new StringBuilder();
            while( true ) {
                String line = br.readLine();
                if( isNull( line ) ) break;
                sb.append( line );
                sb.append( System.lineSeparator() );
            }
            p.waitFor();
            exitCode = p.exitValue();
            return sb.toString();
        }
        catch( InterruptedException _e ) {
           exitCode = 255;
           return null;
        }
        catch( IOException _e ) {
            LOGGER.log( Level.SEVERE, "Problem executing command", _e );
            return null;
        }
    }


    /**
     * Returns the exit code from the command just executed.
     *
     * @return the exit code.
     */
    public int getExitCode() {
        return exitCode;
    }
}
