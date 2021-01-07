package com.dilatush.util.cli;

import java.util.Map;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Instances of this class contain the result of parsing the arguments on the command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ParsedCommandLine {

    private final boolean               valid;
    private final Map<String,ParsedArg> parsedArguments;
    private final String                errorMsg;
    private final int                   totalPresentCount;
    private final int                   optionalPresentCount;
    private final int                   positionalPresentCount;


    /**
     * Creates a new instance of this class that contains the given parsed arguments, is valid, and has no error message.
     *
     * @param _parsedArguments The map of the results of parsing the command line arguments.
     * @param _optionalPresentCount The count of optional arguments present on the command line.
     * @param _positionalPresentCount The count of positional arguments present on the command line.
     */
    public ParsedCommandLine( final Map<String, ParsedArg> _parsedArguments, final int _optionalPresentCount, final int _positionalPresentCount ) {
        parsedArguments = _parsedArguments;
        valid = true;
        errorMsg = null;
        optionalPresentCount = _optionalPresentCount;
        positionalPresentCount = _positionalPresentCount;
        totalPresentCount = optionalPresentCount + positionalPresentCount;
    }


    /**
     * Creates a new instance of this class that is not valid, contains an error message, but has no parsed arguments.
     *
     * @param _errorMsg The error message describing why the command line could not be parsed.
     */
    public ParsedCommandLine( final String _errorMsg ) {
        errorMsg = _errorMsg;
        parsedArguments = null;
        valid = false;
        totalPresentCount = 0;
        optionalPresentCount = 0;
        positionalPresentCount = 0;
    }


    /**
     * Returns the {@link ParsedArg} instance for the argument with the given reference name.  If the result of parsing the arguments was not valid
     * (i.e., {@link #isValid()} returns <code>false</code>), or if the given reference name is empty or <code>null</code>, or if the given
     * reference name is not present in the argument definitions, then returns a <code>null</code>.
     *
     * @param _argumentName The reference name of the argument whose results are being retrieved.
     * @return the {@link ParsedArg} instance for the argument with the given reference name
     */
    public ParsedArg get( final String _argumentName ) {

        // if we didn't get an argument name, return null...
        if( isEmpty( _argumentName ) )
            return null;

        // if we have no results (because there was a parsing error, presumably), then return a null...
        if( parsedArguments == null )
            return null;

        // otherwise, return whatever our map has for the given name..
        return parsedArguments.get( _argumentName );
    }


    /**
     * Returns <code>true</code> if the argument with the given reference name was present on the parsed command line at least once.  Note that
     * <code>false</code> will be returned if the result of parsing the command line was not valid, if the given reference name is empty or
     * <code>null</code>, or if the given reference name is not present in the argument definitions.
     *
     * @param _argumentName The reference name of the argument whose presence is being queried.
     * @return  <code>true</code> if the argument with the given reference name was present on the parsed command line at least once
     */
    @SuppressWarnings( "unused" )
    public boolean isPresent( final String _argumentName ) {

        // try to get the parsed argument results...
        ParsedArg parsedArg = get( _argumentName );

        // if we failed to get the parsed argument results, return false...
        if( parsedArg == null )
            return false;

        // otherwise, return the actual value...
        return parsedArg.present;
    }


    /**
     * Returns the value of the argument with the given reference name (as an object of the class defined in the argument definition) if it was
     * present on the parsed command line, or if it is a binary argument and it was not present on the parsed command line.  Note that
     * <code>null</code> will be returned if the result of parsing the command line was not valid, if the given reference name is empty or
     * <code>null</code>, or if the given reference name is not present in the argument definitions.
     * @see ParsedArg#value
     *
     * @param _argumentName The reference name of the argument whose value is being queried.
     * @return  the value of the argument (as an object of the class defined in the argument definition) if the argument with the given reference
     *          name was present on the parsed command line, or if it is a binary argument and it was not present on the parsed command line
     */
    @SuppressWarnings( "unused" )
    public Object getValue( final String _argumentName ) {

        // try to get the parsed argument results...
        ParsedArg parsedArg = get( _argumentName );

        // if we failed to get the parsed argument results, return null...
        if( parsedArg == null )
            return null;

        // otherwise, return the actual value...
        return parsedArg.value;
    }


    /**
     * Returns the number of times that the argument with the given reference name was present on the parsed command line.  Note that zero
     * will be returned if the result of parsing the command line was not valid, if the given reference name is empty or
     * <code>null</code>, or if the given reference name is not present in the argument definitions.
     *
     * @param _argumentName The reference name of the argument whose appearances are being queried.
     * @return  the number of times that the argument with the given reference name was present on the parsed command line
     */
    @SuppressWarnings( "unused" )
    public int getAppearances( final String _argumentName ) {

        // try to get the parsed argument results...
        ParsedArg parsedArg = get( _argumentName );

        // if we failed to get the parsed argument results, return zero...
        if( parsedArg == null )
            return 0;

        // otherwise, return the actual value...
        return parsedArg.appearances;

    }


    /**
     * Returns <code>true</code> if all of the command line arguments were parsed successfully, including passing all validity tests.
     *
     * @return <code>true</code> if all of the command line arguments were parsed successfully, including passing all validity tests
     */
    public boolean isValid() {
        return valid;
    }


    /**
     * Returns an explanatory error message if {@link #isValid()} returns <code>false</code>; <code>null</code> otherwise.
     *
     * @return an explanatory error message if {@link #isValid()} returns <code>false</code>; <code>null</code> otherwise
     */
    @SuppressWarnings( "unused" )
    public String getErrorMsg() {
        return errorMsg;
    }


    /**
     * Returns the number of arguments (both optional and positional) that were present.
     *
     * @return the number of arguments (both optional and positional) that were present
     */
    @SuppressWarnings( "unused" )
    public int presentCount() {
        return totalPresentCount;
    }


    /**
     * Returns the number of optional arguments that were present.
     *
     * @return the number of optional arguments that were present
     */
    @SuppressWarnings( "unused" )
    public int optionalPresentCount() {
        return optionalPresentCount;
    }


    /**
     * Returns the number of positional arguments that were present.
     *
     * @return the number of positional arguments that were present
     */
    @SuppressWarnings( "unused" )
    public int positionalPresentCount() {
        return positionalPresentCount;
    }
}
