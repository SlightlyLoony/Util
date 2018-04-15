package com.dilatush.util;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements base64 encoding decoding per RFC 4648, section 4 ("Base 64 Encoding").  This implementation provides direct encoders/decoders for common data
 * types.  It does not handle or produce line breaks.  It does not produce padding, as the padding can be inferred from the length of the encoded result.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Base64 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final char[] BIN2CHAR = getBIN2CHAR();
    private static final byte[] CHAR2BIN = getCHAR2BIN();


    /**
     * Returns a base 64 string that encodes the given long number.  Leading zero bits are not encoded, so the string returned has between 2 and 11
     * characters.
     *
     * @param _number the long integer to encode into base 64.
     * @return the base 64 encoding of the specified long.
     */
    public static String encode( final long _number ) {

        int lzb = bytesToEncode( _number );

        StringBuilder sb = new StringBuilder( charactersNeededForBytes( lzb ) );
        int remainingBytes = lzb;
        while( remainingBytes > 0 ) {
            if( remainingBytes >= 3 ) {
                sb.append( encodeBits( _number >>> ((remainingBytes - 3) << 3), 3 ) );
                remainingBytes -= 3;
            }
            else {
                sb.append( encodeBits( _number, remainingBytes ) );
                remainingBytes = 0;
            }
        }
        return sb.toString();
    }


    /**
     * Returns the long encoded by the specified base 64 string.  Throws an {@link IllegalArgumentException} if the given string is missing, zero length,
     * represents a value too large to fit in a long, is an impossible length to contain an encoding, or contains invalid characters.
     *
     * @param _base64 the base 64 encoding to decode into a long.
     * @return the long decoded from the specified encoding.
     */
    public static long decodeLong( final String _base64 ) {
        if( isEmpty( _base64 ) ) throw new IllegalArgumentException( "Missing or empty base 64 encoding" );
        int bn = bytesNeededForCharacters( _base64.length() );
        if( bn > 8 ) throw new IllegalArgumentException( "Decoded base 64 encoding will not fit in long: " + _base64 );
        int start = 0;
        int end = Math.min( 4, _base64.length() );
        long result = 0;
        while( start < _base64.length() ) {
            result <<= ((end - (start + 1)) << 3);
            result |= decodeBits( _base64, start, end );
            start = end;
            end = Math.min( start + 4, _base64.length() );
        }
        return result;
    }


    /**
     * Returns a base 64 string that encodes the given bytes.  Throws an {@link IllegalArgumentException]} if the bytes are missing or are zero-length.
     *
     * @param _bytes the bytes to encode to base 64.
     * @return the base 64 encoding of the specified bytes.
     */
    public static String encode( final byte[] _bytes ) {

        if( isNull( (Object) _bytes ) || (_bytes.length == 0) ) throw new IllegalArgumentException( "Missing or empty bytes" );

        int len = charactersNeededForBytes( _bytes.length );
        StringBuilder sb = new StringBuilder( len );
        int i = 0;
        while( i < _bytes.length ) {
            long bits = 0;
            int lim = Math.min( 3, _bytes.length - i);
            for( int s = 0; s < lim; s++ ) {
                bits <<= 8;
                bits |= (0xff & _bytes[s + i]);
            }
            sb.append( encodeBits( bits, lim ) );
            i += 3;
        }
        return sb.toString();
    }


    /**
     * Returns a byte array encoded by the specified base 64 string.  Throws an {@link IllegalArgumentException} if the given string is missing, zero length,
     * is an impossible length to contain an encoding, or contains invalid characters.
     *
     * @param _base64 the base 64 encoding to decode into a byte array.
     * @return the byte array decoded from the specified encoding.
     */
    public static byte[] decodeBytes( final String _base64 ) {
        if( isEmpty( _base64 ) ) throw new IllegalArgumentException( "Missing or empty base 64 encoding" );
        int bn = bytesNeededForCharacters( _base64.length() );
        ByteBuffer result = ByteBuffer.allocate( bn );
        int start = 0;
        int end = Math.min( 4, _base64.length() );
        while( start < _base64.length() ) {
            int bytes = end - (start + 1);
            int bits = decodeBits( _base64, start, end );
            for( int i = bytes - 1; i >= 0; i-- ) {
                result.put( (byte) (bits >>> (i << 3) ) );
            }
            start = end;
            end = Math.min( end + 4, _base64.length() );
        }
        return result.array();
    }


    private static char[] getBIN2CHAR() {
        char[] result = new char[64];
        for( int i = 0; i < ALPHABET.length(); i++ ) {
            result[i] = ALPHABET.charAt( i );
        }
        return result;
    }


    private static byte[] getCHAR2BIN() {
        byte[] result = new byte[128];
        Arrays.fill( result, (byte) -1 );  // -1 means an invalid character...
        for( int i = 0; i < ALPHABET.length(); i++ ) {
            result[ ALPHABET.charAt( i ) ] = (byte) i;
        }
        return result;
    }


    /**
     * Decodes the specified characters, returning 1, 2, or 3 bytes of decoded bits.  The decoded bits are right-justified in the result.  The number of
     * characters to decode (_end - _start) must be 2, 3, or 4 (returning 1, 2, or 3 bytes respectively).  Throws an {@link IllegalArgumentException} if any
     * of the arguments are invalid.
     *
     * @param _base64 the base 64 encoded string being decoded.
     * @param _start the index to first character of the string to decode.
     * @param _end the index to the character following the last character of the string to decode.
     * @return the decoded bytes.
     */
    private static int decodeBits( final String _base64, final int _start, final int _end ) {

        if( isEmpty( _base64 ) ) throw new IllegalArgumentException( "Missing or empty base64 string to decode" );
        if( (_end <= _start + 1) || (_start < 0) || (_end > _base64.length()) || (_end - _start > 4) )
            throw new IllegalArgumentException( "Invalid start or end index: " + _start + ", " + _end );

        int bits = 0;
        int index = _start;
        while( index < _end ) {
            bits <<= 6;
            char c = _base64.charAt( index );
            if( c >= 128 ) throw new IllegalArgumentException( "Invalid character in base 64 encoding: " + c );
            int sextet = CHAR2BIN[c];
            if( sextet < 0 ) throw new IllegalArgumentException( "Invalid character in base 64 encoding: " + c );
            bits |= sextet;
            index++;
        }
        return bits;
    }


    /**
     * Encodes the specified bits, returning two, three, or four characters.  The bits to encode are right-justified.  The number of bytes to encode must be
     * one of 1 (returns two characters), 2 (returns three characters, or 3 (returns four characters).
     *
     * @param _bits the bits to encode in base 64.
     * @param _numBytes the number of bytes to encode (1, 2, or 3).
     * @return the base 64 encoded result (2, 3, or 4 characters).
     */
    private static char[] encodeBits( final long _bits, final int _numBytes ) {
        char[] result = new char[_numBytes + 1];
        int bitShift = _numBytes * 6;
        for( int i = 0; i < result.length; i++ ) {
            long bits = _bits >>> bitShift;
            result[i] = BIN2CHAR[ 0x3f & (int) bits ];
            bitShift -= 6;
        }
        return result;
    }


    // Returns the number of bytes required to hold the given number.
    private static int bytesToEncode( final long _number ) {
        return Math.max( 1, 8 - (Long.numberOfLeadingZeros( _number ) >> 3) );
    }


    // Given the number of bytes to be encoded, returns the number of characters needed for the encoding.
    private static int charactersNeededForBytes( final int _bytes ) {
        int rem = _bytes % 3;
        int triplets = _bytes / 3;
        return (4 * triplets) + ((rem == 0) ? 0 : 1 + rem);
    }


    // Given the number of characters in a base 64 encoding, returns the number of bytes that represents.
    // Throws an IllegalArgumentException of the number of characters is impossible.
    private static int bytesNeededForCharacters( final int _characters ) {
        int quads = _characters / 4;
        int rem = _characters % 4;
        if( rem == 1 ) throw new IllegalArgumentException( "Invalid number of characters in base 64 encoding: " + _characters );
        return (3 * quads) + ((rem == 0) ? 0 : rem - 1);
    }


    /**
     * Returns true if the specified character is valid in a base 64 encoding, false otherwise.
     *
     * @param _char the character to test.
     * @return true if the specified character is valid in a base 64 encoding, false otherwise.
     */
    public static boolean isValidBase64Char( final char _char ) {
        if( _char >= 128 ) return false;
        return CHAR2BIN[ _char ] >= 0;
    }
}
