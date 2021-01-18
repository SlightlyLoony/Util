package com.dilatush.util;

import java.util.Arrays;

import static com.dilatush.util.General.isNull;

/**
 * <p>Implements base64 encoding and decoding per the alphabet defined in RFC 4648, section 4 ("Base 64 Encoding"), but without any provision for
 * padding or line breaks.  Eliminating all base64 features other than the actual encoding means that for any given bytes being encoded, there is
 * exactly one encoded base64 string, and vice versa.  The length of the resulting base64 string for any given number of bytes is always the same,
 * and the number of bytes decoded from a base64 string of any given length is always the same.</p>
 * <p>This implementation provides direct encoders/decoders for some common data types.</p>
 * <p>Note that this implementation replaces a prior implementation that was roughly half the speed of this one, couldn't encode or decode
 * length byte arrays or base64 strings, and was much more complex.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Base64 {

    /**
     * The characters of the base 64 "alphabet", in the order of the value they represent, [0..63].
     */
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * For characters with values in the range [0..127] (the index) contains the base 64 value of them.
     */
    private static final byte[] CHAR_TO_VALUE = getCharToValueTable();


    /**
     * Returns the base 64 string that encodes the given long number.  A long contains up to 8 bytes of data, but any zero bytes that can be ignored
     * (the most significant bytes) are not encoded.  If the given number is zero, then a zero length string is the result.  Since between zero and
     * eight bytes could be encoded, the returned base64 string may contain between 0 and 11 (but never 1, 5, or 9) characters long.
     *
     * @param _number the long integer to encode into base 64.
     * @return the base 64 encoding of the specified long.
     */
    @SuppressWarnings( "unused" )
    public static String encode( final long _number ) {

        // figure out how many bytes we need to encode...
        int numBits = 64 - Long.numberOfLeadingZeros( _number );
        int numBytes = (numBits >>> 3) + (((numBits & 0x7) == 0) ? 0 : 1);

        // adjust our input so the most significant byte has bits...
        long val = _number << (8 * (8 - numBytes));

        // turn our long into a byte array...
        byte[] bytes = new byte[numBytes];
        for( int b = 0; b < bytes.length; b++ ) {
            bytes[b] = (byte)((val >>> 8 * (7 - b)) & 0xFF);
        }

        return encode( bytes );
    }


    /**
     * Returns the long encoded by the specified base64 string.  If the given string decodes to fewer than 8 bytes, they will be the least
     * significant bytes of the result.  Valid lengths for the given string are between 0 and 11, but not 1, 5, or 9.  Throws an
     * {@link IllegalArgumentException} if the given string is missing, represents a value too large to fit in a long, is an impossible length to
     * contain an encoding, or contains invalid characters.
     *
     * @param _base64 the base 64 encoding to decode into a long.
     * @return the long decoded from the specified encoding.
     */
    @SuppressWarnings( "unused" )
    public static long decodeLong( final String _base64 ) {
        byte[] bytes = decodeBytes( _base64 );
        if( bytes.length > 8 )
            throw new IllegalArgumentException( "Base 64 string encodes more than 8 bytes" );

        // shift our bytes into a long...
        long result = 0;
        for( byte b : bytes ) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }


    /**
     * Returns the base 64 string that encodes the given bytes.
     *
     * @param _bytes the bytes to encode to base 64.
     * @return the base 64 encoding of the specified bytes.
     * @throws IllegalArgumentException if the bytes are missing
     */
    public static String encode( final byte[] _bytes ) {

        // fail fast if we got no argument...
        if( _bytes == null )
            throw new IllegalArgumentException( "Missing bytes to encode to base 64" );

        // if we got a zero-length byte array, then we have a simple answer...
        if( _bytes.length == 0 )
            return "";

        // calculate how long our output will be...
        int len = (_bytes.length << 2) / 3;  // this is close, but not correct...
        len += ((len & 0x03) == 0) ? 0 : 1;  // apply a correction...

        // make an array for our result...
        char[] result = new char[len];

        /*
         * The loop below is processing triplets of input bytes, or a part thereof.  Each triplet results in a quadruplet of characters.
         * It COULD have been done one byte at a time, but it is unrolled for performance...
         */

        // iterate over our byte triplets...
        int cdex = 0;
        int bdex = 0;
        int val;
        while( bdex < _bytes.length ) {

            /* the first byte of the triplet */

            // get the next byte...
            int b = (_bytes[bdex++] & 0xFF);

            // the upper six bits translate to a character to emit...
            result[cdex++] = ALPHABET.charAt( b >>> 2 );

            // we have two bits left over; shift and save...
            val = (b & 0x03) << 4;

            // if there are no more bytes, then emit the trailing character and break out...
            if( bdex >= _bytes.length ) {
                result[cdex] = ALPHABET.charAt( val );
                break;
            }

            /* the second byte of the triplet */

            // get the next byte...
            b = (_bytes[bdex++] & 0xFF);

            // the two bits leftover plus the four upper bits here translate to a character to emit...
            result[cdex++] = ALPHABET.charAt( val | (b >>> 4) );

            // we have four bits left over; shift and save...
            val = (b & 0x0F) << 2;

            // if there are no more bytes, then emit the trailing character and break out...
            if( bdex >= _bytes.length ) {
                result[cdex] = ALPHABET.charAt( val );
                break;
            }

            /* the third byte of the triplet */

            // get the next byte...
            b = (_bytes[bdex++] & 0xFF);

            // the four bits leftover plus the two upper bits here translate to a character to emit...
            result[cdex++] = ALPHABET.charAt( val | (b >>> 6) );

            // our leftover six bits translate to a second character to emit...
            result[cdex++] = ALPHABET.charAt( b & 0x3F );
        }

        return new String( result );
    }


    /**
     * Returns a byte array encoded by the specified base 64 string.  Throws an {@link IllegalArgumentException} if the given string is missing,
     * is an impossible length to contain an encoding, or contains invalid characters.
     *
     * @param _base64 the base 64 encoding to decode into a byte array.
     * @return the byte array decoded from the specified encoding.
     */
    public static byte[] decodeBytes( final String _base64 ) {

        // fail fast if we got no argument...
        if( isNull( _base64 ) )
            throw new IllegalArgumentException( "Base64 string is missing" );

        // if we got a zero-length string, then we have a simple answer...
        int charLen = _base64.length();
        if( charLen == 0 )
            return new byte[0];

        // if the number of characters modulo 4 is 1, the number of characters in our input is invalid base64...
        if( (charLen & 0x03) == 1 )
            throw new IllegalArgumentException( "Base64 string is not a valid length: " + _base64.length() );

        // calculate how long our output array will be...
        int len = ((charLen << 1) + charLen) >>> 2;  // this calculates 0.75 * charLen IS our output length...

        // make an array for our result...
        byte[] result = new byte[len];

        // get our characters into an array...
        char[] in = _base64.toCharArray();

        /*
         * The loop below is processing quadruplets of input characters, or a part thereof.  Each quad results in a triplet of bytes.
         * It COULD have been done one character at a time, but it is unrolled for performance.
         */

        // iterate over our character quadruplets...
        int cdex = 0;
        int bdex = 0;
        int val = 0;
        int c;
        while( cdex < in.length ) {

            /* the first character of the quad */

            // get the next character's value, checking for validity...
            c = (in[cdex] >= 128) ? -1 : CHAR_TO_VALUE[in[cdex]];
            if( c < 0 )
                throw new IllegalArgumentException( "Invalid character in base 64 string: " + in[cdex] );
            cdex++;  // we don't need to check for end-of-string here, as we've already thrown an exception for strings that ended here...

            // we just save this, shifted, in the value; we only have six bits of our byte...
            val = c << 2;

            /* the second character of the quad */

            // get the next character's value, checking for validity...
            c = (in[cdex] >= 128) ? -1 : CHAR_TO_VALUE[in[cdex]];
            if( c < 0 )
                throw new IllegalArgumentException( "Invalid character in base 64 string: " + in[cdex] );
            cdex++;

            // we have the two more bits we needed for a byte, so stuff it away...
            result[bdex++] = (byte)(val | c >>> 4);

            // save our leftover four bits, shifted, to the value...
            val = (c & 0x0F) << 4;

            // if we're out of characters, break out of the loop...
            if( cdex >= in.length )
                break;

            /* the third character of the quad */

            // get the next character's value, checking for validity...
            c = (in[cdex] >= 128) ? -1 : CHAR_TO_VALUE[in[cdex]];
            if( c < 0 )
                throw new IllegalArgumentException( "Invalid character in base 64 string: " + in[cdex] );
            cdex++;

            // we have the four more bits we needed for a byte, so stuff it away...
            result[bdex++] = (byte)(val | c >>> 2);

            // save our leftover two bits, shifted, to the value...
            val = (c & 0x03) << 6;

            // if we're out of characters, break out of the loop...
            if( cdex >= in.length )
                break;

            /* the fourth character of the quad */

            // get the next character's value, checking for validity...
            c = (in[cdex] >= 128) ? -1 : CHAR_TO_VALUE[in[cdex]];
            if( c < 0 )
                throw new IllegalArgumentException( "Invalid character in base 64 string: " + in[cdex] );
            cdex++;

            // we have the six more bits we needed for a byte, so stuff it away...
            result[bdex++] = (byte)(val | c );

            // we have no bits left over...
            val = 0;
        }

        // if our leftover bits are non-zero, we've got an invalid encoding...
        if( val != 0 )
            throw new IllegalArgumentException( "Invalid base 64 encoding - leftover bits are non-zero" );

        return result;
    }


    /**
     * Returns true if the specified character is valid in a base 64 encoding, false otherwise.
     *
     * @param _char the character to test.
     * @return true if the specified character is valid in a base 64 encoding, false otherwise.
     */
    @SuppressWarnings( "unused" )
    public static boolean isValidBase64Char( final char _char ) {
        return ALPHABET.indexOf( _char ) >= 0;
    }


    /**
     * Returns an array of byte values corresponding to character values (the index).  A value of -1 indicates an invalid character.
     *
     * @return an array of byte values corresponding to character values (the index)
     */
    private static byte[] getCharToValueTable() {

        byte[] result = new byte[128];
        Arrays.fill( result, (byte)-1 );  // the default is an invalid character...
        for( int i = 0; i < ALPHABET.length(); i++ ) {
            result[ALPHABET.charAt( i )] = (byte)i;
        }
        return result;
    }
}
