package com.dilatush.util.test;

import java.util.HashMap;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FalseTestEnabler extends ATestEnabler {


    public FalseTestEnabler() {
        super( new HashMap<String, Object>() );
    }


    @Override
    boolean enabled() {
        return false;
    }
}
