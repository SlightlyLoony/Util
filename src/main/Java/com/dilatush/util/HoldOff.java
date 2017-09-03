package com.dilatush.util;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class HoldOff {

    private int remainingTicks;

    public HoldOff( int _startingTicks ) {
        remainingTicks = _startingTicks;
    }

    public void tick() {
        if( remainingTicks > 0 ) remainingTicks--;
    }

    public boolean check() {
        return remainingTicks <= 0;
    }
}
