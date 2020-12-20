package com.dilatush.util;

/**
 * Implemented by classes with validatable state.  This is intended for use with configuration objects used by {@link JSConfig}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface Validatable {

    /**
     * Returns <code>true</code> if the state of this object is valid, and <code>false</code> otherwise, after logging a description of the invalid
     * state.
     *
     * @return <code>true</code> if the state of this object is valid.
     */
    boolean isValid();
}
