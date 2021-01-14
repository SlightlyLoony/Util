package com.dilatush.util.cli;

import com.dilatush.util.cli.argdefs.ArgDef;
import com.dilatush.util.cli.argdefs.OptArgDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dilatush.util.General.isNull;

/**
 * Simple POJO that contains the value of a defined argument, optional or positional.  Instances of this class are immutable and threadsafe.
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
     * The parameter value of this argument.  There are several possibilities for its value:
     * <ul>
     *     <li>For optional arguments that <i>cannot</i> have a parameter (boolean arguments), the value is a {@link Boolean} instance,
     *     <code>true</code> if the argument appeared at least once, and <code>false</code> otherwise.</li>
     *     <li>For arguments with an <i>optional</i> parameter, if the parameter is not supplied on the command line, then the value is determined by
     *     the {@link ArgDef#defaultValue} and {@link ArgDef#parser}in its definition.</li>
     *     <li>For optional arguments that don't appear on the command line, the value is determined by the {@link OptArgDef#absentValue} and
     *     {@link ArgDef#parser}in its definition.</li>
     *     <li>For arguments with parameters that appear multiple times on the command line, the value is determined by the last argument parsed
     *     (the rightmost argument).</li>
     * </ul>
     */
    public final Object value;


    /**
     * The list of the parameter values of this argument.  This field is relevant mainly for arguments (optional or positional) that may appear
     * multiple times on the command line.  For those arguments, their parameter values are in this list, in left-to-right order of their appearance
     * on the command line.
     */
    public final List<Object> values;


    /**
     * The number of times this argument appeared on the command line (may be zero or greater, though usually is zero or one).  This is mainly useful
     * for optional arguments that can appear multiple times (such as <code>-vvv</code> for more verbosity), and for positional arguments that could
     * be globbed filenames.
     */
    public final int appearances;


    /**
     * The argument definition for this argument.
     */
    public final ArgDef argumentDefinition;


    /**
     * Creates a new instance of this class with the given values.
     *
     * @param _present The present value.
     * @param _value The value value.
     * @param _values The values value.
     * @param _appearances The appearances value.
     * @param _argumentDefinition The argument definition.
     */
    private ParsedArg( final boolean _present, final Object _value, final List<Object> _values,
                      final int _appearances, final ArgDef _argumentDefinition ) {
        present = _present;
        value = _value;
        values = _values;
        appearances = _appearances;
        argumentDefinition = _argumentDefinition;
    }


    /**
     * Create a new instance of this class with the given argument definition and value.  If the given value is {@code null} , both the {@link #value}
     * and  {@link #values} fields will be {@code null}; otherwise the {@link #value} field is set to the given value, and the {@link #values} field
     * is set to an immutable list with a single element equal to the given value.  The resulting object will have the {@link #present} field equal
     * to {@code null}, the {@link #appearances} field equal to zero, and the {@link #argumentDefinition} field set to the given argument definition.
     *
     * @param _argDef The argument definition for this argument.
     * @param _value The value (generally the absent value) for this field.
     */
    public ParsedArg( final ArgDef _argDef, final Object _value ) {
        this( false, _value, initValues( _value), 0, _argDef );
    }


    /**
     * Returns a new instance of {@link ParsedArg} indicating the argument was present on the command line with the given parameter value.  If this
     * instance was not present (thus indicating that this invocation is marking the first presence), then the existing value is replaced with the
     * new one.  Otherwise, the new value is added to the existing values.  In all cases, the number of appearances is incremented.
     *
     * @param _value The parameter value to add.
     * @return the new instance of {@link ParsedArg}
     */
    public ParsedArg add( final Object _value ) {

        // if this is the first appearance, we need to replace the value...
        if( !present ) {
            return new ParsedArg( true, _value, initValues( _value ), 1, argumentDefinition );
        }

        // otherwise we add the value...
        List<Object> list = new ArrayList<>( values );
        list.add( _value );
        return new ParsedArg( true, _value, Collections.unmodifiableList( list ), appearances + 1, argumentDefinition );
    }


    /**
     * Returns a newly instantiated immutable list containing one element with the given value.
     *
     * @param _value The value to put in the list.
     * @return the newly instantiated immutable list
     */
    private static List<Object> initValues( final Object _value ) {

        if( isNull( _value ) )
            return null;

        List<Object> list = new ArrayList<>();
        list.add( _value );
        return Collections.unmodifiableList( list );
    }
}
