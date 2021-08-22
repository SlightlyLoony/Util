package com.dilatush.util;

/**
 * Static container class for functions related to bytes and arrays of bytes.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Bytes {


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
