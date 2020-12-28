package com.dilatush.util.test;

import java.util.Map;

/**
 * Implements a {@link TestEnabler} that provides a two-phase periodic (repeating) enabler.  In the first phase, the {@link #isEnabled()} result is
 * equal to a configurable starting value for a configurable time (in milliseconds).  In the second phase, also of a configurable length, the
 * {@link #isEnabled()} result is the opposite.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class PeriodicTestEnabler extends ATestEnabler {

    private final boolean startAs;
    private final long phase1;
    private final long phase2;

    private long phase1Done;
    private long phase2Done;


    /**
     * Creates a new instance of this class with the given properties.  There are three properties that are used by this instance, if present:
     * <ul>
     *     <li><code>_phase1_</code> is the length, in milliseconds, phase 1.  During phase 1, the result of {@link #isEnabled()} is equal to the
     *     value of <code>_startAs_</code>.  If this property is not supplied, it defaults to 1000 milliseconds (one second).</li>
     *     <li><code>_phase2_</code> is the length, in milliseconds, phase 2.  During phase 2, the result of {@link #isEnabled()} is equal to the
     *     inverted value of <code>_startAs_</code>.  If this property is not supplied, it defaults to 1000 milliseconds (one second).</li>
     *     <li><code>_startAs_</code>, a boolean, determines the output of {@link #isEnabled()} during phase 1; the inverse of this property
     *     determines the output of {@link #isEnabled()} during phase 2.  If this property is not supplied, it defaults to <code>false</code>.</li>
     * </ul>
     * If other properties are present, they are ignored by this instance but are available publicly (and in particular, to the software being
     * tested).
     *
     * @param _properties the properties for this instance.
     */
    public PeriodicTestEnabler( final Map<String, Object> _properties ) {
        super( _properties );

        String PHASE1   = "_phase1_";
        String PHASE2   = "_phase2_";
        String START_AS = "_startAs_";

        phase1 = properties.containsKey( PHASE1 ) ? getAsLong( PHASE1 ) : 1000;
        phase2 = properties.containsKey( PHASE2 ) ? getAsLong( PHASE2 ) : 1000;
        startAs = properties.containsKey( START_AS ) && getAsBoolean( START_AS );
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
        phase1Done = phase1 + System.currentTimeMillis();
        phase2Done = phase1Done + phase2;
    }


    /**
     * Returns <code>true</code> if this instance is currently enabled.
     *
     * @return <code>true</code> if this instance is currently enabled
     */
    @Override
    protected boolean enabled() {

        long now = System.currentTimeMillis();

        // if we've finished with the current cycle, then update our times for the next one...
        if( now >= phase2Done ) {
            while( now >= phase2Done ) {  // we loop in case we somehow missed an entire cycle...
                phase1Done = phase2Done + phase1;
                phase2Done = phase1Done + phase2;
            }
        }
        return (now >= phase1Done) ^ startAs;
    }
}
