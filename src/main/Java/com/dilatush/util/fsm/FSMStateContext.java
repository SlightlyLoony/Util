package com.dilatush.util.fsm;

import java.time.Duration;
import java.time.Instant;

/**
 * (this may be subclassed)
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class FSMStateContext {

    // these values are set by FSM...
    private Duration timeInState = Duration.ZERO;  // the total time spent in this state...
    private Instant  lastEntered = Instant.now();  // time this state was most recently entered...
    private Instant  lastLeft    = Instant.now();  // time this state was most recently left...
    private long     entries     = 0;              // the total number of times this state has been entered...


    /*package-private*/ void setTimeInState( final Duration _timeInState ) {
        timeInState = _timeInState;
    }


    /*package-private*/ void setLastEntered( final Instant _lastEntered ) {
        lastEntered = _lastEntered;
    }


    /*package-private*/ void setLastLeft( final Instant _lastLeft ) {
        lastLeft = _lastLeft;
    }


    /*package-private*/ void setEntries( final long _entries ) {
        entries = _entries;
    }


    public Duration getTimeInState() {
        return timeInState;
    }


    public Instant getLastEntered() {
        return lastEntered;
    }


    public Instant getLastLeft() {
        return lastLeft;
    }


    public long getEntries() {
        return entries;
    }
}
