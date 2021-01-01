package com.dilatush.util.cli;

import java.util.List;

/**
 * Simple POJO that contains the result of parsing a single argument on the command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ParsedArg {


    /**
     * This field is <code>true</code> if this argument was present on the parsed command line at least once, and <code>false</code> if this
     * argument didn't appear on the command line at all.
     */
    public boolean present;

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
    public Object value;


    /**
     * The number of times this argument appeared on the command line (may be zero or greater, though usually is zero or one).  This is mainly useful
     * for optional arguments that can appear multiple times (such as <code>-vvv</code> for more verbosity), and for positional arguments that could
     * be globbed filenames.
     */
    public int appearances;
}
