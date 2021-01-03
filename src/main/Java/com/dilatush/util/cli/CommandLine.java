package com.dilatush.util.cli;

import com.sun.istack.internal.NotNull;

import java.util.*;

/**
 * Instances of this class define a valid command line, and parse an actual command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CommandLine {

    private final Map<String, ParsedArg> argumentResults;   // maps argument reference names to argument results
    private final Map<Character, ArgDef> shortDefs;         // maps short argument names to optional argument definitions
    private final Map<String, ArgDef>    longDefs;          // maps long argument names to optional argument definitions
    private final Map<String, ArgDef>    refDefs;           // maps reference names to argument definitions
    private final List<ArgDef>           positionalDefs;    // ordered list of positional arguments (left-to-right)
    private final String                 summary;           // a summary detail of this command
    private final String                 detail;            // a detailed detail of this command

    private int     variableAppearanceCount;  // the count of positional parameters with variable number of appearances...
    private boolean nextArgPositional;        // true if the next argument processed is positional even if it starts with a "-"
    private boolean allArgsPositional;        // true if all all arguments process are positional even if they start with a "-"


    public CommandLine( final String _summary, final String _detail ) {

        argumentResults = new HashMap<>();
        shortDefs       = new HashMap<>();
        longDefs        = new HashMap<>();
        refDefs         = new HashMap<>();
        positionalDefs  = new ArrayList<>();

        summary         = _summary;
        detail          = _detail;
    }


    /**
     * Parse the command line represented by the given arguments.
     *
     * @param _args The command line arguments.
     * @return the parsed command line
     */
    public ParsedCLI parse( final String[] _args ) {

        // a little setup...
        variableAppearanceCount = 0;
        nextArgPositional = false;
        allArgsPositional = false;


        try {

            // first we process all the arguments we find on the command line...
            processPresentArguments( _args );

            // then we see if any mandatory arguments are missing...
            checkForMissing();
        }
        catch( CLDefException _e ) {
            return new ParsedCLI( _e.getMessage() );
        }

        // count our appearances...
        int optionals = 0;
        int positionals = 0;
        for( Map.Entry<String,ArgDef> entry : refDefs.entrySet() ) {
            if( entry.getValue() instanceof APositionalArgDef ) {
                positionals += argumentResults.get( entry.getKey() ).appearances;
            }
            if( entry.getValue() instanceof AOptionalArgDef ) {
                optionals += argumentResults.get( entry.getKey() ).appearances;
            }
        }

        return new ParsedCLI( argumentResults, optionals, positionals );
    }


    private void checkForMissing() throws CLDefException {

        for( String refName : refDefs.keySet() ) {

            ArgDef    argDef    = refDefs.get( refName );
            ParsedArg argParsed = argumentResults.get( refName );

            // if we have an optional parameter, by definition it cannot be required...
            if( argDef instanceof AOptionalArgDef )
                continue;

            if( (argDef.parameterMode == ParameterMode.MANDATORY) && (argParsed.value == null) ) {
                throw new CLDefException( "Required parameter is missing: " + argDef.referenceName );
            }
        }
    }


    private void processPresentArguments( final String[] _args ) throws CLDefException {
        // a little setup...
        RefInt ppi = new RefInt();  // our positional parameter index...
        RefInt i = new RefInt();    // our command line argument index...

        // walk through our arguments and handle them...
        for( i.value = 0; i.value < _args.length; i.value++ ) {

            String arg = _args[i.value];

            if     ( nextArgPositional || allArgsPositional ) handleSpecials( arg, ppi );
            else if( "-".equals( arg ) )                      handleSingleHyphen ();
            else if( "--".equals( arg ) )                     handleDoubleHyphen ();
            else if( arg.startsWith( "--" ) )                 handleLongOptional ( _args, i, arg );
            else if( arg.startsWith( "-" ) )                  handleShortOptional( arg           );
            else                                              handlePositional   ( arg, ppi      );
        }
    }


    private void handleSpecials( final String _arg, final RefInt _ppi ) throws CLDefException {
        nextArgPositional = false;
        handlePositional( _arg, _ppi );
    }


    private void handleSingleHyphen() {
        nextArgPositional = true;
    }


    private void handleDoubleHyphen() {
        allArgsPositional = true;
    }


    private void handlePositional( final String _arg, RefInt _ppi ) throws CLDefException {

        // get our definition, and bump the index...
        ArgDef def = positionalDefs.get( _ppi.value++ );

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
        if( def.parameterMode != ParameterMode.DISALLOWED ) {
            if( i.value + 1 < _args.length ) {
                String trial = _args[i.value + 1];
                if( !trial.startsWith( "-" ) ) {
                    i.value++;
                    parameter = trial;
                }
            }
        }

        updateArgument( "--" + longName, def, parameter );
    }


    private void updateArgument( final String _nameUsed, final ArgDef _def, final String _parameter ) throws CLDefException {

        // if we have a parameter and parameters are not allowed, barf...
        if( (_parameter != null) && (_def.parameterMode == ParameterMode.DISALLOWED ) )
            throw new CLDefException( "Unexpected parameter '" + _parameter + "' for argument '" + _nameUsed + "'." );

        // if we don't have a parameter, and parameters are mandatory, barf...
        if( (_parameter == null) && (_def.parameterMode == ParameterMode.MANDATORY) )
            throw new CLDefException( "Missing mandatory parameter for argument '" + _nameUsed + "'." );

        // get our argument's current results and update them...
        Object parameterValue = null;
        if( _parameter != null ) {
            if( _def.parser != null ) {

                // invoke the parser and get the result...
                parameterValue = _def.parser.parse( _parameter );

                // if the result was null, the parser had problem...
                if( parameterValue == null ) {
                    throw new CLDefException( _def.parser.getErrorMessage() );
                }

                // if the result was of the wrong type, we have a worser problem...
                if( !_def.type.isAssignableFrom( parameterValue.getClass() ) ) {
                    throw new CLDefException( "Expected parser result of type " + _def.type.getSimpleName()
                            + ", got: " + parameterValue.getClass().getSimpleName() );
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
        if( _def.parameterMode == ParameterMode.DISALLOWED ) {
            argumentResults.put( _def.referenceName, new ParsedArg(
                    true, results.appearances + 1, results.appearances + 1
            ) );
        }

        // otherwise, use the value from the parameter...
        else {
            argumentResults.put( _def.referenceName, new ParsedArg(
                    true, parameterValue, results.appearances + 1
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

        // make sure we've got either a positional or an optional argument definition - nothing else will do...
        if( !((_argDef instanceof AOptionalArgDef) || (_argDef instanceof APositionalArgDef)) )
            throw new IllegalArgumentException( "_argDef must be instance of either AOptionalArgDef or APositionalArgDef" );

        // some things apply to any argument type...
        // stuff it away by reference name, checking for duplication...
        if( refDefs.containsKey( _argDef.referenceName ) )
            throw new IllegalArgumentException( "Duplicate argument reference name: " + _argDef.referenceName );
        refDefs.put( _argDef.referenceName, _argDef );

        // stuff away the default results (this is how we handle the values of non-appearing arguments)...
        argumentResults.put( _argDef.referenceName, new ParsedArg( _argDef ) );

        // if we've got a positional argument...
        if( _argDef instanceof APositionalArgDef ) {

            // if this argument can appear a variable number of times, make sure we're not trying to do more than one of these...
            if( _argDef.maxAllowed != 1 ) {
                variableAppearanceCount++;
                if( variableAppearanceCount >= 2 )
                    throw new IllegalArgumentException( "Tried to add a second positional arguments with variable number of appearances: "
                            + _argDef.referenceName );
            }
            positionalDefs.add( _argDef );
        }

        // if we've got an optional argument...
        if( _argDef instanceof AOptionalArgDef ) {

            AOptionalArgDef optionalArgDef = (AOptionalArgDef) _argDef;

            // stuff it away by short name(s), checking for duplicates...
            if( optionalArgDef.shortNames != null ) {
                for( char shortName : optionalArgDef.shortNames ) {
                    if( shortDefs.containsKey( shortName ) )
                        throw new IllegalArgumentException( "Duplicate short optional argument name: '" + shortName + "' in " + _argDef.referenceName );
                    shortDefs.put( shortName, _argDef );
                }
            }

            // stuff it away by long names, checking for duplicates...
            if( optionalArgDef.longNames != null ) {
                for( String longName : optionalArgDef.longNames ) {
                    if( longDefs.containsKey( longName ) )
                        throw new IllegalArgumentException( "Duplicate long optional argument name: '" + longName + "' in " + _argDef.referenceName );
                    longDefs.put( longName, _argDef );
                }
            }
        }
    }


    public String getSummary() {
        return summary;
    }


    public String getDetail() {
        return detail;
    }


    private static class CLDefException extends Exception {

        private CLDefException( final String _msg ) {
            super( _msg );
        }
    }


    private static class RefInt {
        private int value;
    }
}
