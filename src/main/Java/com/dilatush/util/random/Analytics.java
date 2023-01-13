package com.dilatush.util.random;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Static container class for methods that implement various tests of randomness.  The tests themselves are shamelessly copied from the excellent (but not Java!) Diehard
 * program.  See this <a href="https://en.wikipedia.org/wiki/Diehard_tests">Wikipedia article</a> for more details.
 */
public class Analytics {

    public static int BIRTHDAY_SPACINGS_DEFAULT_DAYS_POWER    = 24;  // default is 2^24...
    public static int BIRTHDAY_SPACINGS_DEFAULT_SAMPLES_POWER = 9;   // default is 2^9...


    /**
     *
     * @param _source
     * @param _days
     * @param _samples
     * @return
     */
    public static double birthdaySpacings( final Randomish _source, final int _days, final int _samples ) {

        // sanity checks...
        if( (_days < 0) || (_days > 31) ) throw new IllegalArgumentException( "_days must be in [0..31]" );
        if( (_samples <= 0) || (_samples > 31) ) throw new IllegalArgumentException( "_samples must be in [0..31]" );

        // generate the constants we'll need...
        var numSamples  = 1 << _samples;
        var numDays     = 1 << _days;
        var numPasses   = 33 - _days;
        var sampleData  = new int[numSamples];
        for( int i = 0; i < numSamples; i++ ) {
            sampleData[i] = _source.nextInt();
        }


        // some loop state setup...
        var mask = ~0 << (numPasses - 1);

        // loop through all the passes we're going to make...
        for( int i = 0; i < numPasses; i++ ) {

            // collect some "birthday" samples...
            var samples = new int[numSamples];
            for( int s = 0; s < numSamples; s++ ) {
                samples[s] = ((sampleData[s] & mask) >>> Integer.numberOfTrailingZeros( mask ));
            }

            // compute all the spacings between the "birthdays"...
            var spacings = new int[numSamples * (numSamples - 1) / 2];
            var s = 0;
            for( int s1 = 0; s1 < numSamples; s1++ ) {
                for( int s2 = s1+1; s2 < numSamples; s2++ ) {
                    spacings[s++] = Math.abs( samples[s1] - samples[s2] );
                }
            }

            // find all the spacings with multiple hits...
            var hits = new ArrayList<Spacing>( 500 );
            Arrays.sort( spacings );
            for( int s1 = 0; s1 < spacings.length;  ) {
                var s2 = s1;
                var sc = 0;
                do {
                    sc++;
                } while( (++s2 < spacings.length) && (spacings[s1] == spacings[s2]));
                if( sc > 1 )
                    hits.add( new Spacing( spacings[s1], sc ) );
                s1 += sc;
            }

            hits.sort( ( o1, o2 ) -> o2.count - o1.count );

            new Object().hashCode();
        }

        return 0;
    }


    private record Spacing( int spacing, int count ) {}


    public static double birthdaySpacings( final Randomish _source ) {
        return birthdaySpacings( _source, BIRTHDAY_SPACINGS_DEFAULT_DAYS_POWER, BIRTHDAY_SPACINGS_DEFAULT_SAMPLES_POWER );
    }


    public static void main( String[] _arg ) {
        var r = new XORShift32( 0, 0, 1 );
        var x = birthdaySpacings( r, 24, 11 );
    }
}
