package com.dilatush.util.cli;

import com.dilatush.util.Files;
import com.dilatush.util.TextFormatter;
import com.dilatush.util.cli.argdefs.ArgDef;
import com.dilatush.util.cli.argdefs.OptArgDef;
import com.dilatush.util.cli.argdefs.PosArgDef;
import com.dilatush.util.cli.parsers.ParameterParser;
import com.dilatush.util.cli.validators.ParameterValidator;

import java.io.*;
import java.util.*;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;
import static com.dilatush.util.cli.ParameterMode.*;


/**
 * Instances of this class define the arguments expected on the command line, and parse an actual command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CommandLine {

    /*
     * Maps names to argument definitions.  Three kinds of names are mapped: reference (unmodified), short optional (leading "-"), and
     * long optional (leading "--").
     */
    private final Map<String, ArgDef> nameDefs;

    private final List<OptArgDef>     optionalDefs;      // ordered list of optional argument definitions (in order of their addition)
    private final List<PosArgDef>     positionalDefs;    // ordered list of positional argument definitions (left-to-right)
    private final String              name;              // the name of the app this command line is for
    private final String              summary;           // a summary detail of this command
    private final String              detail;            // a detailed detail of this command
    private final int                 width;             // width of help text
    private final int                 indent;            // indent width for help text


    /**
     * Creates a new instance of this class with the given values.
     *
     * @param _name The name of this command.  This is used only for generating help text.
     * @param _summary The (shorter) summary description for this command.  Conventionally this is no more than a line or two, but there is no
     *                 actual restriction.
     * @param _detail The (longer) detailed help for this command.  This should be a very complete description, ideally similar to what one would
     *                find in a man page.
     * @param _width The width of generated help text, in characters.  Conventionally this is 80 characters, and probably shouldn't be set much
     *               differently than this.  Values less than 20 or greater than 150 will cause an {@link IllegalArgumentException} to be thrown.
     * @param _indent The left indent (tab size) of generated help text.  Conventionally this is 4, but there is no requirement other than it must
     *                be larger than 1.  Values less than 2 will cause an {@link IllegalArgumentException} to be thrown.
     */
    public CommandLine( final String _name, final String _summary, final String _detail, final int _width, final int _indent ) {

        nameDefs = new HashMap<>();

        positionalDefs  = new ArrayList<>();
        optionalDefs    = new ArrayList<>();

        name    = _name;
        summary = _summary;
        detail  = _detail;
        width   = _width;
        indent  = _indent;

        // some validation...
        if( isEmpty( _name ) )
            throw new IllegalArgumentException( "No command name supplied." );
        if( isEmpty( _summary ) )
            throw new IllegalArgumentException( "No summary description supplied." );
        if( isEmpty( _detail ) )
            throw new IllegalArgumentException( "No detailed description supplied." );
        if( (width < 20) || (width > 150) )
            throw new IllegalArgumentException( "Width is outside acceptable range of [20..150]." );
        if( indent < 2 )
            throw new IllegalArgumentException( "Indent is less than 2." );
    }


    /**
     * Parse the command line represented by the given arguments, returning a {@link ParsedCommandLine} with the results of that parsing.
     *
     * @param _args The application's command line arguments, as the parameter to it's <code>main( String[] args )</code> method.
     * @return the results of parsing the command line
     */
    public ParsedCommandLine parse( final String[] _args ) {

        // create a context for this parse operation...
        ParseContext context = new ParseContext();

        try {

            // normalize and do simple checking on our arguments...
            normalize( context, _args );

            // initialize all defined argument results to absent values...
            initialize( context );

            // parse all the optional arguments...
            parseOptionalArguments( context );

            // parse all the positional arguments...
            parsePositionalArguments( context );
        }

        // this exception is thrown when some error occurs during parsing
        catch( ParseException _e ) {

            // return an invalid result, with an explanatory message (bummer!)...
            return new ParsedCommandLine( _e.getMessage() );
        }

        // count our appearances...
        Counts counts = countAppearances( context );

        // return a valid result - hooray!
        return new ParsedCommandLine( context.argumentResults, counts.optionals, counts.positionals );
    }


    private static class ParseContext {

        private final List<ArgParts>         optionalArgs    = new ArrayList<>();
        private final List<String>           positionalArgs  = new ArrayList<>();
        private final Map<String, ParsedArg> argumentResults = new HashMap<>();    // maps argument reference names to argument results
    }

    private static class ArgParts {

        private final String name;
        private final String parameter;


        private ArgParts( final String _name, final String _parameter ) {
            name = _name;
            parameter = _parameter;
        }
    }


    /**
     * Process the given array of command line arguments into (separate) lists of optional and positional arguments.  This handles the special
     * case of the "--" argument, splits optional arguments into name and parameter, and verifies that all optional argument names exist.
     *
     * @param _context The parse context for this operation to use.
     * @param _args The command line arguments.
     */
    private void normalize( final ParseContext _context, final String[] _args ) throws ParseException {

        // some setup...
        boolean allPositional = false;

        // iterate over all our arguments...
        for( String arg : _args ) {

            // do we have a positional argument?
            if( allPositional || (arg.charAt( 0 ) != '-') ) {

                // nothing to check; just add it to our positional arguments list...
                _context.positionalArgs.add( arg );
            }

            // do we have our special case ("--")?
            else if( "--".equals( arg ) ) {

                // switch to all positional mode...
                allPositional = true;
            }

            // do we have a long optional name?
            else if( arg.startsWith( "--" ) ) {

                // get the parts of the argument...
                String[] parts = arg.substring( 2 ).split( "=" );

                // if the length is more than two, we have a problem...
                if( parts.length > 2 )
                    throw new ParseException( "Invalid argument has multiple parameters: " + arg );

                // make sure we know about this name...
                String name = "--" + parts[0];
                if( !nameDefs.containsKey( name ) )
                    throw new ParseException( "Unknown optional argument name: " + name );

                // add it to our optional arguments...
                _context.optionalArgs.add( new ArgParts( name, (parts.length == 2) ? parts[1] : null ) );
            }

            // well, if it's none of the above, then it must be one or more short optional names...
            else {

                // handle the special case of "-" by itself...
                if( "-".equals( arg ) )
                    arg = "--";

                // get the parts of the argument...
                String[] parts = arg.substring( 1 ).split( "=" );

                // if the length is more than two, we have a problem...
                if( parts.length > 2 )
                    throw new ParseException( "Invalid argument has multiple parameters: " + arg );

                // iterate over all the names we captured, validating them and adding them to our optional arguments...
                for( int i = 0; i < parts[0].length(); i++ ) {

                    // get the name we're working on now...
                    String rawName = parts[0].substring( i, i + 1 );

                    // make sure we know about this name...
                    String name = "-" + rawName;
                    if( !nameDefs.containsKey( name ) )
                        throw new ParseException( "Unknown optional argument name: " + name );

                    // add it to our optional arguments...
                    String parameter = (i + 1 >= parts[0].length()) ? ((parts.length == 2) ? parts[1] : null ) : null;
                    _context.optionalArgs.add( new ArgParts( name, parameter ) );
                }
            }
        }
    }


    /**
     * Initialize all the parse results.  This creates a {@link ParsedArg} instance for each defined argument with the correct values for the case
     * where the argument does not appear on the command line.
     *
     * @param _context The parse context for this operation to use.
     * @throws ParseException on the occurrence of any parsing problem
     */
    private void initialize( final ParseContext _context ) throws ParseException {

        // iterate over all the names...
        for( Map.Entry<String, ArgDef> entry : nameDefs.entrySet() ) {

            String name = entry.getKey();
            ArgDef def = entry.getValue();

            // if we have a name with no leading hyphen, then we have a reference name - time to go to work...
            if( name.charAt( 0 ) != '-' ) {

                //if this is an optional argument and there is an absent value specified, resolve the absent value; null otherwise...
                Object value = ((def instanceof OptArgDef) && !isEmpty( ((OptArgDef) def).absentValue ))
                        ? resolveParameterValue( def, ((OptArgDef) def).absentValue )
                        : null ;

                // stuff it away in our results...
                _context.argumentResults.put( name, new ParsedArg( def, value ) );
            }
        }
    }


    /**
     * Resolves the given parameter string into the appropriate value type for the argument with the given definition.  If the parameter is a
     * reference to an environment variable, the value of that variable becomes the parameter value.  If the parameter is a reference to a file, the
     * contents of that file become the parameter value.  If the definition includes a parameter parser, it will be used to convert the given string
     * to correct type for the argument; otherwise the given parameter string is used directly.  If the definition includes a parameter validator, it
     * will be used to validate the parameter.  The resulting value is checked to make sure it is the type defined for the argument.
     *
     * @param _argDef The argument definition for the argument parameter being resolved.
     * @param _parameter The string parameter to resolve.
     * @return the value resulting from resolving the parameter
     * @throws ParseException on the occurrence of any parsing problem
     */
    private Object resolveParameterValue( final ArgDef _argDef, final String _parameter ) throws ParseException {

        // default our parameter and make sure we actually got one...
        String parameter = _parameter;
        if( isEmpty( parameter ) )
            throw new ParseException( "Missing parameter: " + _argDef.helpName );

        // handle translations from environment variables or files...
        parameter = environmentVariableCheck( parameter );
        parameter = filePathCheck( parameter );

        // our parameter IS our value, unless we have a parser...
        Object value = parameter;

        // if we have a parameter parser, use it...
        if( _argDef.parser != null ) {

            // invoke the parser and get the result...
            ParameterParser.Result parseResult = _argDef.parser.parse( parameter );

            // if the parser had problem...
            if( !parseResult.valid ) {
                throw new ParseException( "Parameter parsing problem for '" + _argDef.helpName + "': " + parseResult.message );
            }

            // ok, we've got our new parsed value...
            value = parseResult.value;

            // if the result was of the wrong type, we have a worser problem...
            if( !_argDef.type.isAssignableFrom( value.getClass() ) ) {
                throw new ParseException( "For parameter '" + _argDef.helpName + "', expected parser result of type "
                        + _argDef.type.getSimpleName() + ", got: " + value.getClass().getSimpleName() );
            }
        }

        // if we have a parameter validator, use it...
        if( _argDef.validator != null ) {

            // get our validation result...
            ParameterValidator.Result vr = _argDef.validator.validate( value );
            if( !vr.valid ) {
                throw new ParseException( "On parameter '" + _argDef.helpName + "' with value '" + parameter
                                          + "', got validation error: " + vr.message );
            }
        }

        return value;
    }


    /**
     * Parse the optional arguments present on the command line (and now in the context).
     *
     * @param _context The parse context.
     * @throws ParseException if the given command line cannot be parsed; the message explains why
     */
    private void parseOptionalArguments( final ParseContext _context ) throws ParseException {

        // iterate over all our optional arguments...
        for( ArgParts parts : _context.optionalArgs ) {

            ArgDef def = nameDefs.get( parts.name );   // we've already verified that this argument exists, so no need to check...

            // if we have no parameter, and the parameter is mandatory, see if we can do interactive...
            String parameter = parts.parameter;
            if( (def instanceof OptArgDef) && (def.parameterMode == MANDATORY) && isEmpty( parameter )
                    && (((OptArgDef) def).interactiveMode != InteractiveMode.DISALLOWED)) {

                // get our optional argument definition...
                OptArgDef optDef = (OptArgDef) def;

                // then we'll get the parameter from the user interactively...
                Console console = System.console();

                // if we can get a Console, get the input interactively...
                if( !isNull( console ) ) {

                    // prompt and read the answer...
                    String prompt = isEmpty( optDef.prompt ) ? "Enter value for '" + parts.name + "': " : optDef.prompt;
                    if( optDef.interactiveMode == InteractiveMode.PLAIN ) {
                        parameter = console.readLine( prompt );
                    } else {
                        parameter = new String( console.readPassword( prompt ) );
                    }
                }

                // if we can't get a Console (for example, when running in the IDE), we'll do it the hard way, plain only...
                else {
                    try {
                        System.out.println( "WARNING: cannot get a console in this environment, so using stdout and stdin." );
                        System.out.println( "Passwords will be visible as you type them." );
                        System.out.print( optDef.prompt );
                        BufferedReader br =  new BufferedReader( new InputStreamReader( System.in  ) );
                        parameter = br.readLine();
                    }
                    catch( IOException _e ) {
                        throw new ParseException( "Could not read from console: " + _e.getMessage() );
                    }
                }
            }

            // if we have a parameter, and our parameter is disallowed, we have a problem...
            if( (def.parameterMode == DISALLOWED) && !isEmpty( parameter ) )
                throw new ParseException( "Unexpected parameter for argument '" + parts.name + "': " + parameter );

            // if we have a disallowed parameter, then we have a binary optional argument with an implied value of true...
            if( def.parameterMode == DISALLOWED )
                parameter = "true";

            // if we have no parameter and our parameter is optional, use the default parameter...
            if( (def.parameterMode == OPTIONAL) && isEmpty( parameter ) ) {
                parameter = def.defaultValue;
            }

            // resolve our value...
            Object value = resolveParameterValue( def, parameter );

            // get our updated parse results for this argument...
            ParsedArg result = _context.argumentResults.get( def.referenceName ).add( value );

            // if we've gotten too many of this particular argument, barf...
            if( (def.maxAllowed > 0) && (result.appearances > def.maxAllowed) )
                throw new ParseException( "Maximum count (" + def.maxAllowed + ") of '" + parts.name + "' exceeded." );

            // stuff it away...
            _context.argumentResults.put( def.referenceName, result );
        }
    }


    /**
     * Parse the positional arguments present on the command line (and now in the context).
     *
     * @param _context The parse context.
     * @throws ParseException if the given command line cannot be parsed; the message explains why
     */
    private void parsePositionalArguments( final ParseContext _context ) throws ParseException {

        // our positional argument index (pay attention: this is important!)...
        int pai = 0;

        // iterate over all our positional argument definitions...
        for( int pdi = 0; pdi < positionalDefs.size(); pdi++ ) {

            // get the positional argument definition we're working on...
            PosArgDef def = positionalDefs.get( pdi );

            // if we have a non-unitary argument here, then we have to figure out how many positional arguments to consume here...
            int eatArgs = 1;  // assume we're going to consume 1, the usual case...
            if( def.isNotUnitary() ) {

                // good thing that computers are good at math...
                int argsRemaining = (_context.positionalArgs.size() - pai);
                int unitaryDefinitionsRemaining = (positionalDefs.size() - (pdi + 1));
                eatArgs =  argsRemaining - unitaryDefinitionsRemaining;

                // if we have no arguments to eat and this is a mandatory argument, or if we have negative arguments to eat, oopsie...
                if( ((eatArgs == 0) && (def.parameterMode == MANDATORY)) || (eatArgs < 0) )
                    throw new ParseException( "Mandatory positional argument missing" );
            }

            // if eatArgs == 0, then we have an optional parameter with no argument, so use the absent value...
            if( eatArgs == 0 ) {
                resolveAndAddValue( _context, def, def.defaultValue );
            }

            // otherwise, eat the appropriate number of arguments...
            else {
                for( ; eatArgs > 0; eatArgs-- ) {

                    // if we've run out of arguments, let the user know about their sad, sad situation...
                    if( pai >= _context.positionalArgs.size() )
                        throw new ParseException( "Missing one or more mandatory positional arguments" );

                    // add the next value...
                    resolveAndAddValue( _context, def, _context.positionalArgs.get( pai ) );

                    // bump our argument index...
                    pai++;
                }
            }
        }

        // if we have arguments left over, then the user gave us too many...
        if( pai < _context.positionalArgs.size() )
            throw new ParseException( "Too many positional arguments" );
    }


    /**
     * Resolve the given parameter into a value for the argument in the given definition, updating the results in the context.
     *
     * @param _context The parse context.
     * @param _def The definition of the argument whose parameter is being resolved.
     * @param _parameter The parameter being resolved.
     * @throws ParseException if the maximum allowed number of occurrences has been exceeded
     */
    private void resolveAndAddValue( final ParseContext _context, final ArgDef _def, final String _parameter ) throws ParseException {

        // resolve our value...
        Object value = resolveParameterValue( _def, _parameter );

        // get our updated parse results for this argument...
        ParsedArg result = _context.argumentResults.get( _def.referenceName ).add( value );

        // if we've gotten too many of this particular argument, barf...
        if( (_def.maxAllowed > 0) && (result.appearances > _def.maxAllowed) )
            throw new ParseException( "Maximum count (" + _def.maxAllowed + ") of '" + _def.helpName + "' exceeded." );

        // stuff it away...
        _context.argumentResults.put( _def.referenceName, result );
    }


    /**
     * Count the total appearances of all optional arguments and (separately) all positional arguments.
     *
     * @return a tuple with the optional and positional appearance counts
     */
    private Counts countAppearances( final ParseContext _context ) {

        Counts counts = new Counts();

        _context.argumentResults.forEach( (name, result) -> {
            if( result.argumentDefinition instanceof OptArgDef )
                counts.optionals += result.appearances;
            if( result.argumentDefinition instanceof PosArgDef )
                counts.positionals += result.appearances;
        } );

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
     * Checks to see if the given parameter string specifies an environment variable (by being surrounded with percent ("%") symbols), in which
     * case the value of the environment variable becomes the parameter.  If a non-existent environment variable is specified, the empty string
     * is returned.
     *
     * @param _parameter The parameter string to check and (possibly) replace.
     * @return the possibly modified parameter string
     */
    private String environmentVariableCheck( final String _parameter ) {

        // if we have an environment variable name surrounded by percent signs, extract it...
        if( (_parameter.length() > 2) && (_parameter.charAt( 0 ) == '%') && (_parameter.charAt( _parameter.length() - 1 ) == '%') ) {
            String env = System.getenv( _parameter.substring( 1, _parameter.length() - 1 ) );
            return isNull( env ) ? "" : env;
        }

        // otherwise, just return what we got...
        return _parameter;
    }


    /**
     * Checks to see if the given parameter string specifies a file (by being surrounded with hash ("#") symbols), in which case the contents of the
     * file becomes the parameter.  If the file can't be found or can't be read, the empty string is returned.
     *
     * @param _parameter The parameter string to check and (possibly) replace.
     * @return the possibly modified parameter string
     */
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
     * Add the given {@link ArgDef} to this command line definition.  Optional argument definitions appear in the help in the order they are added.
     * Positional arguments <i>must</i> be added in left-to-right order they appear on the command line.
     *
     * @param _argDef The {@link ArgDef} to add to this command line definition.
     */
    public void add( final ArgDef _argDef ) {

        // fail fast if some dummy called us with a null...
        Objects.requireNonNull( _argDef, "Cannot add a null argument definition");

        // make sure we've got either a positional or an optional argument definition - nothing else will do...
        if( !((_argDef instanceof OptArgDef) || (_argDef instanceof PosArgDef)) )
            throw new IllegalArgumentException( "_argDef must be instance of either OptArgDef or PosArgDef" );

        // stuff it away by reference name, checking for duplication...
        addName( _argDef.referenceName, _argDef );

        // if we've got a positional argument...
        if( _argDef instanceof PosArgDef ) {

            PosArgDef posDef = (PosArgDef) _argDef;

            // if we have a parameter mode of DISALLOWED, that's not allowed...
            if( posDef.parameterMode == ParameterMode.DISALLOWED )
                throw new IllegalArgumentException( "Tried to add a positional argument with a DISALLOWED parameter mode" );

            // if this argument can appear a variable number of times, make sure we're not trying to do more than one of these...
            if( posDef.isNotUnitary() ) {

                // scan existing positional argument definitions to see if any of them are non-unitary...
                for( PosArgDef def : positionalDefs ) {
                    if( def.isNotUnitary() )
                        throw new IllegalArgumentException( "Tried to add a second positional arguments with variable number of appearances: '"
                                + posDef.helpName + "' and '" + def.helpName + "'" );
                }
            }
            positionalDefs.add( posDef );
        }

        // if we've got an optional argument...
        else /* MUST be an instance of OptArgDef */ {

            OptArgDef optionalArgDef = (OptArgDef) _argDef;

            // stuff it away by short name(s), checking for duplicates...
            for( String shortName : optionalArgDef.shortNames ) {
                addName( "-" + shortName, optionalArgDef );
            }

            // stuff it away by long names, checking for duplicates...
            for( String longName : optionalArgDef.longNames ) {
                addName( "--" + longName, _argDef );
            }

            // stuff it away in order, for help production...
            optionalDefs.add( optionalArgDef );
        }
    }


    /**
     * Adds the given name (which could be an argument reference name, short optional argument name, or long optional argument name) to the name
     * definitions, checking for duplicates.
     *
     * @param _name The name to add to the name definitions.
     * @param _def The argument definition the given name belongs to.
     * @throws IllegalArgumentException if the given name is a duplicate
     */
    private void addName( final String _name, final ArgDef _def ) {

        if( nameDefs.containsKey( _name ) ) {
            if( _def.referenceName.equals( _name ) )
                throw new IllegalArgumentException( "Duplicate argument reference name: " + _name );
            throw new IllegalArgumentException( "Duplicate optional argument name '" + _name
                                                + "' in argument with reference name: " + _def.referenceName );
        }
        nameDefs.put( _name, _def );
    }


    /**
     * Returns the length of the longest option name line.
     *
     * @return the length of the longest option name line
     */
    @SuppressWarnings( "unused" )
    public int getOptionalCommandWidth() {

        int length = 0;
        for( OptArgDef def : optionalDefs ) {
            int argLength = def.getArgumentDescription().length();
            if( argLength > length )
                length = argLength;
        }
        return length;
    }


    /**
     * Returns a summary help string, suitable for display on a console.
     *
     * @return a summary help string
     */
    public String getSummaryHelp() {

        StringBuilder sb = new StringBuilder();

        // emit a pro forma command line and description...
        TextFormatter tf = new TextFormatter( width, 0, 0, 2 );
        tf.add( getProFormaCommandLine() );
        tf.add( "//BR//" + summary );
        sb.append( tf.getFormattedText() );

        // handle the positional arguments...
        sb.append( System.lineSeparator() );
        for( PosArgDef def : positionalDefs ) {
            sb.append( def.getArgumentDescription() );
            sb.append( System.lineSeparator() );
            tf = new TextFormatter( width, indent, 0, 2 );
            tf.add( def.summary );
            sb.append( tf.getFormattedText() );
        }

        // handle the optional arguments...
        sb.append( System.lineSeparator() );
        for( OptArgDef def : optionalDefs ) {
            sb.append( def.getArgumentDescription() );
            sb.append( System.lineSeparator() );
            tf = new TextFormatter( width, indent, 0, 2 );
            tf.add( def.summary );
            sb.append( tf.getFormattedText() );
        }

        return sb.toString();
    }


    /**
     * Returns a detailed help string, suitable for display on a console.
     *
     * @return a detailed help string
     */
    public String getDetailedHelp() {

        StringBuilder sb = new StringBuilder();

        // emit a pro forma command line and description...
        TextFormatter tf = new TextFormatter( width, 0, 0, 2 );
        tf.add( getProFormaCommandLine() );
        tf.add( "//BR//" + detail );
        sb.append( tf.getFormattedText() );

        // handle the positional arguments...
        sb.append( System.lineSeparator() );
        sb.append( "Positional arguments:" );
        sb.append( System.lineSeparator() );
        for( PosArgDef def : positionalDefs ) {
            sb.append( System.lineSeparator() );
            sb.append( def.getArgumentDescription() );
            sb.append( System.lineSeparator() );
            tf = new TextFormatter( width, indent, 0, 2 );
            tf.add( def.detail );
            sb.append( tf.getFormattedText() );
        }

        // handle the optional arguments...
        sb.append( System.lineSeparator() );
        sb.append( "Optional arguments:" );
        sb.append( System.lineSeparator() );
        for( OptArgDef def : optionalDefs ) {
            sb.append( System.lineSeparator() );
            sb.append( def.getArgumentDescription() );
            sb.append( System.lineSeparator() );
            tf = new TextFormatter( width, indent, 0, 2 );
            tf.add( def.detail );
            sb.append( tf.getFormattedText() );
        }

        return sb.toString();
    }


    /**
     * Return a <i>pro forma</i> command line for use in help.
     *
     * @return the <i>pro forma</i> command line
     */
    private String getProFormaCommandLine() {
        StringBuilder sb = new StringBuilder();
        sb.append( name );
        if( optionalDefs.size() > 0 ) {
            sb.append( " [optional arguments]" );
        }
        for( PosArgDef def : positionalDefs ) {
            sb.append( " " );
            sb.append( def.getArgumentDescription() );
        }
        return sb.toString();
    }


    /**
     * Return the name of the command whose command line is defined in this instance.
     *
     * @return the name of the command
     */
    public String getName() {
        return name;
    }


    /**
     * Return the summary help for the command whose command line is defined in this instance.
     *
     * @return the summary help.
     */
    public String getSummary() {
        return summary;
    }


    /**
     * Return the detailed help for the command whose command line is defined in this instance.
     *
     * @return the detailed help.
     */
    public String getDetail() {
        return detail;
    }


    /**
     * An exception used internally to simply handling of parsing problems.
     */
    private static class ParseException extends Exception {

        private ParseException( final String _msg ) {
            super( _msg );
        }
    }
}
