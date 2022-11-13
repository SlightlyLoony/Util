package com.dilatush.util.crc;

import java.nio.charset.StandardCharsets;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * <p>Enumerates the CRC algorithms that can be computed by {@link CRC}.</p>
 * <p>Notes on the parameter values, in order:</p>
 * <ol>
 * <li>Name: a human-readable name for the algorithm.  This name is not actually used for anything other than display.</li>
 * <li>Polynomial coefficients: This represents the coefficients for the generator polynomial, with an implied high-order coefficient.  For example, the CRC-8 algorithm has
 * 0x07 for this parameter.  With the implied high-order coefficient, this would be 0x107.  This represents the generator polynomial
 * x<sup>8</sup> + x<sup>2</sup> + x<sup>1</sup> + x<sup>0</sup>. </li>
 * <li>Width: the number of bits [1..64] in the CRC code generated by this CRC algorithm.  Note that the degree of the generator polynomial is always one greater.</li>
 * <li>Initial value: the value assigned to the CRC code before starting the CRC computation.</li>
 * <li>XOR out: the bit pattern to Exclusive-OR with the computed CRC code before providing the result.</li>
 * <li>Bit order: The order of the bits in the generator polynomial and the data the CRC is being calculated on.  Normal order is the MSB in the high-order bit, as normal with
 * two's-complement numerical representation.  Reversed order means the MSB in the low-order bit, the mirror image of two's-complement representation.  For example, the 16 bit
 * value 0xf5a1 in normal bit order becomes 0x85af in reversed order.</li>
 * <li>Check: the result expected if the standard test data is used to compute a CRC code with this algorithm.  The standard test data are the bytes resulting from the UTF-8
 * encoding of the string {@code "123456789"}.</li>
 * </ol>
 * <p>The values for the CRC definitions are shamelessly stolen from the wonderful <i>Catalogue of parametrised CRC algorithms</i>, which is part of the <i>CRC RevEng</i>
 * project by <i>Greg Cook</i>.  The project - an admirable and heroic effort - can be found <a href="https://reveng.sourceforge.io/">here</a>.</p>
 */
public enum CRCAlgorithm {

    /** Eight bit CRC algorithm originally used with IBM's SMSBus. */
    CRC8     ( "CRC-8",       0x07,                    8, 0x00,                    0x00,                    BitOrder.NORMAL,   0xf4                   ),

    /** Sixteen bit CRC algorithm originally used with ARCNET. */
    CRC16    ( "CRC-16",      0x8005,                 16, 0x0000,                  0x0000,                  BitOrder.REVERSED, 0xbb3d                 ),

    /** Twenty-four bit CRC algorithm originally used with OpenPGP. */
    CRC24    ( "CRC-24",      0x0086_4cfb,            24, 0xb7_04ce,               0x00_0000,               BitOrder.NORMAL,   0x0021_cf02            ),

    /** Twenty-four bit CRC algorithm originally used with the OS-9 operating system */
    CRC24_OS9( "CRC-24/OS-9", 0x80_0063,              24, 0xff_ffff,               0xff_ffff,               BitOrder.NORMAL,   0x20_0fa5              ),

    /** Thirty-two bit CRC algorithm originally used with HDLC (High-Level Data Link Control. */
    CRC32    ( "CRC-32",      0x04C1_1DB7,            32, 0xffff_ffffL,            0xffff_ffffL,            BitOrder.REVERSED, 0xcbf4_3926L           ),

    /** Sixty-four bit CRC algorithm originally used in the Go programming language. */
    CRC64    ( "CRC-64",      0x0000_0000_0000_001bL, 64, 0xffff_ffff_ffff_ffffL,  0xffff_ffff_ffff_ffffL,  BitOrder.REVERSED, 0xb909_56c7_75a4_1001L );


    /** The standard test data used to generate the CRC code for checking the CRC algorithms.  The result of the calculation should equal the "check" value of each algorithm. */
    final public static byte[] CHECK_INPUT = "123456789".getBytes( StandardCharsets.UTF_8 );

    final public  String   name;
    final public  long     polynomial;
    final public  int      width;
    final public  long     initialValue;
    final public  long     xorOut;
    final public  BitOrder bitOrder;
    final public  long     check;
    final public  long     crcMask;

    private long[]   precomputed;         // a table of precomputed (memoized) CRCs the indexed byte values and this algorithm...

    CRCAlgorithm( final String _name, final long _polynomial, final int _width, final long _initialValue, final long _xorOut,
                  final BitOrder _bitOrder, final long _check ) {

        // sanity checks...
        if( isNull( _bitOrder ) ) throw new IllegalArgumentException( "_bitOrder may not be null" );
        if( isEmpty( _name) ) throw new IllegalArgumentException( "_name may not be null or zero length" );
        if( (_width < 1) || (_width > 64) ) throw new IllegalArgumentException( "_width is out of the range [1..64]" );

        name         = _name;
        polynomial   = _polynomial;
        width        = _width;
        initialValue = _initialValue;
        xorOut       = _xorOut;
        bitOrder     = _bitOrder;
        check        = _check;
        crcMask     = (~0L) >>> (64 - width);
    }


    /**
     * Return the precomputed CRC for the given index byte.
     *
     * @param _byte The index byte to look up the CRC for.
     * @return The precomputed CRC for the given index byte.
     */
    public long getByteCRC( final int _byte ) {

        // lazy initialization...
        if( precomputed == null )
            precompute();

        // look up the CRC and return it...
        return precomputed[0xff & _byte];
    }


    /**
     * Generate the table of precomputed CRCs for this CRC algorithm.
     */
    private void precompute() {

        // some setup...
        precomputed = new long[256];

        // the table computation details depend on the bit order...
        if( bitOrder == BitOrder.NORMAL ) {

            // get a mask for the high-order bit in the CRC...
            long hiBit = 1L << (width - 1);

            // for all possible index byte values...
            for( int n = 0; n < 256; n++ ) {

                // put n (the current index byte value) in the high-order byte of the CRC...
                long pCRC = ((long)n) << (width - 8);

                // for each bit in the byte...
                for( int b = 0; b < 8; b++ ) {

                    // if the high-order bit is a one, XOR the polynomial coefficients with the CRC left shifted one...
                    if( (pCRC & hiBit) != 0 )
                        pCRC = polynomial ^ (pCRC << 1);

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
            var poly = reverse( polynomial );

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
     * Mirror the low-order (width) bits in the given value.
     *
     * @param _val The value to mirror.
     * @return The mirrored value.
     */
    private long reverse( final long _val ) {
        return Long.reverse( _val ) >>> (64 - width);
    }
}
