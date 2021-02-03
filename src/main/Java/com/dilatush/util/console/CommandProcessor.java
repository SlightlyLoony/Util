package com.dilatush.util.console;

import java.util.List;

/**
 * Subclasses of this abstract class implement a command processor for use by {@link CommandProcessorConsoleProvider}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class CommandProcessor {

    // the full name of this processor...
    public final String name;

    // the "one-liner" description of this processor...
    public final String description;

    // the more complete help for this processor...
    public final String help;


    /**
     * Create a new instance of this class with the given name, description, and help.
     *
     * @param _name The name of this command (which is what the user types to use it).
     * @param _description The brief "one-liner" description of this command.
     * @param _help The detailed help for this command.
     */
    protected CommandProcessor( final String _name, final String _description, final String _help ) {
        name        = _name;
        description = _description;
        help        = _help;
    }


    /**
     * Called when the console client invokes this command.  The given line is the entire command line as the user typed it.  The given list of
     * "words" are the parsed words and quoted strings that appear after the command itself, if any.
     *
     * @param _line  The command line as the user of the console client typed it.
     * @param _words The "words" appearing after the command itself.
     */
    protected abstract void onCommandLine( String _line, List<String> _words );
}
