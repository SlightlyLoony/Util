package com.dilatush.util;

import static com.dilatush.util.General.isNull;

/**
 * <p>Instances of this class represent integer values in the range [0..144,682,570,706,075,775] as sequences of one to eight bytes.  Note that all
 * valid values are positive; instances of this class cannot represent negative values.  This class is useful in applications where smaller values are
 * more common than larger values, and the smaller size is valuable.  Examples include network packet serialization and storage serialization.</p><p>
 * The most significant bits of the first byte encode the number of bytes in the value, as follows:
 * </p><p>
 * 0XXXXXXX: one byte; the least significant 7 bits directly encode values in the range [0..127].
 * </p><p>
 * 10XXXXXX: two bytes; the least significant 14 bits encode (offset 128) values in the range [128..16,511].
 * </p><p>
 * 110XXXXX: three bytes; the least significant 21 bits encode (offset ) values in the range [16,512..2,113,663].
 * </p><p>
 * 1110XXXX: four bytes; the least significant 28 bits encode (offset ) values in the range [2,113,664..270,549,119].
 * </p><p>
 * 11110XXX: five bytes; the least significant 35 bits encode (offset ) values in the range [270,549,120..34,630,287,487].
 * </p><p>
 * 111110XX: six bytes; the least significant 42 bits encode (offset ) values in the range [34,630,287,488..4,432,676,798,591].
 * </p><p>
 * 1111110X: seven bytes; the least significant 49 bits encode (offset ) values in the range [4,432,676,798,592..567,382,630,219,903].
 * </p><p>
 * 1111111X: eight bytes; the least significant 57 bits encode (offset ) values in the range [567,382,630,219,904..144,682,570,706,075,775].
 * </p><p>
 * Instances of this class are immutable and threadsafe.
 * </p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class VarInt {

    private static final long MAX_1_BYTE_VALUE =               -1 + (1L <<  7);
    private static final long MAX_2_BYTE_VALUE = MAX_1_BYTE_VALUE + (1L << 14);
    private static final long MAX_3_BYTE_VALUE = MAX_2_BYTE_VALUE + (1L << 21);
    private static final long MAX_4_BYTE_VALUE = MAX_3_BYTE_VALUE + (1L << 28);
    private static final long MAX_5_BYTE_VALUE = MAX_4_BYTE_VALUE + (1L << 35);
    private static final long MAX_6_BYTE_VALUE = MAX_5_BYTE_VALUE + (1L << 42);
    private static final long MAX_7_BYTE_VALUE = MAX_6_BYTE_VALUE + (1L << 49);
    private static final long MAX_8_BYTE_VALUE = MAX_7_BYTE_VALUE + (1L << 57);

    private final long decodedValue;
    private final byte[] encodedValue;


    /**
     * Constructs a new instance of this class from the given long integer value, which must be in the range [0..].
     *
     * @param _value
     *      the value to encode in a new instance of this class.
     */
    public VarInt( final long _value ) {

        if( (_value < 0) || (_value > MAX_8_BYTE_VALUE) )
            throw new IllegalArgumentException( "Value is out of range: " + _value );

        decodedValue = _value;

        if( _value <= MAX_1_BYTE_VALUE ) {
            encodedValue = new byte[] { (byte)_value };
        }
        else if( _value <= MAX_2_BYTE_VALUE ) {
            long offsetValue = _value - (MAX_1_BYTE_VALUE + 1);
            encodedValue = new byte[] {
                (byte) (0b1000_0000 | (offsetValue >>> 8)),
                (byte) offsetValue
            };
        }
        else if( _value <= MAX_3_BYTE_VALUE ) {
            long offsetValue = _value - (MAX_2_BYTE_VALUE + 1);
            encodedValue = new byte[] {
                (byte) (0b1100_0000 | (offsetValue >>> 16)),
                (byte) (0xFF & (offsetValue >>> 8)),
                (byte) offsetValue
            };
        }
        else if( _value <= MAX_4_BYTE_VALUE ) {
            long offsetValue = _value - (MAX_3_BYTE_VALUE + 1);
            encodedValue = new byte[] {
                    (byte) (0b1110_0000 | (offsetValue >>> 24)),
                    (byte) (0xFF & (offsetValue >>> 16)),
                    (byte) (0xFF & (offsetValue >>> 8)),
                    (byte) offsetValue
            };
        }
        else if( _value <= MAX_5_BYTE_VALUE ) {
            long offsetValue = _value - (MAX_4_BYTE_VALUE + 1);
            encodedValue = new byte[] {
                    (byte) (0b1111_0000 | (offsetValue >>> 32)),
                    (byte) (0xFF & (offsetValue >>> 24)),
                    (byte) (0xFF & (offsetValue >>> 16)),
                    (byte) (0xFF & (offsetValue >>> 8)),
                    (byte) offsetValue
            };
        }
        else if( _value <= MAX_6_BYTE_VALUE ) {
            long offsetValue = _value - (MAX_5_BYTE_VALUE + 1);
            encodedValue = new byte[] {
                    (byte) (0b1111_1000 | (offsetValue >>> 40)),
                    (byte) (0xFF & (offsetValue >>> 32)),
                    (byte) (0xFF & (offsetValue >>> 24)),
                    (byte) (0xFF & (offsetValue >>> 16)),
                    (byte) (0xFF & (offsetValue >>> 8)),
                    (byte) offsetValue
            };
        }
        else if( _value <= MAX_7_BYTE_VALUE ) {
            long offsetValue = _value - (MAX_6_BYTE_VALUE + 1);
            encodedValue = new byte[] {
                    (byte) (0b1111_1100 | (offsetValue >>> 48)),
                    (byte) (0xFF & (offsetValue >>> 40)),
                    (byte) (0xFF & (offsetValue >>> 32)),
                    (byte) (0xFF & (offsetValue >>> 24)),
                    (byte) (0xFF & (offsetValue >>> 16)),
                    (byte) (0xFF & (offsetValue >>> 8)),
                    (byte) offsetValue
            };
        }
        else {
            long offsetValue = _value - (MAX_7_BYTE_VALUE + 1);
            encodedValue = new byte[] {
                    (byte) (0b1111_1110 | (offsetValue >>> 56)),
                    (byte) (0xFF & (offsetValue >>> 48)),
                    (byte) (0xFF & (offsetValue >>> 40)),
                    (byte) (0xFF & (offsetValue >>> 32)),
                    (byte) (0xFF & (offsetValue >>> 24)),
                    (byte) (0xFF & (offsetValue >>> 16)),
                    (byte) (0xFF & (offsetValue >>> 8)),
                    (byte) offsetValue
            };
        }
    }


    /**
     * Constructs a new instance of this class from the given bytes.  Note that the number of bytes provided <i>must</i> match the number of bytes
     * encoded in the first byte.
     *
     * @param _bytes
     *      the bytes representing an encoded instance of this class.
     */
    public VarInt( final byte[] _bytes ) {

        if( isNull( (Object) _bytes ) || (_bytes.length == 0) )
            throw new IllegalArgumentException( "Bytes are missing or empty" );

        int bytes = numberOfEncodedBytes( _bytes[0] );

        if( bytes != _bytes.length )
            throw new IllegalArgumentException( "Actual number of bytes doesn't match encoded number of bytes (" +
                    _bytes.length + " vs. " + bytes + ")" );

        encodedValue = _bytes;
        switch( bytes ) {

            case 1:
                decodedValue = encodedValue[0];
                break;

            case 2:
                decodedValue = MAX_1_BYTE_VALUE + 1 + (
                        ((encodedValue[0] & 0x3F) << 8) |
                         (encodedValue[1] & 0xFF));
                break;

            case 3:
                decodedValue = MAX_2_BYTE_VALUE + 1 + (
                        ((encodedValue[0] & 0x1F) << 16) |
                        ((encodedValue[1] & 0xFF) <<  8) |
                         (encodedValue[2] & 0xFF));
                break;

            case 4:
                decodedValue = MAX_3_BYTE_VALUE + 1 + (
                        ((encodedValue[0] & 0x0F) << 24) |
                        ((encodedValue[1] & 0xFF) << 16) |
                        ((encodedValue[2] & 0xFF) <<  8) |
                         (encodedValue[3] & 0xFF));
                break;

            case 5:
                decodedValue = MAX_4_BYTE_VALUE + 1 + (
                        ((encodedValue[0] & 0x07L) << 32) |
                        ((encodedValue[1] & 0xFFL) << 24) |
                        ((encodedValue[2] & 0xFFL) << 16) |
                        ((encodedValue[3] & 0xFFL) <<  8) |
                         (encodedValue[4] & 0xFFL));
                break;

            case 6:
                decodedValue = MAX_5_BYTE_VALUE + 1 + (
                        ((encodedValue[0] & 0x03L) << 40) |
                        ((encodedValue[1] & 0xFFL) << 32) |
                        ((encodedValue[2] & 0xFFL) << 24) |
                        ((encodedValue[3] & 0xFFL) << 16) |
                        ((encodedValue[4] & 0xFFL) <<  8) |
                         (encodedValue[5] & 0xFFL));
                break;

            case 7:
                decodedValue = MAX_6_BYTE_VALUE + 1 + (
                        ((encodedValue[0] & 0x01L) << 48) |
                        ((encodedValue[1] & 0xFFL) << 40) |
                        ((encodedValue[2] & 0xFFL) << 32) |
                        ((encodedValue[3] & 0xFFL) << 24) |
                        ((encodedValue[4] & 0xFFL) << 16) |
                        ((encodedValue[5] & 0xFFL) <<  8) |
                         (encodedValue[6] & 0xFFL));
                break;

            case 8:
                decodedValue = MAX_7_BYTE_VALUE + 1 + (
                        ((encodedValue[0] & 0x01L) << 56) |
                        ((encodedValue[1] & 0xFFL) << 48) |
                        ((encodedValue[2] & 0xFFL) << 40) |
                        ((encodedValue[3] & 0xFFL) << 32) |
                        ((encodedValue[4] & 0xFFL) << 24) |
                        ((encodedValue[5] & 0xFFL) << 16) |
                        ((encodedValue[6] & 0xFFL) <<  8) |
                         (encodedValue[7] & 0xFFL));
                break;

            // this is here only to eliminate a compiler error (possible uninitialized variable)...
            default:
                decodedValue = 0;
        }
    }


    /**
     * Given the first byte of an encoded instance of this class, returns the number of bytes required for the complete encoding.
     *
     * @param _firstByte
     * @return
     */
    public static int numberOfEncodedBytes( final byte _firstByte ) {
        return Integer.numberOfLeadingZeros( ~(0xFFFFFF00 | (0xFE & _firstByte) ) ) - 23;
    }


    public long decodedValueAsLong() {
        return decodedValue;
    }

    
    public int decodedValueAsInt() { return (int) decodedValue; }
    

    public byte[] encodedValue() {
        return encodedValue;
    }


    public int encodedLength() {
        return encodedValue.length;
    }


    @Override
    public boolean equals( final Object _o ) {
        if( this == _o ) return true;
        if( _o == null || getClass() != _o.getClass() ) return false;

        VarInt varInt = (VarInt) _o;

        return decodedValue == varInt.decodedValue;
    }


    @Override
    public int hashCode() {
        return (int) (decodedValue ^ (decodedValue >>> 32));
    }
}
