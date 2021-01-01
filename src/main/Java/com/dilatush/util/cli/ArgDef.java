package com.dilatush.util.cli;

/**
 * Instances of this class describe a command line optional parameter.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings("rawtypes")
public class ArgDef {

    // these fields apply to argument definitions of any type...
    public final ArgumentType      argType;           // the type of argument (optional or positional)
    public final String            referenceName;     // the internal reference name for this argument
    public final String            summary;           // a short description of this option
    public final String            description;       // an arbitrary length description of this option
    public final ArgumentArity     arity;             // the (allowable) arity of this argument

    // these fields pertain to the argument's value...
    public final Class             type;              // the type of the option's value
    public final ArgumentValidator validator;         // the validator for this option
    public final ArgumentParser    parser;            // the parser for this option

    // these fields apply only to Optional argument definitions...
    public final char[]            shortOptionNames;  // all the short (one character) names for this option
    public final String[]          longOptionNames;   // all the long (one or more characters) names for this option

    // these fields apply only to Positional argument definitions...
    public final int               index;             // the zero-based index of this argument's position on the command line


    /**
     * Create a new instance of this class that defines an optional argument.
     *
     * @param _referenceName The name used internally to refer to this argument.  The name must be unique across all arguments.
     * @param _summary A summary description of this argument (used in the summary help).
     * @param _description A detailed description of this argument (used in the detailed help).
     * @param _arity The arity of this argument.
     * @param _type The class object for the type of this argument's value.
     * @param _validator The validator for this argument.
     * @param _parser The parser for this argument, which translates the command line string parameter to a Java value.
     * @param _shortOptionNames The short (single character) names of this argument on the command line.
     * @param _longOptionNames The long (string) names of this argument on the command line.
     */
    public ArgDef( final String _referenceName, final String _summary, final String _description, final ArgumentArity _arity,
                   final Class _type, final ArgumentValidator _validator, final ArgumentParser _parser, final char[] _shortOptionNames,
                   final String[] _longOptionNames ) {
        argType = ArgumentType.OPTIONAL;
        referenceName = _referenceName;
        summary = _summary;
        description = _description;
        arity = _arity;
        type = _type;
        validator = _validator;
        parser = _parser;
        shortOptionNames = _shortOptionNames;
        longOptionNames = _longOptionNames;
        index = 0;
    }


    /**
     * Create a new instance of this class that defines a positional argument.  The <code>_index</code> parameter have positive or negative values.
     * Positive values indicate positions starting at the left side (0 = first position, 1 = second position, etc.).  Negative values indicate
     * positions as counted from the right side (-1 = first position from the right, -2 = second position from the right, etc.).  The reason for
     * having the negative indexes is to handle the case of a positional arguments with arities other than {1,1}:
     * <ul>
     *     <li>The simplest case is that all the positional arguments have arities of {n,m} where n > 0 and n = m.  In this scenario there are no
     *     ambiguities about which position corresponds to which argument - every argument must appear exactly n tiems at its indexed position.  In
     *     this case, all positional arguments should have positive indexes.</li>
     *     <li>It's possible for a positional argument to have an arity like {0,1} or {0,0}.  In these cases, if that argument is the last
     *     positional argument, then there still is no ambiguity - so long as there is at most one such positional argument.  Positive indexes can
     *     still be used for all the positional arguments.  However, consider the case of three positional arguments, with arities of {1,1}, {1,0},
     *     and {1,1} (from left to right).  In this case the middle argument may appear on the command line any number of times.  A positive index
     *     of 0 works for the first argument, and an index of 1 for the second argument - but what index to use for the rightmost one?  It can't be
     *     a positive index because we can't know (when we're defining the arguments) how many appearances there will be for the middle argument.
     *     That's the situation that the negative indexes address; in this example the rightmost argument should be specified with an index of
     *     -1.  Note that in this case, just like the previous case, all this only works if there is no more than one positional argument with an
     *     arity other than {n,m} where n > 0 and n = m.  Attempts to define multiple positional arguments that don't meet that test will cause
     *     an exception.</li>
     * </ul>
     *
     * @param _referenceName The name used internally to refer to this argument.  The name must be unique across all arguments.
     * @param _summary A summary description of this argument (used in the summary help).
     * @param _description A detailed description of this argument (used in the detailed help).
     * @param _arity The arity of this argument.
     * @param _type The class object for the type of this argument's value.
     * @param _validator The validator for this argument.
     * @param _parser The parser for this argument, which translates the command line string parameter to a Java value.
     * @param _index The zero-based index of the position of this argument on the command line.
     */
    public ArgDef( final String _referenceName, final String _summary, final String _description, final ArgumentArity _arity, final Class _type,
                   final ArgumentValidator _validator, final ArgumentParser _parser, final int _index ) {

        argType = ArgumentType.POSITIONAL;
        referenceName = _referenceName;
        summary = _summary;
        description = _description;
        arity = _arity;
        type = _type;
        validator = _validator;
        parser = _parser;
        shortOptionNames = null;
        longOptionNames = null;
        index = _index;
    }
}
