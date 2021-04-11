package com.dilatush.test;

import com.dilatush.util.Outcome;
import com.dilatush.util.config.AConfig;

import java.util.List;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Config extends AConfig {

    public int x = 10;
    public int y;
    public SubConfig z = new SubConfig();


    @Override
    public void verify( final List<String> _messages ) {
        validate( () -> x >= 0, _messages, "X can't be less than zero: " + x );
        validate( () -> (y > 0) && (y < 1000), _messages, "Y is out of range: " + y );
        z.verify( _messages );
    }


    public static void main( final String[] _args ) {
        Config config = new Config();
        Outcome<?> result = config.init( "Test", "script/Test.java", "script/subsDoc.txt" );
        config.hashCode();
    }

}
