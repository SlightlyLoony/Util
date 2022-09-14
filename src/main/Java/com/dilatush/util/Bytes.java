package com.dilatush.util;

import static com.dilatush.util.General.isNull;

/**
 * Static container class for functions related to bytes and arrays of bytes.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class Bytes {


    /**
     * Return a copy of {@code _n} bytes of the given source bytes, starting at index {@code _start}.
     *
     * @param _source The bytes to copy from.
     * @param _start The starting index of the range of bytes to copy.
     * @param _n The number of bytes to copy.
     * @return The copied bytes.
     * @throws IllegalArgumentException if the given source bytes are null, or if the given range is not entirely contained in the given source bytes.
     */
    public static byte[] copy( final byte[] _source, final int _start, final int _n ) {

        // sanity checks...
        if( isNull( (Object)_source ) ) throw new IllegalArgumentException( "_source is null" );
        if( (_start < 0) || (_start >= _source.length) ) throw new IllegalArgumentException( "_start is out of range for _source" );
        if( (_n < 0) || ((_start + _n) > _source.length) ) throw new IllegalArgumentException( "_n is out of range for _start and _source" );

        // make a place for our results...
        var result = new byte[_n];

        // copy the bytes from the source...
        System.arraycopy( _source, _start, result, 0, _n );

        // we're done!
        return result;
    }


    /**
     * Returns the result of the XOR operation on the operand bytes from {@code _a} in the range [{@code _aStart}..{@code _aStart}+{@code _n}), and the operand bytes from
     * {@code _b} in the range [{@code _bStart}..{@code _bStart}+{@code _n}).  The result is {@code _n} bytes long.
     *
     * @param _a The array A of source bytes.
     * @param _aStart The start of the range of source bytes in A.
     * @param _b The array B of source bytes.
     * @param _bStart The start of the range of source bytes in B.
     * @param _n The number of bytes in the ranges of source bytes, and in the results.
     * @return The result of XORing the range of source bytes in A with the range of source bytes in B.
     * @throws IllegalArgumentException if the source bytes are null, or if any part of the given range is outside the given source bytes.
     */
    public static byte[] xor( final byte[] _a, final int _aStart, final byte[] _b, final int _bStart, final int _n ) {

        // sanity checks...
        if( isNull( _a, _b ) ) throw new IllegalArgumentException( "_a or _b is null" );
        if( (_aStart < 0) || (_bStart < 0) || (_n < 0) ) throw new IllegalArgumentException( "_aStart or _bStart or _n is negative" );
        if( ((_aStart + _n) > _a.length) || ((_bStart + _n) > _b.length)) throw new IllegalArgumentException( "_a or _b does not contain the full source range" );

        // make a place for our result...
        var result = new byte[_n];

        // do the actual XOR operation...
        for( int i = 0; i < _n; i++ ) {
            result[i] = (byte)(_a[_aStart + i] ^ _b[_bStart+i]);
        }

        // ...and we're done!
        return result;
    }


    /**
     * Returns the result of the AND operation on the operand bytes from {@code _a} in the range [{@code _aStart}..{@code _aStart}+{@code _n}), and the operand bytes from
     * {@code _b} in the range [{@code _bStart}..{@code _bStart}+{@code _n}).  The result is {@code _n} bytes long.
     *
     * @param _a The array A of source bytes.
     * @param _aStart The start of the range of source bytes in A.
     * @param _b The array B of source bytes.
     * @param _bStart The start of the range of source bytes in B.
     * @param _n The number of bytes in the ranges of source bytes, and in the results.
     * @return The result of ANDing the range of source bytes in A with the range of source bytes in B.
     * @throws IllegalArgumentException if the source bytes are null, or if any part of the given range is outside the given source bytes.
     */
    public static byte[] and( final byte[] _a, final int _aStart, final byte[] _b, final int _bStart, final int _n ) {

        // sanity checks...
        if( isNull( _a, _b ) ) throw new IllegalArgumentException( "_a or _b is null" );
        if( (_aStart < 0) || (_bStart < 0) || (_n < 0) ) throw new IllegalArgumentException( "_aStart or _bStart or _n is negative" );
        if( ((_aStart + _n) > _a.length) || ((_bStart + _n) > _b.length)) throw new IllegalArgumentException( "_a or _b does not contain the full source range" );

        // make a place for our result...
        var result = new byte[_n];

        // do the actual AND operation...
        for( int i = 0; i < _n; i++ ) {
            result[i] = (byte)(_a[_aStart + i] & _b[_bStart+i]);
        }

        // ...and we're done!
        return result;
    }


    /**
     * Returns the result of the OR on the operand bytes from {@code _a} in the range [{@code _aStart}..{@code _aStart}+{@code _n}), and the operand bytes from
     * {@code _b} in the range [{@code _bStart}..{@code _bStart}+{@code _n}).  The result is {@code _n} bytes long.
     *
     * @param _a The array A of source bytes.
     * @param _aStart The start of the range of source bytes in A.
     * @param _b The array B of source bytes.
     * @param _bStart The start of the range of source bytes in B.
     * @param _n The number of bytes in the ranges of source bytes, and in the results.
     * @return The result of ORing the range of source bytes in A with the range of source bytes in B.
     * @throws IllegalArgumentException if the source bytes are null, or if any part of the given range is outside the given source bytes.
     */
    public static byte[] or( final byte[] _a, final int _aStart, final byte[] _b, final int _bStart, final int _n ) {

        // sanity checks...
        if( isNull( _a, _b ) ) throw new IllegalArgumentException( "_a or _b is null" );
        if( (_aStart < 0) || (_bStart < 0) || (_n < 0) ) throw new IllegalArgumentException( "_aStart or _bStart or _n is negative" );
        if( ((_aStart + _n) > _a.length) || ((_bStart + _n) > _b.length)) throw new IllegalArgumentException( "_a or _b does not contain the full source range" );

        // make a place for our result...
        var result = new byte[_n];

        // do the actual XOR operation...
        for( int i = 0; i < _n; i++ ) {
            result[i] = (byte)(_a[_aStart + i] | _b[_bStart+i]);
        }

        // ...and we're done!
        return result;
    }


    /**
     * Returns the result of the binary negation operation on the operand bytes from {@code _a} in the range [{@code _aStart}..{@code _aStart}+{@code _n}).  The result is
     * {@code _n} bytes long.
     *
     * @param _a The array A of source bytes.
     * @param _aStart The start of the range of source bytes in A.
     * @param _n The number of bytes in the range of source bytes, and in the results.
     * @return The result of ORing the range of source bytes in A with the range of source bytes in B.
     * @throws IllegalArgumentException if the source bytes are null, or if any part of the given range is outside the given source bytes.
     */
    public static byte[] negate( final byte[] _a, final int _aStart, final int _n ) {

        // sanity checks...
        //noinspection RedundantCast
        if( isNull( (Object)_a ) ) throw new IllegalArgumentException( "_a is null" );
        if( (_aStart < 0) || (_n < 0) ) throw new IllegalArgumentException( "_aStart or _n is negative" );
        if( ((_aStart + _n) > _a.length) ) throw new IllegalArgumentException( "_a does not contain the full source range" );

        // make a place for our result...
        var result = new byte[_n];

        // do the actual XOR operation...
        for( int i = 0; i < _n; i++ ) {
            result[i] = (byte)(~_a[_aStart + i]);
        }

        // ...and we're done!
        return result;
    }


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
        //noinspection RedundantCast
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
     * Returns the given byte array representing a big-Endian positive integer value adjusted to the given new length.  If the new length is less than the original length, an
     * {@link IllegalArgumentException} is thrown.  If the new length is longer than the original length, the result is the original byte array with zero bytes prepended as needed
     * to expand the array to the desired length.  Note that the returned array is <i>never</i> the given array.  Even if the new length is the same as the given byte array's
     * length, a copy is made and returned.
     *
     * @param _bytes The byte array to adjust the length of
     * @param _newLength The new length for the byte array.
     * @return The byte array with its length adjusted.
     * @throws IllegalArgumentException if the given bytes are null or the new length is negative
     */
    public static byte[] adjustNumeric( final byte[] _bytes, final int _newLength ) {

        // sanity check...
        //noinspection RedundantCast
        if( isNull( (Object)_bytes ) ) throw new IllegalArgumentException( "_bytes is null" );
        if( _newLength < _bytes.length ) throw new IllegalArgumentException( "_newLength is less than the given array's length" );

        // create our result array of the right length...
        var result = new byte[_newLength];

        // copy the right number of bytes into our result...
        System.arraycopy( _bytes, 0, result, _newLength - _bytes.length, _bytes.length );

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
