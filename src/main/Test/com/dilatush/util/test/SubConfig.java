package com.dilatush.util.test;

import com.dilatush.util.config.AConfig;

import java.util.List;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SubConfig extends AConfig {

    public String z;


    @Override
    public void verify( final List<String> _messages ) {
        validate( () -> (z != null) && (z.length() < 5), _messages, "Z is not valid: " + z );
    }
}
