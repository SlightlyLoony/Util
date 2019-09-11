package com.dilatush.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.util.Streams.toUTF8String;
import static com.dilatush.util.Strings.isEmpty;
import static java.lang.ProcessBuilder.Redirect;

/**
 * Configure and execute an SSH command.  This class provides methods to configure the SSH command using either individual command line tokens
 * ({@link #addToken(String)}) or bash-style command line fragments ({@link #addFragment(String)}).  In addition, this class provides numerous
 * convenience methods for configuring particular SSH options.
 * <p>Note that there are two rather different ways of executing commands on the remote computer:</p>
 * <ol>
 *     <li>Direct host execution (no login shell): Generally this is the simplest and most useful way to execute remotely.  The specified command is
 *     executed; when it completes, SSH then immediately terminates the connection.  Multiple commands can be separated by either newlines or
 *     semicolons.  The exit code of the last program executed will be the exit code from SSH.  The safest way to wait for the command to complete
 *     is to use {@link #waitFor(long, TimeUnit)}, eliminating the possibility of a hang.</li>
 *     <li>Interactive execution (with login shell): This is possible, but complicated by several things - most especially the fact that you must find
 *     a way to determine that the remote shell is at a login prompt so you can know when to send input to it.  When you are finished using an
 *     interactive instance, be certain to execute the <code>exit</code> command on the remote shell, so that SSH will terminate normally (as opposed
 *     to invoking {@link #destroyForcibly()}.</li>
 * </ol>
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SSHExecutor {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    final static private String SSH                    = "/usr/bin/ssh";   // path to the ssh executable...
    final static private String FORCE_TERMINAL_AND_TTY = "-tt";

    private final List<String> elements;
    private final boolean      interactive;

    private Process process;
    private String  command;
    private String  host;


    /**
     * Creates a new instance of this class that will connect to the specified host.  If interactive is specified as <code>false</code>, then the
     * instance will be in direct host execution mode (no login shell will be used, and commands cannot be executed interactively).  If interactive is
     * specified as <code>true</code>, then the instance will be in interactive mode and no direct host execution command may be specified.
     *
     * @param _host the host name (or dotted-form IP) of the host to connect to
     * @param _interactive if <code>true</code>, specifies interactive mode
     */
    public SSHExecutor( final String _host, final boolean _interactive ) {

        // sanity check...
        if( isEmpty( _host ) )
            throw new IllegalArgumentException( "No host specified" );

        host = _host;
        interactive = _interactive;
        elements = new ArrayList<>();
        elements.add( SSH );
        if( interactive )
            elements.add( FORCE_TERMINAL_AND_TTY );
    }


    /**
     * Creates a new instance of this class that will connect to the specified host.  The instance will be in direct host execution mode: no login
     * shell will be used, and commands cannot be executed interactively. This is exactly the same as {@link #SSHExecutor(String, boolean)} with a
     * <code>false</code> interactive argument.
     */
    public SSHExecutor( final String _host ) {
        this( _host, false );
    }


    /**
     * Sets the command to be directly executed on the remote host (in direct host command execution mode).  Note that multiple commands may be
     * included if separated by semicolons.  The command should be in usual format, including quotes as needed.  However, do <i>not</i> surround the
     * entire command with quotes.  For example, the strings <i>ls -l</i> and <i>cp "My File" /tmp/a.txt</i> are correctly formatted, but the string
     * <i>"ls -l"</i> is not.
     *
     * @param _command the command to be directly executed on the remote host
     * @throws IllegalStateException if instance is in interactive mode
     */
    public void setCommand( final String _command ) {

        // mode check...
        if( interactive )
            throw new IllegalStateException( "Direct host execution command may not be set in interactive mode" );

        command = _command;
    }


    public void setForceIPv4() {
        elements.add( "-4" );
    }


    /**
     * Adds the given token to this instance.  These correspond exactly to the tokens presented in the arguments array to a process that's starting.
     *
     * @param _token the token to add.
     */
    private void addToken( final String _token ) {

        // sanity check...
        if( isEmpty( _token ) )
            throw new IllegalArgumentException( "Attempt to add a null or empty token" );

        elements.add( _token );
    }


    /**
     * Adds the given command line fragment to this instance, which will add one or more tokens.  For example, adding the fragment
     * "-o \"ServerAliveCountMax 2\"" is exactly the same as adding the two tokens "-o" and "ServerAliveCountMax 2" with {@link #addToken(String)}.
     * The given fragment is unquoted with {@link Bash#unquote(String)}.
     *
     * @param _fragment the command line fragment to add.
     */
    private void addFragment( final String _fragment ) {

        // sanity check...
        if( isEmpty( _fragment ) )
            throw new IllegalArgumentException( "Attempt to add a null or empty fragment" );

        elements.addAll( Bash.unquote( _fragment ) );
    }


    /**
     * Start the SSH process as currently configured.
     *
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if attempting to start a process that is already running
     * @throws SecurityException as thrown by {@link ProcessBuilder#start()}
     */
    public void start() throws IOException {

        // sanity check...
        if( (process != null) && process.isAlive() )
            throw new IllegalStateException( "Process is already running" );

        // add the host...
        elements.add( host );

        // if we're in direct mode, add the command...
        if( !interactive )
            elements.add( command );

        ProcessBuilder builder = new ProcessBuilder( elements );
        process = builder.start();
    }


    /**
     * Returns all of the process output since the process was started, or since the most recent invocation of this method.  Note that there is a
     * small chance of a decoding error when invoking this method.  If this method is called when only <i>some</i> of the bytes of a multibyte UTF-8
     * character have been read, then the last character read will be decoded incorrectly.
     *
     * @return the process output
     * @throws IOException on any I/O error
     */
    public String getSSHOutput() throws IOException {
        return toUTF8String( process.getInputStream() );
    }


    /**
     * Returns all of the process error output since the process was started, or since the most recent invocation of this method.  Note that there is
     * a small chance of a decoding error when invoking this method.  If this method is called when only <i>some</i> of the bytes of a multibyte UTF-8
     * character have been read, then the last character read will be decoded incorrectly.
     *
     * @return the process output
     * @throws IOException on any I/O error
     */
    public String getSSHErrorOutput() throws IOException {
        return toUTF8String( process.getErrorStream() );
    }


    /**
     * Write the specified string to the SSH input, encoding it as UTF-8.
     *
     * @param _input the string to input to SSH
     * @throws IOException on any I/O error
     * @throws IllegalStateException if this instance is in direct host command execution mode
     */
    public void putSSHInput( final String _input ) throws IOException {

        // mode check...
        if( !interactive )
            throw new IllegalStateException( "Can't send SSH input when in direct host command execution mode" );

        process.getOutputStream().write( _input.getBytes( StandardCharsets.UTF_8 ) );
        process.getOutputStream().flush();
    }


    /**
     * Returns the output stream connected to the normal input of the
     * subprocess.  Output to the stream is piped into the standard
     * input of the process represented by this {@code Process} object.
     *
     * <p>If the standard input of the subprocess has been redirected using
     * {@link ProcessBuilder#redirectInput(Redirect)}
     * ProcessBuilder.redirectInput}
     * then this method will return a
     * <a href="ProcessBuilder.html#redirect-input">null output stream</a>.
     *
     * <p>Implementation note: It is a good idea for the returned
     * output stream to be buffered.
     *
     * @return the output stream connected to the normal input of the subprocess
     * @throws IllegalStateException if this instance is in direct host command execution mode
     */
    public OutputStream getOutputStream() {

        // mode check...
        if( !interactive )
            throw new IllegalStateException( "Can't use output stream when in direct host command execution mode" );

        return process.getOutputStream();
    }


    /**
     * Returns the input stream connected to the normal output of the
     * subprocess.  The stream obtains data piped from the standard
     * output of the process represented by this {@code Process} object.
     *
     * <p>If the standard output of the subprocess has been redirected using
     * {@link ProcessBuilder#redirectOutput(Redirect) ProcessBuilder.redirectOutput}
     * then this method will return a
     * <a href="ProcessBuilder.html#redirect-output">null input stream</a>.
     *
     * <p>Otherwise, if the standard error of the subprocess has been
     * redirected using
     * {@link ProcessBuilder#redirectErrorStream(boolean)
     * ProcessBuilder.redirectErrorStream}
     * then the input stream returned by this method will receive the
     * merged standard output and the standard error of the subprocess.
     *
     * <p>Implementation note: It is a good idea for the returned
     * input stream to be buffered.
     *
     * @return the input stream connected to the normal output of the
     *         subprocess
     */
    public InputStream getInputStream() {
        return process.getInputStream();
    }


    /**
     * Returns the input stream connected to the error output of the
     * subprocess.  The stream obtains data piped from the error output
     * of the process represented by this {@code Process} object.
     *
     * <p>If the standard error of the subprocess has been redirected using
     * {@link ProcessBuilder#redirectError(Redirect)
     * ProcessBuilder.redirectError} or
     * {@link ProcessBuilder#redirectErrorStream(boolean)
     * ProcessBuilder.redirectErrorStream}
     * then this method will return a
     * <a href="ProcessBuilder.html#redirect-output">null input stream</a>.
     *
     * <p>Implementation note: It is a good idea for the returned
     * input stream to be buffered.
     *
     * @return the input stream connected to the error output of
     *         the subprocess
     */
    public InputStream getErrorStream() {
        return process.getErrorStream();
    }


    /**
     * Causes the current thread to wait, if necessary, until the
     * process represented by this {@code Process} object has
     * terminated.  This method returns immediately if the subprocess
     * has already terminated.  If the subprocess has not yet
     * terminated, the calling thread will be blocked until the
     * subprocess exits.
     *
     * @return the exit value of the subprocess represented by this
     *         {@code Process} object.  By convention, the value
     *         {@code 0} indicates normal termination.
     * @throws InterruptedException if the current thread is
     *         {@linkplain Thread#interrupt() interrupted} by another
     *         thread while it is waiting, then the wait is ended and
     *         an {@link InterruptedException} is thrown.
     */
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }


    /**
     * Causes the current thread to wait, if necessary, until the
     * subprocess represented by this {@code Process} object has
     * terminated, or the specified waiting time elapses.
     *
     * <p>If the subprocess has already terminated then this method returns
     * immediately with the value {@code true}.  If the process has not
     * terminated and the timeout value is less than, or equal to, zero, then
     * this method returns immediately with the value {@code false}.
     *
     * <p>The default implementation of this methods polls the {@code exitValue}
     * to check if the process has terminated. Concrete implementations of this
     * class are strongly encouraged to override this method with a more
     * efficient implementation.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the subprocess has exited and {@code false} if
     *         the waiting time elapsed before the subprocess has exited.
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting.
     * @throws NullPointerException if unit is null
     * @since 1.8
     */
    public boolean waitFor( final long timeout, final TimeUnit unit ) throws InterruptedException {
        return process.waitFor( timeout, unit );
    }


    /**
     * Returns the exit value for the subprocess.
     *
     * @return the exit value of the subprocess represented by this
     *         {@code Process} object.  By convention, the value
     *         {@code 0} indicates normal termination.
     * @throws IllegalThreadStateException if the subprocess represented
     *         by this {@code Process} object has not yet terminated
     */
    public int exitValue() {
        return process.exitValue();
    }


    /**
     * Kills the subprocess. Whether the subprocess represented by this
     * {@code Process} object is forcibly terminated or not is
     * implementation dependent.
     */
    public void destroy() {
        process.destroy();
    }


    /**
     * Kills the subprocess. The subprocess represented by this
     * {@code Process} object is forcibly terminated.
     *
     * <p>The default implementation of this method invokes {@link #destroy}
     * and so may not forcibly terminate the process. Concrete implementations
     * of this class are strongly encouraged to override this method with a
     * compliant implementation.  Invoking this method on {@code Process}
     * objects returned by {@link ProcessBuilder#start} and
     * {@link Runtime#exec} will forcibly terminate the process.
     *
     * <p>Note: The subprocess may not terminate immediately.
     * i.e. {@code isAlive()} may return true for a brief period
     * after {@code destroyForcibly()} is called. This method
     * may be chained to {@code waitFor()} if needed.
     *
     * @return the {@code Process} object representing the
     *         subprocess to be forcibly destroyed.
     * @since 1.8
     */
    public Process destroyForcibly() {
        return process.destroyForcibly();
    }


    /**
     * Tests whether the subprocess represented by this {@code Process} is
     * alive.
     *
     * @return {@code true} if the subprocess represented by this
     *         {@code Process} object has not yet terminated.
     * @since 1.8
     */
    public boolean isAlive() {
        return process.isAlive();
    }


    /**
     * Returns the exit code from the command just executed.
     *
     * @return the exit code.
     * @throws IllegalThreadStateException if the SSH process has not yet terminated
     */
    public int getExitCode() {
        return process.exitValue();
    }


    public static void main( String[] _args ) throws IOException, InterruptedException {

        SSHExecutor ssh = new SSHExecutor( "beast" );
        ssh.setCommand( "ls -l ; ls" );
        ssh.setForceIPv4();
        ssh.start();
        ssh.waitFor();
        String output = ssh.getSSHOutput();
        ssh.hashCode();
    }
}