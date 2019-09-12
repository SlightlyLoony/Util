package com.dilatush.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.SSHClientOptions.*;
import static com.dilatush.util.SSHClientOptions.ServerAliveCountMax;
import static com.dilatush.util.Streams.toUTF8String;
import static com.dilatush.util.Strings.isEmpty;
import static java.lang.ProcessBuilder.Redirect;

/**
 * Configure and execute an SSH command.  This class provides methods to configure the SSH command using either individual command line tokens
 * ({@link #addToken(String)}) or bash-style command line fragments ({@link #addFragment(String)}).  In addition, this class provides numerous
 * convenience methods for configuring particular SSH options.
 * <p>Note that there are three rather different ways of using instances of this class to communicate with the remote computer:</p>
 * <ol>
 *     <li>Direct host execution (no login shell): Generally this is the simplest and most useful way to execute commands remotely.  The specified
 *     command is executed; when it completes, SSH then immediately terminates the connection.  Multiple commands can be separated by either newlines
 *     or semicolons.  The exit code of the last program executed will be the exit code from SSH.  The safest way to wait for the command to complete
 *     is to use {@link #waitFor(long, TimeUnit)}, eliminating the possibility of a hang.  To use this method, get an instance of this class
 *     via {@link #SSHExecutor(String, String)}.</li>
 *     <li>Interactive execution (with login shell): This is possible, but complicated by several things - most especially the fact that you must find
 *     a way to determine that the remote shell is at a login prompt so you can know when to send input to it.  When you are finished using an
 *     interactive instance, be certain to execute the <code>exit</code> command on the remote shell, so that SSH will terminate normally (as opposed
 *     to invoking {@link #destroyForcibly()}.  To use this method, get an instance of this class via {@link #SSHExecutor(String)}.</li>
 *     <li>Port forwarding: This establishes an SSH connection that is used <i>only</i> for forwarding ports (i.e., a "tunnel").  In this mode it
 *     is not possible to execute any commands on the remote host.  To use this method, get an instance of this class via
 *     {@link #SSHExecutor(String, int, int)}.</li>
 * </ol>
 * <p>The SSH user <i>must</i> be authenticated via private key in order to use this class.  There is no provision for handling password
 * authentication.</p>
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SSHExecutor {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    final static private String DEFAULT_SSH_PATH       = "/usr/bin/ssh";   // default path to the ssh executable...

    private final List<String> elements;
    private final boolean      interactive;
    private final boolean      forwarding;

    private Process process;
    private String  command;
    private String  host;

    private boolean onlyIPv4;
    private boolean onlyIPv6;
    private boolean compression;
    private boolean user;
    private boolean quiet;
    private int     verbose;


    /**
     * Creates a new instance of this class that will connect to the specified host and directly execute the specified command without using a login
     * shell.  Note that multiple commands may be included if separated by semicolons.  The command should be in usual format, including quotes as
     * needed.  Because no login shell is used, paths may not be present and it's safest to provide the path to executables.  Also, note that there
     * will be no expansions, including globbing.  However, the command <i>may</i> run a script, which <i>will</i> run in a shell, and will have all
     * the usual shell features available.  The SSH session will terminate immediately upon the commands completing, and the exit code will be the
     * exit code of the last command run.  If there was an SSH error of some kind, the exit code will be 255.
     *
     * @param _host the host name (or dotted-form IP) of the host to connect to
     * @param _command the command to be directly executed on the remote host
     */
    public SSHExecutor( final String _host, final String _command ) {

        // sanity check...
        if( isEmpty( _host ) )
            throw new IllegalArgumentException( "No host specified" );
        if( isEmpty( _command ) )
            throw new IllegalArgumentException( "No command specified" );

        host        = _host;
        command     = _command;
        interactive = false;
        forwarding  = false;
        elements    = new ArrayList<>();
        elements.add( DEFAULT_SSH_PATH );
    }


    /**
     * Creates a new instance of this class that will connect to the specified host and enters interactive mode, meaning that commands may be sent to
     * the remote host (via {@link #sendRemoteInput(String)}), and results read from the remote host (via {@link #getRemoteOutput()}.  Note that in
     * interactive sessions, the commands sent to the host are echoed to the results (along with any command line prompts).
     *
     * @param _host the host name (or dotted-form IP) of the host to connect to
     */
    public SSHExecutor( final String _host ) {

        // sanity check...
        if( isEmpty( _host ) )
            throw new IllegalArgumentException( "No host specified" );

        host        = _host;
        interactive = true;
        forwarding  = false;
        elements    = new ArrayList<>();
        elements.add( DEFAULT_SSH_PATH );
        elements.add( "-tt" );  // force allocation of a pseudo-terminal and tty...
    }


    /**
     * Creates an instance of this class that will connect to the specified host in direct host execution mode, but configured for port forwarding
     * (local or remote).  The specified alive interval controls how often (in seconds) the SSH client will check to see if the SSH server (on the
     * remote host) is still alive, and the specified alive count max controls how many times one of those checks can fail before the SSH client will
     * disconnect and exit with an error.  The product of these to values is approximately how long the SSH connection can be down before the SSH
     * client will disconnect.
     *
     * @param _host the host name (or dotted-form IP) of the host to connect to
     * @param _serverAliveInterval interval between SSH server checks
     * @param _serverAliveCountMax how many checks can fail before disconnecting
     */
    public SSHExecutor( final String _host, final int _serverAliveInterval, final int _serverAliveCountMax ) {

        // sanity check...
        if( isEmpty( _host ) )
            throw new IllegalArgumentException( "No host specified" );

        host        = _host;
        interactive = false;
        forwarding  = true;
        elements    = new ArrayList<>();
        elements.add( DEFAULT_SSH_PATH );
        elements.add( "-nNT" );  // don't execute any remote commands, prevent reading from stdin, disable pseudo-terminal allocation...
        setOption( ServerAliveCountMax, Integer.toString( _serverAliveCountMax ) );
        setOption( ServerAliveInterval, Integer.toString( _serverAliveInterval ) );
    }


    public void setOption( final SSHClientOptions _option, final String _optionValue ) {

        // sanity check...
        if( isNull( _option ) )
            throw new IllegalArgumentException( "No option specified" );
        if( isEmpty( _optionValue ) )
            throw new IllegalArgumentException( "No option value specified" );

        elements.add( "-o" );
        elements.add( _option + " " + _optionValue );
    }


    /**
     * By default, this class uses <code>/usr/bin/ssh</code> as the path for SSH on the remote host.  This method allows overriding that default to
     * set the path to anything desired.
     *
     * @param _sshPath the path to the <code>ssh</code> executable (on the remote host)
     */
    public void setSSHPath( final String _sshPath ) {

        // sanity check...
        if( isEmpty( _sshPath ) )
            throw new IllegalArgumentException( "No SSH path specified" );

        elements.set( 0, _sshPath );
    }


    /**
     * Tells this instance to use only IPv4 as a transport.
     */
    public void setOnlyIPv4() {
        if( onlyIPv6 )
            throw new IllegalStateException( "Cannot use only IPv4 if already set to use IPv6 only" );
        if( onlyIPv4 )
            return;
        elements.add( "-4" );
        onlyIPv4 = true;
    }


    /**
     * Tells this instance to use only IPv6 as a transport.
     */
    public void setOnlyIPv6() {
        if( onlyIPv4 )
            throw new IllegalStateException( "Cannot use only IPv6 if already set to use IPv4 only" );
        if( onlyIPv6 )
            return;
        elements.add( "-6" );
        onlyIPv6 = true;
    }


    /**
     * Tells this instance to compress all data, in both directions, on the SSH connection.  Generally this only makes sense for very slow
     * connections, such as acoustic modems.
     */
    public void setCompression() {
        if( compression )
            return;
        elements.add( "-C" );
        compression = true;
    }


    /**
     * Sets a path to an identity file to use when authenticating the SSH connection made by this instance.  Multiple identity files may be specified.
     * Each needs to be a complete path to the identity file.
     *
     * @param _identityFilePath path to an identity file to use when authenticating the SSH connection made by this instance
     */
    public void setIdentityFilePath( final String _identityFilePath ) {

        if( isEmpty( _identityFilePath ) )
            throw new IllegalArgumentException( "No identity file path specified" );

        elements.add( "-i" );
        elements.add( _identityFilePath );
    }


    /**
     * Sets the user for this instance to use when connecting to the remote host.  If no user is specified, then SSH will attempt to connect to the
     * remote host using the same local user that this SSH instance is running as.
     *
     * @param _user the user for this instance to use when connecting to the remote host
     */
    public void setUser( final String _user ) {

        if( user )
            throw new IllegalStateException( "Attempted to set user that was already set" );
        if( isEmpty( _user ) )
            throw new IllegalArgumentException( "No user specified" );

        elements.add( "-l" );
        elements.add( _user );
        user = true;
    }


    /**
     * Tells this instance to operate in quiet mode, minimizing SSH's messages.
     */
    public void setQuiet() {
        if( quiet )
            return;
        elements.add( "-q" );
        quiet = true;
    }


    /**
     * Tells this instance to output messages about it's progress.  Valid levels are 1..3, with higher levels giving more detailed output.  The output
     * is sent to the error output.
     *
     * @param _level the level of verbosity
     */
    public void setVerbose( final int _level ) {
        if( verbose > 0 )
            throw new IllegalStateException( "Attempted to set verbose that was already set" );
        if( (_level < 1) || (_level > 3) )
            throw new IllegalArgumentException( "Invalid verboseness level: " + _level );

        switch( _level ) {
            case 1: elements.add( "-v"   ); break;
            case 2: elements.add( "-vv"  ); break;
            case 3: elements.add( "-vvv" ); break;
        }
        verbose = _level;
    }


    /**
     * Tells this instance to forward connections to a local TCP port to the remote host, and from there forward them to a TCP port on another host.
     * This is useful when a server is accessible from the remote host, but not the local host.  Once this port forwarding is set up, processes on the
     * local host, or other hosts with network access to the local host, can access the server normally only accessible from the remote host.  See
     * <a href="https://en.wikipedia.org/wiki/Port_forwarding" target="_top">Wikipedia</a> for more information.
     * <p>The local binding address can be the IP address of a particular interface to bind only to that interface, "*" to bind to all interfaces,
     * or "localhost" to bind only to the localhost address (127.0.0.1) for local use only.  Note that the SSH client's <b>GatewayPorts</b> setting
     * may have to be changed to allow this to work.</p>
     * <p>The local port address specifies the TCP port that will be listening on the local host.</p>
     * <p>The remote host specifies the host name or IP address of the remote server that connections will be forwarded to.</p>
     * <p>The remote port specifies the TCP port on the remote host that connections will be forwarded to.</p>
     *
     * @param _localBindAddress the IP address to bind to on the local host
     * @param _localPort the TCP port on the local host to listen on
     * @param _remoteHost the host name or IP address of the remote server to forward connections to
     * @param _remotePort the TCP port on the remote server to forward connections to
     */
    public void setLocalPortForwarding( final String _localBindAddress, final int _localPort, final String _remoteHost, final int _remotePort ) {

        // sanity checks...
        if( !forwarding )
            throw new IllegalStateException( "Attempted to set local port forwarding when instance is not in forwarding mode" );
        if( isEmpty( _localBindAddress ) )
            throw new IllegalArgumentException( "No local bind address specified" );
        if( isEmpty( _remoteHost ) )
            throw new IllegalArgumentException( "No remote host specified" );

        elements.add( "-L" );
        elements.add( _localBindAddress + ":" + Integer.toString( _localPort ) + ":" + _remoteHost + ":" +Integer.toString( _remotePort ));
    }


    /**
     * Tells this instance to forward connections to a local TCP port to the remote host, and from there forward them to a TCP port on another host.
     * This is useful when a server is accessible from the remote host, but not the local host.  Once this port forwarding is set up, processes on the
     * local host, or other hosts with network access to the local host, can access the server normally only accessible from the remote host.  See
     * <a href="https://en.wikipedia.org/wiki/Port_forwarding" target="_top">Wikipedia</a> for more information.
     * <p>The local port address specifies the TCP port that will be listening on the local host.</p>
     * <p>The remote host specifies the host name or IP address of the remote server that connections will be forwarded to.</p>
     * <p>The remote port specifies the TCP port on the remote host that connections will be forwarded to.</p>
     *
     * @param _localPort the TCP port on the local host to listen on, bound to localhost (127.0.0.1) so that only local processes may connect to it
     * @param _remoteHost the host name or IP address of the remote server to forward connections to
     * @param _remotePort the TCP port on the remote server to forward connections to
     */
    public void setLocalPortForwarding( final int _localPort, final String _remoteHost, final int _remotePort ) {

        // sanity checks...
        if( !forwarding )
            throw new IllegalStateException( "Attempted to set local port forwarding when instance is not in forwarding mode" );
        if( isEmpty( _remoteHost ) )
            throw new IllegalArgumentException( "No remote host specified" );

        elements.add( "-L" );
        elements.add( Integer.toString( _localPort ) + ":" + _remoteHost + ":" +Integer.toString( _remotePort ));
    }


    /**
     * Tells this instance to forward connections from a TCP port on the remote host to the local host, and from there forward them to a TCP port on
     * another host.  This is useful when a server is accessible from the local host, but not the remote host.  Once this port forwarding is set up,
     * processes on the remote host, or other hosts with network access to the remote host, can access the server normally only accessible from the
     * local host.  See <a href="https://en.wikipedia.org/wiki/Port_forwarding" target="_top">Wikipedia</a> for more information.
     * <p>The remote binding address can be the IP address of a particular interface to bind only to that interface, "*" to bind to all interfaces,
     * or "localhost" to bind only to the localhost address (127.0.0.1) for local use only.  Note that the SSH server's <b>GatewayPorts</b> setting
     * may have to be changed to allow this to work.</p>
     * <p>The remote port address specifies the TCP port that will be listening on the remote host.</p>
     * <p>The local host specifies the host name or IP address of the local server that connections will be forwarded to.</p>
     * <p>The local port specifies the TCP port on the local server that connections will be forwarded to.</p>
     *
     * @param _remoteBindAddress the IP address to bind to on the remote host
     * @param _remotePort the TCP port on the remote host to listen on
     * @param _localHost the host name or IP address of the local server to forward connections to
     * @param _localPort the TCP port on the local server to forward connections to
     */
    public void setRemotePortForwarding( final String _remoteBindAddress, final int _remotePort, final String _localHost, final int _localPort ) {

        // sanity checks...
        if( !forwarding )
            throw new IllegalStateException( "Attempted to set remote port forwarding when instance is not in forwarding mode" );
        if( isEmpty( _remoteBindAddress ) )
            throw new IllegalArgumentException( "No remote bind address specified" );
        if( isEmpty( _localHost ) )
            throw new IllegalArgumentException( "No local host specified" );

        elements.add( "-R" );
        elements.add( _remoteBindAddress + ":" + Integer.toString( _remotePort ) + ":" + _localHost + ":" + Integer.toString( _localPort ) );
    }


    /**
     * Tells this instance to forward connections from a TCP port on the remote host to the local host, and from there forward them to a TCP port on
     * another host.  This is useful when a server is accessible from the local host, but not the remote host.  Once this port forwarding is set up,
     * processes on the remote host, or other hosts with network access to the remote host, can access the server normally only accessible from the
     * local host.  See <a href="https://en.wikipedia.org/wiki/Port_forwarding" target="_top">Wikipedia</a> for more information.
     * <p>The remote port address specifies the TCP port that will be listening on the remote host.</p>
     * <p>The local host specifies the host name or IP address of the local server that connections will be forwarded to.</p>
     * <p>The local port specifies the TCP port on the local server that connections will be forwarded to.</p>
     *
     * @param _remotePort the TCP port on the remote host to listen on, bound to localhost (127.0.0.1) so that only local processes may connect to it
     * @param _localHost the host name or IP address of the local server to forward connections to
     * @param _localPort the TCP port on the local server to forward connections to
     */
    public void setRemotePortForwarding( final int _remotePort, final String _localHost, final int _localPort ) {

        // sanity checks...
        if( !forwarding )
            throw new IllegalStateException( "Attempted to set remote port forwarding when instance is not in forwarding mode" );
        if( isEmpty( _localHost ) )
            throw new IllegalArgumentException( "No local host specified" );

        elements.add( "-R" );
        elements.add( Integer.toString( _remotePort ) + ":" + _localHost + ":" + Integer.toString( _localPort ) );
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
        if( !interactive && !forwarding )
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
    public String getRemoteOutput() throws IOException {
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
     * Write the specified string to the remote host shell's input, encoding it as UTF-8.  This command is only usable in interactive mode.
     *
     * @param _input the string to input to SSH
     * @throws IOException on any I/O error
     * @throws IllegalStateException if this instance is in direct host command execution mode
     */
    public void sendRemoteInput( final String _input ) throws IOException {

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


    public String toString() {
        StringBuilder sb = new StringBuilder();
        for( String element : elements ) {
            if( sb.length() > 0 )
                sb.append( " " );
            sb.append( quoteIfNecessary( element ) );
        }
        if( !interactive && !forwarding ) {
            sb.append( " " );
            sb.append( quoteIfNecessary( command ) );
        }
        return sb.toString();
    }


    private String quoteIfNecessary( final String _str ) {
        String result = _str;
        if( (result.length() == 0) || ((result.charAt( 0 ) != '"') && result.contains( " " )) )
            result = Bash.doubleQuote( result );
        return result;
    }


    /**
     * Simple test code.
     * 
     * @param _args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main( String[] _args ) throws IOException, InterruptedException {

        // direct mode example...
        SSHExecutor ssh = new SSHExecutor( "beast", 5, 2 );
        ssh.setOnlyIPv4();
        ssh.start();
        ssh.waitFor( 100, TimeUnit.MILLISECONDS );
        String directOutput = ssh.getRemoteOutput();

        // interactive mode example...
        ssh = new SSHExecutor( "beast" );
        ssh.start();
        Thread.sleep( 100 );  // allow time to connect...
        ssh.sendRemoteInput( "ls -l /apps\n" );
        Thread.sleep( 100 );  // allow time to execute...
        String interactiveOutput = ssh.getRemoteOutput();
        ssh.sendRemoteInput( "exit\n" );
        ssh.waitFor( 100, TimeUnit.MILLISECONDS );

        // port forwarding example...
        ssh = new SSHExecutor( "paradise", 5, 2 );
        ssh.setRemotePortForwarding( 5432, "beast", 22 );
        ssh.setLocalPortForwarding( 5432, "localhost", 5432 );
        ssh.setVerbose( 3 );
        ssh.start();
        String portForwardSSH = ssh.toString();
        Thread.sleep( 100 );
        String portForwardOutput = ssh.getRemoteOutput();
        String portForwardError = ssh.getSSHErrorOutput();
        ssh.waitFor();

        ssh.hashCode();
    }
}