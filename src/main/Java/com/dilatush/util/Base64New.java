package com.dilatush.util;

import java.util.Arrays;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements base64 encoding decoding per RFC 4648, section 4 ("Base 64 Encoding").  This implementation provides direct encoders/decoders for
 * common data types.  It does not handle or produce line breaks.  It does not produce padding, as the padding can be inferred from the length of the
 * encoded result.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Base64New {

    /**
     * The characters of the base 64 "alphabet", in the order of the value they represent, [0..63].
     */
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /*
     * This table is the heart of the decoder, providing the means to translate from a character quadruplet to a byte triplet.  The primary index
     * is the ASCII code of the character, and the secondary index is the index of the character within the quadruplet ([0..3]).  The value in the
     * table is the decoded value in the right position within the 3 byte (24 bit) result.  Note that this table is sparse in the sense of valid
     * entries.  The invalid entries are all filled with -1, which is used to detect bogus characters in a base64 string.
     */
    private static final int[][] DECODE_TABLE = getDecoderTable();


    /**
     * Returns a base 64 string that encodes the given long number.  Leading zero bits are not encoded, so the string returned has between 2 and 11
     * characters.
     *
     * @param _number the long integer to encode into base 64.
     * @return the base 64 encoding of the specified long.
     */
    public static String encode( final long _number ) {
        return null;
    }


    /**
     * Returns the long encoded by the specified base 64 string.  Throws an {@link IllegalArgumentException} if the given string is missing, zero length,
     * represents a value too large to fit in a long, is an impossible length to contain an encoding, or contains invalid characters.
     *
     * @param _base64 the base 64 encoding to decode into a long.
     * @return the long decoded from the specified encoding.
     */
    public static long decodeLong( final String _base64 ) {
        return 0;
    }


    /**
     * Returns a base 64 string that encodes the given bytes.
     *
     * @param _bytes the bytes to encode to base 64.
     * @return the base 64 encoding of the specified bytes.
     * @throws IllegalArgumentException if the bytes are missing or are zero-length
     */
    public static String encode( final byte[] _bytes ) {
        return null;
    }


    /**
     * Returns a byte array encoded by the specified base 64 string.  Throws an {@link IllegalArgumentException} if the given string is missing,
     * zero length, is an impossible length to contain an encoding, or contains invalid characters.
     *
     * @param _base64 the base 64 encoding to decode into a byte array.
     * @return the byte array decoded from the specified encoding.
     */
    public static byte[] decodeBytes( final String _base64 ) {

        // get our parameters, which will fail fast if we have something we can't possibly decode...
        Parameters params = getParameters( _base64 );

        // get our character array to make the decoding faster...
        char[] chars = _base64.toCharArray();

        // make sure we don't have any out-of-range characters...
        for( char c : chars ) {
            if( c >= 128 )
                throw new IllegalArgumentException( "Illegal character in base64 string: '" + c + ";" );
        }

        // make a place for our result...
        byte[] result = new byte[ params.bytes ];

        // figure out how many times we're going to iterate...
        int iters = params.triplets + ((params.extraBytes == 0) ? 0 : 1);

        // handle all our full triplets of bytes, each of which takes four characters...
        for( int triplet = 0; triplet < iters; triplet++ ) {

            int val = 0;   // starting value of the triplet...
            int to = triplet * 4;  // starting offset into character array...

            for( int offset = 0; offset < 4; offset++ ) {
                char c = (to + offset >= chars.length) ? 'A' : chars[to + offset];
                val += DECODE_TABLE[c][offset];
            }

            stuffTriplet( result, triplet * 3, val );
        }

        // we're done - return with our results...
        return result;
    }


    private static void stuffTriplet( byte[] _bytes, int _byteOffset, int _value ) {

        for( int i = 0; i < 3; i++ ) {
            int offset = _byteOffset + i;
            if( offset >= _bytes.length )
                return;
            _bytes[ offset ] = (byte)(_value >> 8 * (2 - i));
        }
    }


    /**
     * Returns true if the specified character is valid in a base 64 encoding, false otherwise.
     *
     * @param _char the character to test.
     * @return true if the specified character is valid in a base 64 encoding, false otherwise.
     */
    public static boolean isValidBase64Char( final char _char ) {
        return ALPHABET.indexOf( _char ) >= 0;
    }


    private static  Parameters getParameters( final String _base64 ) {

        if( isEmpty( _base64 ) )
            throw new IllegalArgumentException( "Null or empty base64 string" );

        // calculate the decoded length, catching impossible lengths...
        int quads = _base64.length() >> 2;
        int remainder = _base64.length() & 0x03;

        // a remainder of zero means we have an exact number of triplets...
        if( remainder == 0 )
            return new Parameters( 3 * quads, quads, 0 );

        // a remainder of one means we have an impossible string length...
        if( remainder == 1 )
            throw new IllegalArgumentException( "Impossible base64 string length: " + _base64.length() );

        // a remainder of two or three means we have 1 or 2 leftover bytes...
        return new Parameters( 3 * quads + remainder - 1, quads, remainder - 1 );
    }


    /**
     * Specifies the parameters needed by the decoder: total resulting bytes, number of triplets, extra bytes after the last triplet.
     */
    private static class Parameters {
        private final int bytes;
        private final int triplets;
        private final int extraBytes;


        public Parameters( final int _bytes, final int _triplets, final int _extraBytes ) {
            bytes = _bytes;
            triplets = _triplets;
            extraBytes = _extraBytes;
        }
    }


    /**
     * Generate the decoder table from the base64 alphabet.
     *
     * @return the decoder table.
     */
    private static int[][] getDecoderTable() {

        // create our result and fill it with -1s, to indicate invalid values...
        int[][] result = new int[128][4];
        for( int[] sub : result )
            Arrays.fill( sub, -1 );

        // iterate over our alphabet...
        for( int val = 0; val < ALPHABET.length(); val++ ) {
            int i = ALPHABET.charAt( val ) & 0x7F;  // make sure we don't have any extra high-order bits...
            for( int pos = 0; pos < 4; pos++ ) {
                result[i][pos] = val << (18 - pos * 6);
            }
        }

        return result;
    }


    public static void main( final String[] _args ) {

        byte[] a = decodeBytes( "AA" );
        a = decodeBytes( "ABCDEFG" );
        System.currentTimeMillis();
    }
}
