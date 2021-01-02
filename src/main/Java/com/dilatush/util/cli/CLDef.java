package com.dilatush.util.cli;

import com.sun.istack.internal.NotNull;

import java.util.*;

/**
 * Instances of this class define a valid command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CLDef {

    private final Map<String, ParsedArg> argumentResults;   // maps argument reference names to argument results
    private final Map<Character, ArgDef> shortDefs;         // maps short argument names to optional argument definitions
    private final Map<String, ArgDef>    longDefs;          // maps long argument names to optional argument definitions
    private final Map<String, ArgDef>    refDefs;           // maps reference names to argument definitions
    private final List<ArgDef>           positionalDefs;    // ordered list of positional arguments (left-to-right)
    private final String                 summary;           // a summary description of this command
    private final String                 detail;            // a detailed description of this command

    private int varArityCount;     // the count of positional parameters with variable arity...


    public CLDef( final String _summary, final String _detail ) {

        argumentResults = new HashMap<>();
        shortDefs       = new HashMap<>();
        longDefs        = new HashMap<>();
        refDefs         = new HashMap<>();
        positionalDefs  = new ArrayList<>();

        summary         = _summary;
        detail          = _detail;

        varArityCount = 0;
    }


    /**
     * Parse the command line represented by the given arguments.
     *
     * @param _args The command line arguments.
     * @return the parsed command line
     */
    public ParsedCLI parse( final String[] _args ) {

        try {

            // first we process all the arguments we find on the command line...
            processPresentArguments( _args );

            // then we see if any mandatory arguments are missing...
            checkForMissing();
        }
        catch( CLDefException _e ) {
            return new ParsedCLI( _e.getMessage() );
        }

        return new ParsedCLI( argumentResults );
    }


    private void checkForMissing() throws CLDefException {

        for( String refName : refDefs.keySet() ) {

            ArgDef    argDef    = refDefs.get( refName );
            ParsedArg argParsed = argumentResults.get( refName );

            if( (argDef.parameterAllowed == ParameterAllowed.MANDATORY) && (argParsed.value == null) ) {
                throw new CLDefException( "Required parameter is missing: " + argDef.referenceName );
            }
        }
    }


    private void processPresentArguments( final String[] _args ) throws CLDefException {
        // a little setup...
        RefInt ppi = new RefInt();  // our positional parameter index...
        RefInt i = new RefInt();    // our command line argument index...

        // walk through our arguments and handle them...
        for( i.index = 0; i.index < _args.length; i.index++ ) {

            String arg = _args[i.index];

            if     ( "-".equals( arg ) )      handleSingleHyphen ();
            else if( "--".equals( arg ) )     handleDoubleHyphen ();
            else if( arg.startsWith( "--" ) ) handleLongOptional ( _args, i, arg      );
            else if( arg.startsWith( "-" ) )  handleShortOptional( arg                 );
            else                              handlePositional   ( _args[i.index], ppi );
        }
    }


    private void handleSingleHyphen() {
        // TODO: what the heck do we do with this?
    }


    private void handleDoubleHyphen() {
        // TODO: what the heck do we do with this?
    }


    private void handlePositional( final String _arg, RefInt _ppi ) throws CLDefException {

        // get our definition, and bump the index...
        ArgDef def = positionalDefs.get( _ppi.index++ );

        updateArgument( def.referenceName, def, _arg );
    }


    private void handleShortOptional( final String _arg ) throws CLDefException {
        // get the one-or-more short names and the zero-or-one parameters...
        String[] parts = _arg.substring( 1 ).split( "=" );

        // if the length is more than two, we have a problem...
        if( parts.length > 2 )
            throw new CLDefException( "Invalid argument has multiple parameters: " + _arg );

        // walk through the short commands we've got...
        for( int a = 0; a < parts[0].length(); a++ ) {

            // get the short name and the parameter (if there is one)...
            char shortName = parts[0].charAt( a );
            String parameter = ((a + 1 >= parts[0].length()) && (parts.length == 2)) ? parts[1] : null;

            // if we don't have an argument definition for this short name, we've got a problem...
            if( !shortDefs.containsKey( shortName ) )
                throw new CLDefException( "Unknown optional argument: -" + shortName );

            // get our argument's definition...
            ArgDef def = shortDefs.get( shortName );

            updateArgument( "-" + shortName, def, parameter );
        }
    }


    private void handleLongOptional( final String[] _args, RefInt i, final String _arg ) throws CLDefException {

        // get the long name...
        String longName = _arg.substring( 2 );

        // if we don't have an argument definition for this argument name, we have a problem...
        if( !longDefs.containsKey( longName ) )
            throw new CLDefException( "Unknown optional argument: --" + longName );

        // get our argument definition...
        ArgDef def = longDefs.get( longName );

        // if a parameter is not disallowed, see if we have one...
        String parameter = null;
        if( def.parameterAllowed != ParameterAllowed.DISALLOWED ) {
            if( i.index + 1 < _args.length ) {
                String trial = _args[i.index + 1];
                if( !trial.startsWith( "-" ) ) {
                    i.index++;
                    parameter = trial;
                }
            }
        }

        updateArgument( "--" + longName, def, parameter );
    }


    private void updateArgument( final String _nameUsed, final ArgDef _def, final String _parameter ) throws CLDefException {

        // if we have a parameter and parameters are not allowed, barf...
        if( (_parameter != null) && (_def.parameterAllowed == ParameterAllowed.DISALLOWED ) )
            throw new CLDefException( "Unexpected parameter '" + _parameter + "' for argument '" + _nameUsed + "'." );

        // if we don't have a parameter, and parameters are mandatory, barf...
        if( (_parameter == null) && (_def.parameterAllowed == ParameterAllowed.MANDATORY) )
            throw new CLDefException( "Missing mandatory parameter for argument '" + _nameUsed + "'." );

        // get our argument's current results and update them...
        Object parameterValue = null;
        if( _parameter != null ) {
            if( _def.parser != null ) {
                parameterValue = _def.parser.parse( _parameter, _def.type );
                if( parameterValue == null ) {
                    throw new CLDefException( _def.parser.getErrorMessage() );
                }
            }
            else {
                parameterValue = _parameter;
            }
            if( _def.validator != null ) {
                if( !_def.validator.validate( parameterValue ) ) {
                    throw new CLDefException( _def.validator.getErrorMessage() );
                }
            }
        }

        // update our argument's value...
        ParsedArg results =  argumentResults.get( _def.referenceName );

        // in the special case of arguments with parameters disallowed, put the number of appearances as the value...
        if( _def.parameterAllowed == ParameterAllowed.DISALLOWED ) {
            argumentResults.put( _def.referenceName, new ParsedArg(
                    true, results.appearances + 1, results.appearances + 1, results.type
            ) );
        }

        // otherwise, use the value from the parameter...
        else {
            argumentResults.put( _def.referenceName, new ParsedArg(
                    true, parameterValue, results.appearances + 1, results.type
            ) );
        }
    }


    /**
     * Add the given {@link ArgDef} to this command line definition.  Optional argument definitions may be added in any order.  Positional arguments
     * <i>must</i> be added in left-to-right order.
     *
     * @param _argDef The {@link ArgDef} to add to this command line definition.
     */
    public void add( @NotNull final ArgDef _argDef ) {

        // fail fast if some dummy called us with a null...
        Objects.requireNonNull( _argDef, "Cannot add a null argument definition");

        // some things apply to any argument type...
        // stuff it away by reference name, checking for duplication...
        if( refDefs.containsKey( _argDef.referenceName ) )
            throw new IllegalArgumentException( "Duplicate argument reference name: " + _argDef.referenceName );
        refDefs.put( _argDef.referenceName, _argDef );

        // stuff away the default results (this is how we handle the values of non-appearing arguments)...
        argumentResults.put( _argDef.referenceName, new ParsedArg( _argDef ) );

        // if we've got a positional argument...
        if( _argDef.argType == ArgumentType.POSITIONAL ) {

            // if this argument has variable arity, make sure we're not trying to do more than one of these...
            if( _argDef.arity.isVariable() ) {
                varArityCount++;
                if( varArityCount >= 2 )
                    throw new IllegalArgumentException( "Tried to add a second positional arguments with variable arity: " + _argDef.referenceName );
            }
            positionalDefs.add( _argDef );
        }

        // if we've got an optional argument...
        else if( _argDef.argType == ArgumentType.OPTIONAL ) {

            // stuff it away by short name(s), checking for duplicates...
            if( _argDef.shortOptionNames != null ) {
                for( char shortName : _argDef.shortOptionNames ) {
                    if( shortDefs.containsKey( shortName ) )
                        throw new IllegalArgumentException( "Duplicate short optional argument name: '" + shortName + "' in " + _argDef.referenceName );
                    shortDefs.put( shortName, _argDef );
                }
            }

            // stuff it away by long names, checking for duplicates...
            if( _argDef.longOptionNames != null ) {
                for( String longName : _argDef.longOptionNames ) {
                    if( longDefs.containsKey( longName ) )
                        throw new IllegalArgumentException( "Duplicate long optional argument name: '" + longName + "' in " + _argDef.referenceName );
                    longDefs.put( longName, _argDef );
                }
            }
        }
    }


    private static class CLDefException extends Exception {

        private CLDefException( final String _msg ) {
            super( _msg );
        }
    }


    private static class RefInt {
        private int index;
    }
}
