package com.dilatush.util.test;

import java.util.Map;

/**
 * Implements a {@link TestEnabler} that is always enabled.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class SimpleTestEnabler extends ATestEnabler {


    public SimpleTestEnabler( final Map<String, Object> _properties ) {
        super( _properties );
    }


    /**
     * Returns {@code true}.
     *
     * @return {@code true}
     */
    @Override
    protected boolean enabled() {
        return true;
    }
}
