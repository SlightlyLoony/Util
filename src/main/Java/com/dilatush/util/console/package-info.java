/**
 * <p>Provides a simple, reasonably secure embeddable console.  The intent is to provide a remote command-line capability for embedded systems or server
 * daemons.  This implementation uses a simple, fixed protocol (described below) that is encrypted with AES with 128 bit keys in CTR mode with a
 * 64 bit nonce and 64 bit counter, in continuous streams (separate streams in each direction).  The AES key is a shared secret, manually
 * distributed, and ownership of it implicitly provides authentication.</p>
 * <p>The protocol is as follows:</p>
 * <ol>
 *     <li>The client initiates a TCP connection to the server, which is listening on specific TCP port.</li>
 *     <li>The server transmits an identification string, its version, and its name as comma-separated values followed by a newline, all encoded
 *     in UTF-8.  For example, a console server of the first-released version, with a name of "test", would transmit "Console Server,1.0,test\n".</li>
 *     <li>The client receives this string and determines (from the name) whether it has an encryption key for this console server.  If it does
 *     not, it closes the TCP connection.</li>
 *     <li>The server generates a random initialization vector, converts it to UTF-8 encoded base64, appends a newline, and transmits it to the
 *     client.  All further transmissions from the server to the client are encrypted using this initialization vector and the shared key.</li>
 *     <li>The client generates a random initialization vector, converts it to UTF-8 encoded base65, appends a newline, and transmits it to the
 *     server.  All further transmissions from the client to the server are encrypted using this initialization vector and the shared key.</li>
 *     <li>The server transmits a UTF-8 encoded, comma-separated list of the names of the consoles that it can provide, terminated by a newline.  For
 *     example, if a particular server could provide either a "test" console or a "status" console, it would transmit the string "test,status\n".</li>
 *     <li>The client chooses one of the consoles, and transmits its UTF-8 encoded name, terminated by a newline, to the server.</li>
 *     <li>At this point the connection is complete, and the server connects the selected console to the console client.</li>
 * </ol>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
package com.dilatush.util.console;