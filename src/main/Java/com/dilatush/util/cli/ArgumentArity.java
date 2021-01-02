package com.dilatush.util.cli;

/**
 * Instances of this class describe the arity of an argument, or how many times it may appear on the command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ArgumentArity {


    /**
     * Arity for an optional argument that may appear at most one time.
     */
    public static final ArgumentArity OPTIONAL_SINGLE = new ArgumentArity( 0, 1 );


    /**
     * Arity for a mandatory argument that must appear exactly once.
     */
    public static final ArgumentArity MANDATORY_SINGLE = new ArgumentArity( 1, 1 );


    /**
     * Arity for an optional argument that may appear any number of times.
     */
    public static final ArgumentArity OPTIONAL_UNLIMITED = new ArgumentArity( 0, 0 );


    /**
     * Arity for a mandatory argument that must appear at least once, but can appear any number of times.
     */
    public static final ArgumentArity MANDATORY_UNLIMITED = new ArgumentArity( 1, 0 );

    /**
     * The minimum number of times that an argument may appear on the command line.  If this field is zero, then the argument may be left off the
     * command line altogether.  If this field is one, then the argument <i>must</i> appear on the command line at least once.  These two values
     * are by far the most common.
     */
    public final int minimum;

    /**
     * The maximum number of times that an argument may appear on the command line.  The value zero is a special case that means, essentially, that
     * the argument may appear any number of times.  The most common value is one, which means that the argument may appear on the command line at
     * most one time.
     */
    public final int maximum;


    /**
     * Creates a new instance of this class with the given minimum and maximum numbers of times that an argument may appear on the command line.
     *
     * @param _minimum The minimum number of times the argument may appear on the command line.  This value must be non-negative.
     * @param _maximum The maximum number of times the argument may appear on the command line.  This value must be non-negative.
     */
    public ArgumentArity( final int _minimum, final int _maximum ) {

        if( _minimum < 0 )
            throw new IllegalArgumentException( "Illegal value for minimum argument arity: " + _minimum );

        if( _maximum < 0 )
            throw new IllegalArgumentException( "Illegal value for maximum argument arity: " + _maximum );

        if( (_maximum != 0) && (_maximum < _minimum) )
            throw new IllegalArgumentException( "Maximum arity may not be non-zero and less than minimum" );

        minimum = _minimum;
        maximum = _maximum;
    }


    /**
     * Returns <code>true</code> if the maximum arity is unlimited.
     *
     * @return <code>true</code> if the maximum arity is unlimited
     */
    public boolean isUnlimited() {
        return maximum == 0;
    }


    /**
     * Returns <code>true</code> if this arity allows a variable number of arguments.  More formally, returns <code>true</code> if {@link #maximum}
     * is non-zero, and {@link #minimum} does not equal {@link #maximum}.
     *
     * @return <code>true</code> if this arity allows a variable number of arguments
     */
    public boolean isVariable() {
        return (maximum > 0) && (minimum != maximum);
    }
}
