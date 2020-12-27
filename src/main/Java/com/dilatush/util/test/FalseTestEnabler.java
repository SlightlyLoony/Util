package com.dilatush.util.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a {@link TestEnabler} that always returns a <code>false</code> from {@link #isEnabled()}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FalseTestEnabler extends ATestEnabler {


    public FalseTestEnabler(  final Map<String, Object> _properties ) {
        super( new HashMap<>() );
    }


    /**
     * Returns <code>true</code> if this instance is currently enabled.
     *
     * @return <code>true</code> if this instance is currently enabled
     */
    @Override
    protected boolean enabled() {
        return false;
    }
}
