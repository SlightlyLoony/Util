package com.dilatush.util;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for functions related to bytes and arrays of bytes.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Bytes {


    /**
     * Returns the given byte array adjusted to the given new length.  If the new length is less than the original length, the result is the given byte array truncated
     * to the new length.  If the new length is longer than the original length, the result is the original byte array with zero bytes appended as needed to expand the
     * array to the desired length.  Note that the returned array is <i>never</i> the given array.  Even if the new length is the same as the given byte array's length, a copy
     * is made and returned.
     *
     * @param _bytes The byte array to adjust the length of
     * @param _newLength The new length for the byte array.
     * @return The byte array with its length adjusted.
     * @throws IllegalArgumentException if the given bytes are null or the new length is negative
     */
    public static byte[] adjust( final byte[] _bytes, final int _newLength ) {

        // sanity check...
        if( isNull( (Object)_bytes ) ) throw new IllegalArgumentException( "_bytes is null" );
        if( _newLength < 0 ) throw new IllegalArgumentException( "_newLength is negative" );

        // create our result array of the right length...
        var result = new byte[_newLength];

        // copy the right number of bytes into our result...
        System.arraycopy( _bytes, 0, result, 0, java.lang.Math.min( _bytes.length, result.length ) );

        // we're done!
        return result;
    }


    /**
     * Returns a byte array that is a concatenation of the given byte arrays, in the same order.
     *
     * @param _bytes The byte arrays to be concatenated.
     * @return The concatenated byte arrays.
     */
    public static byte[] concatenate( final byte[]... _bytes ) {

        // sanity check...
        if( isNull( (Object)_bytes ) ) throw new IllegalArgumentException( "_bytes is null" );

        // compute the length of the concatenated result, and make an array for it...
        var cLength = 0;
        for( byte[] b: _bytes ){
            cLength += b.length;
        }
        var result = new byte[cLength];

        // copy each source array into the result...
        var index = 0;
        for( byte[] bytes : _bytes ) {
            System.arraycopy( bytes, 0, result, index, bytes.length );
            index += bytes.length;
        }

        // we're done!
        return result;
    }


    /**
     * Makes a "pretty" string to display the contents of the given bytes.  The string may produce multiple newline terminated lines, and the entire
     * string is always terminated with a newline.  A {@code null} or empty argument is handled gracefully.  The given bytes are otherwise displayed
     * in rows of 16 hexadecimal values, with 16 alphanumeric values to the right of them (for byte values in ASCII range).
     *
     * @param _bytes The bytes to make a pretty string from.
     * @return the pretty string
     */
    public static String bytesToString( final byte[] _bytes ) {

        // if we were given a null, say so...
        if( _bytes == null )
            return "null\n";

        // if we had no bytes, say so...
        if( _bytes.length == 0 )
            return "empty\n";

        // some setup..
        int rowCount = (_bytes.length >>> 4) + ((((_bytes.length) & 0xf) == 0) ? 0 : 1);
        StringBuilder result = new StringBuilder( 100 * rowCount );

        // iterate over all our rows...
        for( int row = 0; row < rowCount; row++ ) {

            // iterate over the bytes in a row to dump the bytes in hex...
            int maxIndex = java.lang.Math.min( 16, _bytes.length - (row << 4) );
            for( int index = 0; index < maxIndex; index++) {
                result.append( toHex( _bytes[(row << 4) | index] ) );
                result.append( ' ' );
            }

            // output spaces as needed to get to the column where we dump the bytes as chars...
            result.append( Strings.getStringOfChar( ' ', 2 + 3 * (16 - maxIndex) ) );

            // iterate over the byte in a row to dump the bytes in chars...
            for( int index = 0; index < maxIndex; index ++ ) {
                byte thisByte = _bytes[(row << 4) | index];
                result.append( ((thisByte > 32) && (thisByte < 127)) ? (char) thisByte : (char) 183 );
                result.append( ' ' );
            }

            // add a newline...
            result.append( '\n' );
        }

        return result.toString();
    }


    // nibble-to-hex character table...
    private static final char[] hexChars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };


    /**
     * Return the two-character hexadecimal string showing the value of the given byte.
     *
     * @param _byte The byte to show a value for.
     * @return the two-character hexadecimal string showing the value of the given byte
     */
    public static String toHex( final byte _byte ) {
        return new String( new char[] { hexChars[0xf & (_byte >>> 4)], hexChars[_byte & 0xf]} );
    }
}
