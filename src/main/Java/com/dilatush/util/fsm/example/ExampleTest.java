package com.dilatush.util.fsm.example;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ExampleTest {

    public static void main( final String[] _args ) throws InterruptedException {

        EngineController controller = new EngineController( new EngineSim(), ExampleTest::onReport );
        controller.start();


        Thread.sleep( 60000 );
    }


    private static void onReport( final EngineController.Report _report ) {

    }
}
