package com.dilatush.util.fsm.example;

import java.io.IOException;

/**
 * Test framework for FSM-based generator controller, with a simulated control panel and engine.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ExampleTest {


    /**
     * The main method for the example program demonstrating FSMs.
     *
     * @param _args All command line arguments are ignored.
     * @throws InterruptedException if interrupted
     * @throws IOException if I/O is exceptional
     */
    @SuppressWarnings( "unused" )
    public static void main( final String[] _args ) throws InterruptedException, IOException {

        GeneratorSim        generator  = new GeneratorSim();
        Engine              engine     = new EngineSim();

        GeneratorController controller = new GeneratorController( generator, engine );

        generator.run();
    }
}
