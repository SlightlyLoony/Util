package com.dilatush.util.crc;

import static com.dilatush.util.General.isNull;

/**
 * Calculate Cyclic Redundancy Codes (CRCs) for various CRC algorithms.  The algorithm used is determined by the {@link CRCAlgorithm} constructor argument.  The CRC algorithm's
 * width must be a multiple of 8 (the number of bits in a byte).
 * References:
 * <a href="http://www.sunshine2k.de/articles/coding/crc/understanding_crc.html">here</a>,
 * <a href="https://en.wikipedia.org/wiki/Cyclic_redundancy_check">here</a>,
 * <a href="https://reveng.sourceforge.io/crc-catalogue/all.htm">here</a>, and
 * <a href="https://developer.classpath.org/doc/java/util/zip/CRC32-source.html">here</a>.
 */
public class CRC {

    private final  CRCAlgorithm algo;   // the algorithm used for this instance...

    private long   crc;                 // the CRC at the current point in the computation...


    /**
     * Creates a new instance of {@link CRC} for the given CRC algorithm.
     *
     * @param _algo The {@link CRCAlgorithm}.
     */
    public CRC( final CRCAlgorithm _algo ) {

        // sanity check...
        if( isNull( _algo ) ) throw new IllegalArgumentException( "_algo may not be null" );

        algo = _algo;
        init();        // initialize the CRC value...
    }


    /**
     * Update the CRC for the given byte of input data.  To compute the CRC for multiple bytes of input data, call this method for each byte of input data, starting with the
     * most significant byte, then sequentially through the least significant byte.
     *
     * @param _byte The byte of input data to use in updating the CRC value.
     */
    public void update( final byte _byte ) {

        // the CRC computation details depend on the bit order...
        if( algo.bitOrder == BitOrder.NORMAL ) {

            // the index into our precomputed CRCs is the XOR of the high-order byte of the current CRC, and the data byte...
            int index = 0xff & ((int)(crc >>> (algo.width - 8)) ^ _byte);

            // the updated CRC is the XOR of the precomputed CRC and the low-order bytes of the current CRC, shifted toward the MSB by a byte...
            crc = algo.getByteCRC( index ) ^ (crc << 8);
        }

        else {

            // the index into our precomputed CRCs is the XOR of the high-order byte of the current CRC, and the data byte...
            int index = 0xff & ((int)crc ^ _byte);

            // the updated CRC is the XOR of the precomputed CRC and the low-order bytes of the current CRC, shifted toward the MSB by a byte...
            crc = algo.getByteCRC( index ) ^ (crc >>> 8);
        }
    }


    /**
     * Update the CRC for the given bytes of input data.  The bytes must be in order from most significant to least.
     *
     * @param _bytes The array to take input data bytes from.
     * @param _offset The offset to the first input data byte.
     * @param _length The number of input data bytes.
     */
    public void update( final byte[] _bytes, final int _offset, final int _length ) {

        for( int i = 0; i < _length; i++ ) {
            update( _bytes[_offset + i]);
        }
    }


    /**
     * Update the CRC for the given bytes of input data.  The bytes must be in order from most significant to least.  All the bytes in the given byte array will be used.
     *
     * @param _bytes The array to take input data bytes from.
     */
    public void update( final byte[] _bytes ) {
        update( _bytes, 0, _bytes.length );
    }


    /**
     * Initialize this instance to compute a new CRC.
     */
    public void init() {
        crc = algo.initialValue;
    }


    /**
     * Return the current value of the CRC, in the low-order bits of the result.
     *
     * @return The current value of the CRC.
     */
    public long getCRC() {
        return algo.crcMask & (crc ^ algo.xorOut);
    }


    /**
     * Check that the given CRC value matches the algorithm's check value.  This is only useful when testing that the various CRC algorithms are
     * correctly implemented.
     *
     * @param _crc The CRC value to check.
     * @return Returns {@code true} if the check passed (was correct).
     */
    /* package-private */ boolean check( final long _crc ) {
        return algo.check == _crc;
    }
}
