package com.dilatush.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.getLogger;
import static java.util.Objects.isNull;

/**
 * Instances of this class execute a command in a fresh copy of the bash shell, and return the results.  The command to be executed must be a command (or semicolon
 * separated commands and pipes) that terminates the process itself.  Note that this class is intended to supersede the {@link Executor} class, which executes commands
 * outside a shell.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class BashExecutor {

    final static private Logger LOGGER = getLogger();

    private int exitCode;

    private final String script;


    /**
     * Creates a new instance of this class, ready to execute the given script inside an instance of {@code bash}.  Note that the given script is automagically prefixed
     * with {@code set -o pipefail; } so that in the case of a pipe, any non-zero exit code is returned.
     *
     * @param _script The script to execute inside of {@code bash}.
     */
    public BashExecutor( final String _script ) {
        script = "set -o pipefail; " + _script;
    }


    /**
     * Runs this executor, returning the result (the output of the script being run) as a string.  If an error occurs, returns <code>null</code>.
     * The error stream is redirected to stdout, and is returned as part of the result.  The exit code is captured and can be read.  This method
     * will block until the command has finished executing.
     *
     * @return the result of running this executor, or <code>null</code> if there was an error.
     */
    @SuppressWarnings( "DuplicatedCode" )
    public String run() {

        try {
            ProcessBuilder pb = new ProcessBuilder( "bash", "-c", script );
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
