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
     * Always returns <code>false</code> (<i>not</i> enabled).
     *
     * @return <code>false</code>
     */
    @Override
    protected boolean enabled() {
        return false;
    }
}
