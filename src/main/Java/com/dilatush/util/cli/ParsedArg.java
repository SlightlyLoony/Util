package com.dilatush.util.cli;

import java.util.List;

/**
 * Simple immutable POJO that contains the result of parsing a single argument on the command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ParsedArg {


    /**
     * This field is <code>true</code> if this argument was present on the parsed command line at least once, and <code>false</code> if this
     * argument didn't appear on the command line at all.
     */
    public final boolean present;

    /**
     * The value of this argument.  There are several possibilities for its value:
     * <ul>
     *     <li>For arguments that <i>cannot</i> have a parameter (boolean arguments), the value is a {@link Boolean} instance, <code>true</code> if
     *         the argument appeared at least once, and <code>false</code> otherwise.</li>
     *     <li>For arguments that can only have a single <i>optional</i> parameter, if the parameter is not supplied the value is <code>null</code>;
     *         otherwise the value is an object of the class defined in the argument's {@link ArgDef}.</li>
     *     <li>For arguments that can only have a single <i>mandatory</i> parameter, the value is an object of the class defined in the argument's
     *         {@link ArgDef}.</li>
     *     <li>For arguments that can have multiple parameters, whether <i>optional</i> or <i>mandatory</i>, the value is a {@link List} instance
     *         with zero or more elements, each element is an object of the class defined in the argument's {@link ArgDef}.  Note that this means
     *         that if this argument did not appear on the command line parsed, then the value will be an empty list, not a <code>nul.</code>.</li>
     * </ul>
     */
    public final Object value;


    /**
     * The number of times this argument appeared on the command line (may be zero or greater, though usually is zero or one).  This is mainly useful
     * for optional arguments that can appear multiple times (such as <code>-vvv</code> for more verbosity), and for positional arguments that could
     * be globbed filenames.
     */
    public final int appearances;


    /**
     * Creates a new instance of this class with the given values.
     *
     * @param _present Whether this argument was present (<code>true</code>) on the command line.
     * @param _value The value of this argument.
     * @param _appearances The number of times this argument appeared on the command line.
     */
    public ParsedArg( final boolean _present, final Object _value, final int _appearances ) {
        present = _present;
        value = _value;
        appearances = _appearances;
    }
}
