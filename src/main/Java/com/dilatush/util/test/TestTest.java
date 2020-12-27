package com.dilatush.util.test;

import com.dilatush.util.JSConfig;

import static java.lang.Thread.sleep;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestTest {


    public static void main( final String[] _args ) throws InterruptedException {

        // get test configuration...
        TestManager.Config config = new JSConfig<TestManager.Config>().get( "TestTest.js", TestManager.Config.class );
        TestManager mgr = TestManager.getInstance();
        mgr.setConfig( config );

        TestEnabler te1 = mgr.register( "te1" );
        TestEnabler te2 = mgr.register( "te2" );

        System.out.println( "Count: " + te2.getAsLong( "count" ) );


        while( true ) {
            sleep( 500 );

            if( te2.isEnabled() ) {
                System.out.println( "te2 enabled" );
            } else {
                System.out.println( "te2 disabled" );
            }

            if( te1.isEnabled() )
                System.out.println( "te1 enabled " );
        }
    }
}
