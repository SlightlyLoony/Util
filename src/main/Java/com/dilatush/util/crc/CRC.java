package com.dilatush.util.crc;

import static com.dilatush.util.General.isNull;

/**
 * Calculate Cyclic Redundancy Codes (CRCs) for various CRC algorithms.  The algorithm used is determined by the {@link CRCAlgorithm} constructor argument.  The CRC algorithm's
 * width must be a multiple of 8 (the number of bits in a byte).
 */
public class CRC {

    private final  CRCAlgorithm algo;   // the algorithm used for this instance...

    private long   crc;                 // the CRC at the current point in the computation...
    private long[] precomputed;         // a table of precomputed (memoized) CRCs the indexed byte values and this algorithm...
    private long   crcMask;             // the bit mask for this CRC algorithm; the low-order (width) bits are set to 1s...


    /**
     * Creates a new instance of {@link CRC} for the given CRC algorithm.
     *
     * @param _algo The {@link CRCAlgorithm}.
     */
    public CRC( final CRCAlgorithm _algo ) {

        // sanity check...
        if( isNull( _algo ) ) throw new IllegalArgumentException( "_algo may not be null" );

        algo = _algo;
        precompute();  // precompute the CRC values for all possible index bytes...
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
            crc = precomputed[index] ^ (crc << 8);
        }

        else {

            // the index into our precomputed CRCs is the XOR of the high-order byte of the current CRC, and the data byte...
            int index = 0xff & ((int)crc ^ _byte);

            // the updated CRC is the XOR of the precomputed CRC and the low-order bytes of the current CRC, shifted toward the MSB by a byte...
            crc = precomputed[index] ^ (crc >>> 8);
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
        return crcMask & (crc ^ algo.xorOut);
    }


    /**
     * Mirror the low-order (width) bits in the given value.
     *
     * @param _val The value to mirror.
     * @return The mirrored value.
     */
    private long reverse( final long _val ) {
        return Long.reverse( _val ) >>> (64 - algo.width);
    }


    /**
     * Generate the table of precomputed CRCs for this CRC algorithm.
     */
    private void precompute() {

        // some setup...
        precomputed = new long[256];
        crcMask     = (~0L) >>> (64 - algo.width);

        // the table computation details depend on the bit order...
        if( algo.bitOrder == BitOrder.NORMAL ) {

            // get a mask for the high-order bit in the CRC...
            long hiBit = 1L << (algo.width - 1);

            // for all possible index byte values...
            for( int n = 0; n < 256; n++ ) {

                // put n (the current index byte value) in the high-order byte of the CRC...
                long pCRC = ((long)n) << (algo.width - 8);

                // for each bit in the byte...
                for( int b = 0; b < 8; b++ ) {

                    // if the high-order bit is a one, XOR the polynomial coefficients with the CRC left shifted one...
                    if( (pCRC & hiBit) != 0 )
                        pCRC = algo.polynomial ^ (pCRC << 1);

                    // otherwise, just left shift the CRC one...
                    else
                        pCRC <<= 1;
                }

                // stuff away the calculated CRC value for this index byte value...
                precomputed[n] = pCRC & crcMask;
            }
        }

        // when reversed bit order...
        else {

            // mirror the polynomial coefficients (so the high-order coefficient is in the low-order bit)
            var poly = reverse( algo.polynomial );

            // for all possible index byte values...
            for( int n = 0; n < 256; n++ ) {

                // put n (the current index byte value) in the low-order byte of the CRC...
                long pCRC = n;

                // for each bit in the byte...
                for( int b = 0; b < 8; b++ ) {

                    // if the high-order (reversed!) bit is a one, XOR the polynomial coefficients with the CRC right shifted one...
                    if( (pCRC & 1) != 0 )
                        pCRC = poly ^ (pCRC >>> 1);

                    // otherwise, just right shift the CRC one...
                    else
                        pCRC >>>= 1;
                }

                // stuff away the calculated value for this index byte value...
                precomputed[n] = pCRC & crcMask;
            }
        }
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
