package com.dilatush.util.test;

import java.util.Map;

/**
 * Implements a {@link TestEnabler} with a configurable starting value for {@link #isEnabled()}, and keeps that value for the configured number of
 * {@link #isEnabled()} invocations.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CountedTestEnabler extends ATestEnabler {

    private final boolean startAs;
    private final int maxCount;

    private int count;


    /**
     * Creates a new instance of this class with the given properties.  There are two properties that are used by this instance, if present:
     * <ul>
     *     <li><code>_maxCount_</code> is the number of times that {@link #isEnabled()} will return the starting value before changing to the
     *     opposite value.</li>
     *     <li><code>_startAs_</code> is the starting state of this instance.  If this value is <code>true</code> (which is the default value if this
     *     property is not supplied), then this instance is enabled for the configured number of invocations, then disabled afterwards.  If this value
     *     is <code>false</code>, then this instance is disabled for the configured number of invocations, then enabled afterwards.</li>
     * </ul>
     * If other properties are present, they are ignored by this instance but are available publicly (and in particular, to the software being
     * tested).
     *
     * @param _properties the properties for this instance.
     */
    public CountedTestEnabler( final Map<String, Object> _properties ) {
        super( _properties );

        String MAX_COUNT = "_maxCount_";
        String START_AS  = "_startAs_";

        maxCount = properties.containsKey( MAX_COUNT ) ? getAsInt( MAX_COUNT ) : 1000;
        startAs  = !properties.containsKey( START_AS ) || getAsBoolean( START_AS );
    }


    /**
     * Initializes this test enabler.  This happens at two different times:
     * <ul>
     *     <li>Upon instantiation.</li>
     *     <li>When this instance is one component of a {@link CompositeTestEnabler} instance, and the preceding component's enabled state changes
     *     from enabled to disabled.</li>
     * </ul>
     */
    @Override
    public void init() {
        count = 0;
    }


    /**
     * Returns <code>true</code> if this instance is currently enabled.
     *
     * @return <code>true</code> if this instance is currently enabled
     */
    @Override
    protected boolean enabled() {
        if( count >= maxCount )
            return !startAs;
        count++;
        return startAs;
    }
}
