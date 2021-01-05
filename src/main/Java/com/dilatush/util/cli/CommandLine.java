package com.dilatush.util.cli;

import com.dilatush.util.Files;
import com.sun.istack.internal.NotNull;

import java.io.Console;
import java.io.File;
import java.util.*;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Instances of this class define the arguments expected on the command line, and parse an actual command line.
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
    private final List<ArgDef>           optionalDefs;      // ordered list of optional arguments (in order of their addition)

    private int     variableAppearanceCount;  // the count of positional parameters with variable number of appearances...
    private boolean nextArgPositional;        // true if the next argument processed is positional even if it starts with a "-"
    private boolean allArgsPositional;        // true if all all arguments process are positional even if they start with a "-"


    /**
     * Creates a new instance of this class, with the given summary and detail help.
     *
     * @param _summary The (shorter) summary help for this command.
     * @param _detail The (longer) detailed help for this command.
     */
    public CommandLine( final String _summary, final String _detail ) {

        argumentResults = new HashMap<>();
        shortDefs       = new HashMap<>();
        longDefs        = new HashMap<>();
        refDefs         = new HashMap<>();
        positionalDefs  = new ArrayList<>();
        optionalDefs    = new ArrayList<>();

        summary         = _summary;
        detail          = _detail;
    }


    /**
     * Parse the command line represented by the given arguments, returning a {@link ParsedCommandLine} with the results of that parsing.
     *
     * @param _args The application's command line arguments, as the parameter to it's <code>main( String[] args )</code> method.
     * @return the results of parsing the command line
     */
    public ParsedCommandLine parse( final String[] _args ) {

        // a little setup...
        variableAppearanceCount = 0;
        nextArgPositional = false;
        allArgsPositional = false;


        try {

            // first we process all the arguments we find on the command line...
            processPresentArguments( _args );

            // then we process all those arguments that we didn't find on the command line...
            processAbsentArguments();
        }

        // this exception is thrown when some error occurs during parsing
        catch( CLDefException _e ) {

            // return an invalid result, with an explanatory message (bummer!)...
            return new ParsedCommandLine( _e.getMessage() );
        }

        // count our appearances...
        Counts counts = countAppearances();

        // return a valid result - hooray!
        return new ParsedCommandLine( argumentResults, counts.optionals, counts.positionals );
    }


    /**
     * Count the total appearances of all optional arguments and (separately) all positional arguments.
     *
     * @return a tuple with the optional and positional appearance counts
     */
    private Counts countAppearances() {

        Counts counts = new Counts();

        // iterate over all the defined arguments...
        for( Map.Entry<String,ArgDef> entry : refDefs.entrySet() ) {

            // if it's a positional argument, sum its appearances into the positionals count...
            if( entry.getValue() instanceof APositionalArgDef ) {
                counts.positionals += argumentResults.get( entry.getKey() ).appearances;
            }

            // if it's an optional argument, sum its appearances into the optionals count...
            if( entry.getValue() instanceof AOptionalArgDef ) {
                counts.optionals += argumentResults.get( entry.getKey() ).appearances;
            }
        }

        return counts;
    }


    /**
     * Simple tuple for optional and positional appearances counts.
     */
    private static class Counts {
        private int optionals;
        private int positionals;
    }


    /**
     * Checks for missing arguments.
     *
     * @throws CLDefException if a required parameter is missing.
     */
    private void processAbsentArguments() throws CLDefException {

        // iterate over all our defined arguments...
        for( String refName : refDefs.keySet() ) {

            // get the definition and parse results for this argument...
            ArgDef    argDef    = refDefs.get( refName );
            ParsedArg argParsed = argumentResults.get( refName );

            // if we have no parsed argument, then we don't have this argument on the command line...
            if( isNull( argParsed ) ) {

                // update the missing argument, mainly to get the absent value...
                updateArgument( argDef.referenceName, argDef, null, false );
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

        updateArgument( def.referenceName, def, _arg, true );
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

            updateArgument( "-" + shortName, def, parameter, true );
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

        updateArgument( "--" + longName, def, parameter, true );
    }


    private void updateArgument( final String _nameUsed, final ArgDef _def, final String _parameter, final boolean _present ) throws CLDefException {

        // update the number of appearances...
        ParsedArg results =  argumentResults.get( _def.referenceName );
        int     appearances = (isNull( results ) ? 0 : results.appearances) + (_present ? 1 : 0);

        // if we have too many appearances, barf...
        if( appearances > _def.maxAllowed )
            throw new CLDefException( "Parameter '" + _nameUsed + "' appeared at least " + appearances
                    + " times, but the maximum allowed is " + _def.maxAllowed + "." );

        // figure out what value to use for the parameter...
        String parameter = _parameter;

        // if the argument is present, the parameter is mandatory, the parameter is missing, and we have interactive enabled...
        if( _present && (_def.parameterMode == ParameterMode.MANDATORY) && isEmpty( _parameter )
                && (_def.interactiveMode != InteractiveMode.DISALLOWED)) {

            // then we'll get the parameter from the user interactively...
            Console console = System.console();

            // if we can't get a Console (for instance, while running inside of an IDE), we skip this...
            if( !isNull( console ) ) {

                // prompt and read the answer...
                String prompt = isEmpty( _def.prompt ) ? "Enter value for " + _nameUsed + ": " : _def.prompt;
                if( _def.interactiveMode == InteractiveMode.PLAIN ) {
                    parameter = console.readLine( prompt );
                } else {
                    parameter = new String( console.readPassword( prompt ) );
                }
            }
        }

        // handle optional arguments...
        if( _def instanceof AOptionalArgDef ) {

            // handle the case of the argument being present...
            if( _present ) {

                // handle the parameter modes...
                switch( _def.parameterMode ) {
                    case DISALLOWED:
                        if( !isEmpty( parameter) )
                            throw new CLDefException( "Unexpected parameter '" + parameter + "' for argument '" + _nameUsed + "'." );
                        parameter = "" + appearances;
                        break;
                    case OPTIONAL:
                        parameter = isEmpty( parameter ) ? _def.defaultValue : parameter;
                        break;
                    case MANDATORY:
                        if( isEmpty( parameter ) )
                            throw new CLDefException( "Missing mandatory parameter for argument '" + _nameUsed + "'." );
                        break;
                }
            }

            // or not present...
            else {
                parameter = _def.absentValue;
            }
        }

        // handle positional arguments...
        if( _def instanceof APositionalArgDef ) {

            // handle case of argument not present...
            if( !_present ) {
                if( _def.parameterMode == ParameterMode.MANDATORY )
                    throw new CLDefException( "Missing mandatory positional argument: " + _nameUsed );
                parameter = environmentVariableCheck( _def.defaultValue );
            }
        }

        // expand the environmental variable, if one was specified...
        parameter = environmentVariableCheck( parameter );

        // expand the file contents, if one was specified...
        parameter = filePathCheck( parameter );

        // get our argument's current results and update them...
        Object  value = null;
        if( parameter != null ) {
            if( _def.parser != null ) {

                // invoke the parser and get the result...
                value = _def.parser.parse( parameter );

                // if the result was null, the parser had problem...
                if( value == null ) {
                    throw new CLDefException( _def.parser.getErrorMessage() );
                }

                // if the result was of the wrong type, we have a worser problem...
                if( !_def.type.isAssignableFrom( value.getClass() ) ) {
                    throw new CLDefException( "Expected parser result of type " + _def.type.getSimpleName()
                            + ", got: " + value.getClass().getSimpleName() );
                }
            }
            else {
                value = parameter;
            }
            if( _def.validator != null ) {
                if( !_def.validator.validate( value ) ) {
                    throw new CLDefException( _def.validator.getErrorMessage() );
                }
            }
        }

        // now update the argument's results...
        argumentResults.put( _def.referenceName, new ParsedArg( _present, value, appearances ) );
    }


    private String environmentVariableCheck( final String _parameter ) {

        // if we have an environment variable name surrounded by percent signs, extract it...
        if( (_parameter.length() > 2) && (_parameter.charAt( 0 ) == '%') && (_parameter.charAt( _parameter.length() - 1 ) == '%') ) {
            String env = System.getenv( _parameter.substring( 1, _parameter.length() - 1 ) );
            return isNull( env ) ? "" : env;
        }

        // otherwise, just return what we got...
        return _parameter;
    }


    private String filePathCheck( final String _parameter ) {

        // if we have a file path surrounded by pound signs, extract it...
        if( (_parameter.length() > 2) && (_parameter.charAt( 0 ) == '#') && (_parameter.charAt( _parameter.length() - 1 ) == '#') ) {
            String path = _parameter.substring( 1, _parameter.length() - 1 );
            String contents = Files.readToString( new File( path ) );
            return isNull( contents ) ? "" : contents;
        }

        // otherwise, just return what we got...
        return _parameter;
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

        // if we've got a positional argument...
        if( _argDef instanceof APositionalArgDef ) {

            // if we have a parameter mode of DISALLOWED, that's not allowed...
            if( _argDef.parameterMode == ParameterMode.DISALLOWED )
                throw new IllegalArgumentException( "Tried to add a positional argument with a DISALLOWED parameter mode" );

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

            // stuff it away in order, for help production...
            optionalDefs.add( optionalArgDef );
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
