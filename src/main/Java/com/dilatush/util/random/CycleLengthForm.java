package com.dilatush.util.random;

public enum CycleLengthForm {

    AT_LEAST,   /** A repeating pseudorandom source with an unknown cycle length that is at least of the specified length. */
    EXACTLY,    /** A repeating pseudorandom source with a cycle length exactly as specified. */
    INFINITE,   /** A non-repeating pseudorandom source */
    UNKNOWN     /** A repeating pseudorandom source with an unknown cycle length. */
}
