package com.dilatush.util.crc;

/**
 * Enumerates the possible bit orders for CRC data and polynomial coefficients.
 */
public enum BitOrder {

    /** Normal bit order: the most significant bit is the same as the two's-complement most significant bit. */
    NORMAL,

    /** Reversed bit order: the most significant bit is the same as the two's-complement least significant bit. */
    REVERSED
}
