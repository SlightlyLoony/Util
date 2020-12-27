package com.dilatush.util.test;

import java.util.Map;
import java.util.Random;

/**
 * Implements a {@link TestEnabler} that returns a random value from invocations of {@link #isEnabled()}, with a configured fraction of those values
 * (over a long enough period) being equal to the configured <code>_enabledFraction_</code>.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class RandomTestEnabler extends ATestEnabler {

    private final Random random;
    private final double enabledFraction;


    /**
     * Creates a new instance of this class with the given properties.  There are two properties that are used by this instance, if present:
     * <ul>
     *     <li><code>_seed_</code> is the (long) seed for the random generator used by this instance.  If this property is supplied, then the same
     *     set of random values will be generated for every test run.  If this property is not supplied, it defaults to the value of
     *     {@link System#currentTimeMillis()} at the time this instance is created (which means that every test run will have a different
     *     set of random values controlling its behavior).</li>
     *     <li><code>_enabledFraction_</code> is a value in the range [0..1] that determines the fraction of the time that this instance will
     *     be enabled.  For example, if this value is 0.25, then 25% of the time (on average), this instance will be enabled.  Values smaller than
     *     zero are treated as zero; values greater than one are treated as one.</li>
     * </ul>
     * If other properties are present, they are ignored by this instance but are available publicly (and in particular, to the software being
     * tested).
     *
     * @param _properties the properties for this instance.
     */
    public RandomTestEnabler( final Map<String, Object> _properties ) {
        super( _properties );
        String SEED = "_seed_";
        String ENABLED_FRACTION = "_enabledFraction_";
        long seed = properties.containsKey( SEED ) ? getAsLong( SEED ) : System.currentTimeMillis();
        enabledFraction = properties.containsKey( ENABLED_FRACTION ) ? Math.max( 0, Math.min( 1, getAsDouble( ENABLED_FRACTION ) ) ) : 0.5;
        random = new Random( seed );
    }


    @Override
    protected boolean enabled() {
        return random.nextDouble() < enabledFraction;
    }
}
