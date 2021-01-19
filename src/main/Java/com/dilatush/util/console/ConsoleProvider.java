package com.dilatush.util.console;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

/**
 * Implemented by classes providing consoles.  In general, these classes read commands from an input stream, and provide response through an
 * output stream.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface ConsoleProvider {

    void run( final Socket _socket, final BufferedReader _reader, final BufferedWriter _writer);
}
