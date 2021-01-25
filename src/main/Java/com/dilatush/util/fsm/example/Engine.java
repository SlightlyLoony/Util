package com.dilatush.util.fsm.example;

/**
 * Interface to the generator's propane engine.  As can be seen from the interface methods, the interface is quite primitive.  You can turn the power
 * on or off, turn the fuel valve on or off, turn the starter motor on or off, and you can read the tachometer.  If the RPMs go over 1000, the engine
 * has started.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface Engine {

    /**
     * Turn the power to the engine on or off.
     *
     * @param _mode The desired power state.
     */
    void power( final Mode _mode );


    /**
     * Turn the fuel valve on (open) or off (closed).
     *
     * @param _mode The desired fuel valve state.
     */
    void fuel( final Mode _mode );


    /**
     * Turn the starter motor on or off.  Note that the starter motor should not be turned on for more than 15 seconds at a time, and should be
     * allowed to cool down for 15 seconds before being turned on again.
     *
     * @param _mode The desired starter motor state.
     */
    void starter( final Mode _mode );


    /**
     * Returns the current tachometer reading in revolutions per minute (RPM).  To generate AC at 60 Hz, the engine must be turning at 1800 RPM,
     * plus or minus 0.5%.
     *
     * @return the current tachometer reading
     */
    double tachometer();


    enum Mode { ON,OFF; }
}
