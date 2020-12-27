package com.dilatush.util.test;

import java.util.List;
import java.util.Map;

/**
 * Implements a {@link TestEnabler} that computes the result of {@link #isEnabled()} as the logical AND of the configured list of
 * {@link TestEnabler}s, using short-circuit logic on the test enablers in the given order.  During the execution of this logic, if a test enabler
 * is discovered to have transitioned from enabled (<code>true</code> from {@link #isEnabled()}) to disabled, then the {@link #init()} method is
 * invoked on all the test enablers after it on the given list.  This is primarily useful in conjunction with the {@link PeriodicTestEnabler}, but
 * may also be useful in conjunction with custom {@link TestEnabler} implementations or with {@link JavaScriptTestEnabler}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class CompositeTestEnabler extends ATestEnabler {

    private final List<TestEnabler> enablers;


    public CompositeTestEnabler( final Map<String, Object> _properties, final List<TestEnabler> _testEnablers ) {
        super( _properties );
        enablers = _testEnablers;
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
        for( TestEnabler enabler : enablers ) {
            enabler.init();
        }
    }


    /**
     * Returns <code>true</code> if this instance is currently enabled.
     *
     * @return <code>true</code> if this instance is currently enabled
     */
    @Override
    protected boolean enabled() {

        // we assume we're enabled until we find out we actually aren't...
        boolean enabled = true;

        // we assume we're not initializing, until and if an enabled-to-disabled edge is detected...
        boolean init = false;

        // iterate over all our enablers...
        for( TestEnabler enabler : enablers ) {

            // if we're initializing, just do it and move along...
            if( init ) {
                enabler.init();
                continue;
            }

            // figure out whether we are and were enabled...
            boolean was = enabler.getLastEnabled();
            enabled     = enabler.isEnabled();

            // if we see a enabled to disabled edge, set flag to initialize the downstream enablers...
            init = was & !enabled;

            // if we're not enabled, and we're not initializing, then we're done...
            if( !enabled && !init )
                break;

        }

        return enabled;
    }
}
