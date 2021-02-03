package com.dilatush.util.console;

import com.dilatush.util.Strings;
import com.dilatush.util.TextFormatter;

import java.util.ArrayList;
import java.util.List;

import static com.dilatush.util.Strings.*;

/**
 * An abstract class that extends {@link ConsoleProvider} to add the notion of a Command Line Processor, which is a facility that makes it easier
 * to write console server applications that accept command lines, much like you might find when using bash.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class CommandProcessorConsoleProvider extends ConsoleProvider {

    private static final int CONSOLE_WIDTH = 80;
    private static final int SPACES_AFTER_PERIOD = 2;

    // an unordered list of all the registered command processors...
    private final List<CommandProcessor> commandProcessors;

    // a list of command names in the same order as the registered processors...
    private final List<String>            commands;

    // help for the overall console...
    private final String help;


    /**
     * Create a new instance of this class with the given help text, which should describe the purpose of this console.
     *
     * @param _help The help text that describes the purpose of this console.
     */
    protected CommandProcessorConsoleProvider( final String _help ) {
        help              = _help;
        commandProcessors = new ArrayList<>();
        commands          = new ArrayList<>();
    }


    /**
     * Add a new command processor (an instance of {@link CommandProcessor} implements one command that this console can perform).
     *
     * @param _commandProcessor The command processor to add.
     */
    protected void addCommandProcessor( final CommandProcessor _commandProcessor ) {

        // add the new processor...
        commandProcessors.add( _commandProcessor );
    }


    /**
     * Finish preparing the command processors for use.  This method <i>must</i> be called after all the command processors have been added.
     */
    protected void finish() {

        // add our default help and exit processors...
        commandProcessors.add( new HelpProcessor() );
        commandProcessors.add( new ExitProcessor() );

        // generate our list of command names...
        commands.clear();
        for( CommandProcessor processor : commandProcessors )
            commands.add( processor.name );
    }


    /**
     * Parse the given line of text from the console into "words", the first of which is a command.  The matching command processor is then called
     * with the parsed words after the command as its arguments.  "Words" here means strings (unquoted strings without separators, or double-quoted
     * strings containing anything) separated by whitespace or commas.  Inside quoted strings, a backslash ("\") may be used to escape either
     * double-quotes or backslashes.
     *
     * @param _line The line of text received from the console client.
     */
    @Override
    protected void onLine( final String _line ) {

        // parse our line into words...
        List<String> words = parseToWords( _line );

        // if we got no words, then we're going to print some help and skedaddle...
        if( words.size() == 0 ) {
            writeLine( "Did you forget something?  Like, perhaps, entering a command?" );
            showGeneralHelp();
            return;
        }

        // match the first word to a command (or at least TRY to!)...
        int match = chooseFrom( commands, words.get( 0 ), true );

        // if we got a unique match on the command, then execute it...
        if( match >= 0 ) {
            CommandProcessor processor = commandProcessors.get( match );
            processor.onCommandLine( _line, words.subList( 1, words.size() ) );
        }

        // otherwise, if we have no idea what the command was, let the user know and print some help...
        else if( match == Strings.NO_KEY_MATCH_FOR_CHOOSER ) {
            writeLine( "We do not understand this command: " + words.get( 0 ) );
            showGeneralHelp();
        }

        // otherwise, if the command could have been one of several, let the user know and print some help...
        else if( match == Strings.AMBIGUOUS_KEY_MATCH_FOR_CHOOSER ) {
            writeLine( "Come on, gimme a break - what you typed COULD mean more than one of these commands: " + words.get( 0 ) );
            showGeneralHelp();
        }
    }


    /**
     * Display general help on the console, rules for typing commands, and a list of the available commands with a brief description.
     */
    private void showGeneralHelp() {

        // set up a text formatter and print out the general help...
        TextFormatter textFormatter = new TextFormatter( CONSOLE_WIDTH, 0, 0, SPACES_AFTER_PERIOD );
        textFormatter.add( help );
        textFormatter.add( "//BR//When typing the command, you only need enough letters to unambiguously choose the one you want.  " +
                "The available commands are:" );
        writeLine( textFormatter.getFormattedText() );

        // now format and print the commands...
        int width = longest( commands ) + 3;
        textFormatter = new TextFormatter( CONSOLE_WIDTH, width, -width, SPACES_AFTER_PERIOD );
        for( int i = 0; i < commandProcessors.size(); i++ ) {
            textFormatter.add( commands.get( i ) + "//TAB" + width + "//" + commandProcessors.get( i ).description );
            write( textFormatter.getFormattedText() );
            textFormatter.clear();
        }
    }


    /**
     * Display detailed help for the given command.
     *
     * @param _command The command to display help for.
     */
    private void showCommandHelp( final String _command ) {

        // match the command (or at least TRY to!)...
        int match = chooseFrom( commands, _command, true );

        // if we got a unique match on the command, then print help for it...
        if( match >= 0 ) {
            TextFormatter textFormatter = new TextFormatter( CONSOLE_WIDTH, 0, 0, SPACES_AFTER_PERIOD );
            textFormatter.add( commandProcessors.get( match ).help );
            writeLine( textFormatter.getFormattedText() );
        }

        // otherwise, if we have no idea what the command was, let the user know and print some help...
        else if( match == Strings.NO_KEY_MATCH_FOR_CHOOSER ) {
            writeLine( "We can't help you, because we do not understand this command: " + _command );
            showGeneralHelp();
        }

        // otherwise, if the command could have been one of several, let the user know and print some help...
        else if( match == Strings.AMBIGUOUS_KEY_MATCH_FOR_CHOOSER ) {
            writeLine( "Come on, gimme a break - what you typed COULD mean more than one of these commands: " + _command );
            showGeneralHelp();
        }
    }


    /**
     * The "built-in" help command processor.
     */
    private class HelpProcessor extends CommandProcessor {


        private HelpProcessor() {
            super( "?", "get general help, or help for a command",
                    "? [command]*\n" +
                    "When no command is specified, shows general help for this console, for using the commands and a summary description of " +
                    "all commands available.  If one or more commands are specified, shows detailed help for those commands."
            );
        }


        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {

            // if we have zero commands specified, then it's time for the general help...
            if( _words.size() == 0 ) {
                showGeneralHelp();
            }

            // otherwise, we're going to show command-specific help...
            else {

                // iterate over all the commands we got...
                for( String command : _words ) {
                    showCommandHelp( command );
                }
            }
        }
    }


    /**
     * The built in exit command processor.
     */
    private class ExitProcessor extends CommandProcessor {

        private ExitProcessor() {
            super( "exit", "exit this console",
                    "exit\n" +
                    "Exits from this console, which will cause the console client to terminate."
            );
        }


        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {
            exit( "Goodbye!  See you again soon, I hope..." );
        }
    }
}
