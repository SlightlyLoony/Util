//package com.dilatush.util.random;
//
//import java.math.BigInteger;
//import java.util.HashSet;
//import java.util.Set;
//
//import static com.dilatush.util.General.isNull;
//
///**
// * Instances of this class "mix" two or more {@link Randomish} pseudorandom sequences together to produce a high quality pseudorandom sequence with a longer cycle length.  The
// * mixing is performed by XORing the {@link #nextInt()} result from each {@link Randomish} source to produce the next {@link #nextInt()} output from an instance of this class.
// */
//public class XORMixer implements Randomish {
//
//    private final CycleLength cycleLength;
//
//
//    /**
//     * Create a new instance of this class using the given sources.  At least two sources must be provided.
//     *
//     * @param _sources
//     */
//    public XORMixer( final Randomish... _sources ) {
//
//        // sanity checks...
//        if( isNull( (Object) _sources ) ) throw new IllegalArgumentException( "_source may not be null" );
//        if( _sources.length < 2 ) throw new IllegalArgumentException( "at least two _sources must be supplied" );
//
//        // analyze the sources, so we know how to calculate the length...
//        Set<BigInteger> uniqueCycleLengths = new HashSet<>();   // keep track of the unique 'exact' or 'at least' cycle lengths...
//        boolean isInfinite = false;
//        boolean isUnknown  = false;
//        boolean isAtLeast  = false;
//        boolean isExactly  = false;
//        for( Randomish source : _sources ) {
//
//            // note the form of this source's cycle length...
//            isInfinite = isInfinite || (source.cycleLength().form() == CycleLengthForm.INFINITE );
//            isUnknown  = isUnknown  || (source.cycleLength().form() == CycleLengthForm.UNKNOWN  );
//            isAtLeast  = isAtLeast  || (source.cycleLength().form() == CycleLengthForm.AT_LEAST );
//            isExactly  = isExactly  || (source.cycleLength().form() == CycleLengthForm.EXACTLY  );
//
//            // if this source is not infinite or unknown, record the length (but only once for each length!)...
//            if( source.cycleLength().hasLength() ) {
//                uniqueCycleLengths.add( source.cycleLength().cycleLength() );
//            }
//        }
//
//        // use the analysis we just did to figure out the cycle length for this mixer...
//        if( isInfinite )
//            cycleLength = new CycleLength( CycleLengthForm.INFINITE );
//        else if( isUnknown && !(isExactly || isAtLeast) )
//            cycleLength = new CycleLength( CycleLengthForm.UNKNOWN );
//        else if( isExactly ) {
//
//        }
//    }
//
//
//    /**
//     * Returns the next integer in the random or pseudorandom sequence provided by this instance.
//     *
//     * @return The next integer in the random or pseudorandom sequence.
//     */
//    @Override
//    public int nextInt() {
//
//        return 0;
//    }
//
//
//    /**
//     * <p>Returns the cycle length of the random or pseudorandom sequence provided by this instance.  Note that the result may be the exact length, a minimum length, an infinite
//     * length (for a source than never repeats a cycle), or even an unknown length (for a source that repeats cyclically but where the length of the cycle is not computable).<p>
//     * <p>Cycle length is defined somewhat arbitrarily as the number of invocations of {@link #nextInt()} between the start
//     * of a pattern of 10 integers and the start of the next repetition of those same 10 integers</p>.
//     *
//     * @return the cycle length of the random or pseudorandom sequence provided by this instance.
//     */
//    @Override
//    public CycleLength cycleLength() {
//
//        return null;
//    }
//}
