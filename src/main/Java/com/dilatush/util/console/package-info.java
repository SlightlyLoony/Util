/**
 * <p>Provides a simple, reasonably secure embeddable console.  The intent is to provide a remote command-line capability for embedded systems or
 * server daemons.</p>
 * <p>The general usage:</p>
 * <ul>
 *     <li>An embedded process or a daemon process creates, configures, and starts an instance of {@link com.dilatush.util.console.ConsoleServer}.
 *     </li>
 *     <li>One or more instances of {@link com.dilatush.util.console.ConsoleProvider} are configured in the {@code ConsoleServer}.  Each of these
 *     {@code ConsoleProvider}s provides an interface to some aspect of the embedded process or daemon process.  What they do is arbitrary, as is
 *     the number of them; these choices are more a matter of taste than anything else.</li>
 *     <li>One or more instances of {@link com.dilatush.util.console.CommandProcessor} is embedded within each {@code ConsoleProvider}.  Each
 *     {@code CommandProcessor} handles a single named command, which a {@code ConsoleProvider} may have any number of.  For example, one could
 *     have a {@code ConsoleProvider} that allowed direct control of a motor, and three {@code CommandProcessor}s that (1) turned the motor off, (2)
 *     set the motor to run at a particular RPM, or (3) to cause the motor to cycle on and off.</li>
 * </ul>
 * <p>This implementation uses a simple, fixed protocol (described below) that is encrypted with AES with 128 bit keys in CTR mode with
 * a random 128 bit nonce, in continuous streams (separate streams in each direction).  The AES key is a shared secret, manually distributed, and
 * access to it implicitly provides authentication.</p>
 * <p>The protocol is as follows:</p>
 * <ol>
 *     <li>The client initiates a TCP connection to the server, which is listening on specific, known TCP port.</li>
 *     <li>The server transmits a fixed identification string ("Loony Console Server"), its version (as major.minor), and its name as comma-separated
 *     values followed by a newline, all encoded in UTF-8 and sent as plaintext.  For example, a console server of the first-released version, with a
 *     name of "test", would transmit "Loony Console Server,1.0,test\n".  The only restrictions on the name are that it may not contain either a
 *     comma or a newline, for obvious reasons.</li>
 *     <li>The client receives this string and determines (from the name) whether it has an encryption key for this console server.  If it does
 *     not, it closes the TCP connection.</li>
 *     <li>The client generates a random 128 bit initialization vector and transmits it (in binary) to the server.  All further transmissions from the
 *     client to the server are encrypted using this initialization vector and the shared key.</li>
 *     <li>The client sends a console name, terminated by a newline, to the server.  If the server does not provide a console by this name, it
 *     terminates the connection.</li>
 *     <li>The server generates a random 128 bit initialization vector and transmits it (in binary) to the client.  All further transmissions from the
 *     server to the client are encrypted using this initialization vector and the shared key.</li>
 *     <li>The server sends "OK\n" to the client, as an acknowledgement of successful connection and console selection.  If the client receives
 *     anything other than "OK\n", it terminates the connection.</li>
 * </ol>
 * <p>Once the above protocol has been satisfied, there exists an encrypted TCP connection between the console client and the specified console
 * provided by the console server.  These two can exchange data for so long as the connection is maintained.  Either side may end the session at any
 * time by terminating the TCP connection.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
package com.dilatush.util.console;