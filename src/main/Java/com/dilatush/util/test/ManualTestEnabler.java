package com.dilatush.util.test;

import java.util.Map;

/**
 * Implements a {@link TestEnabler} that defaults to disabled, can be enabled or disabled via the property "enabled".
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class ManualTestEnabler extends ATestEnabler {


    public ManualTestEnabler( final Map<String, Object> _properties ) {
        super( _properties );
        properties.put( "enabled", false );
    }


    /**
     * Returns {@code true} if the this enabler has been enabled manually.
     *
     * @return {@code true} if the this enabler has been enabled manually
     */
    @Override
    protected boolean enabled() {
        Object value = properties.get( "enabled" );
        return (value instanceof Boolean) && (Boolean) value;
    }
}
