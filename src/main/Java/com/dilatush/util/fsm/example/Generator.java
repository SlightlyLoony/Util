package com.dilatush.util.fsm.example;

import java.util.function.Consumer;

/**
 * Interface between the generator's electronics package and the rest of the generator, not including the engine.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface Generator {


    /**
     * Turn the engine running indicator on or off.
     *
     * @param _mode the mode for the engine running indicator
     */
    void runningIndicator( final Mode _mode );


    /**
     * Turn the generating indicator on or off.
     *
     * @param _mode the mode for the generating indicator
     */
    void generatingIndicator( final Mode _mode );


    /**
     * Turn the failure indicator on or off.
     *
     * @param _mode the mode for the failure indicator
     */
    void failureIndicator( final Mode _mode );


    /**
     * Turn the automatic transfer switch (ATS) on (generator connected to load) or off (grid connected to load).
     *
     * @param _mode the mode for the ATS
     */
    void ats( final Mode _mode );


    /**
     * Set the listener for events from the generator.
     *
     * @param _listener the event listener
     */
    void setListener( final Consumer<Event> _listener );


    enum Mode { ON, OFF }

    /**
     * All the events produced by the interface.
     */
    enum Event {
        ON,     // when the user presses the "On" button
        OFF,    // when the user presses the "Off" button
        AUTO,   // when the user presses the "Auto" button
        FIXED,  // when the technician presses the secret "Fixed" button
        UP,     // when the grid goes up
        DOWN    // when the grid goes down
    }
}
