package com.dilatush.util.dns.rr;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSRRClass;
import com.dilatush.util.dns.message.DNSRRType;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class represent a DNS TXT Resource Record.  Nominally these contain any number of text strings (though the encoding is
 * undefined), but technically they can contain any binary data.  This implementation presents the actual data as a list of {@link ByteBuffer}s (one
 * for each "string") and (as a convenience) as a list of {@link String}s decoded as ASCII.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TXT extends DNSResourceRecord {

    private static final Outcome.Forge<TXT> outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<?>   encodeOutcome = new Outcome.Forge<>();

    public final List<ByteBuffer> data;

    public final List<String> ascii;


    /**
     * Create a new instance of this class with the given parameters.  Note that this constructor is private and is used only by factory methods and
     * decoders in this class.
     *
     * @param _name       The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass      The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl        This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _dataLength The length (in bytes) of this resource record's data (not including the resource record's header).
     * @param _data       The raw data "strings" contained in this resource record.
     * @param _ascii      The decoded (as ASCII) strings contained in this resource record.
     */
    private TXT(
            final DNSDomainName _name, final DNSRRClass _klass, final long _ttl, final int _dataLength,
            final List<ByteBuffer> _data, final List<String> _ascii ) {

        super( new Init( _name, DNSRRType.TXT, _klass, _ttl, _dataLength ) );
        data  = Collections.unmodifiableList( _data );
        ascii = Collections.unmodifiableList( _ascii );
    }


    /**
     * Create a new instance of this class from the given parameters.  Returns an ok outcome with the newly created instance if there were no
     * problems.  Otherwise, returns a not ok outcome with an explanatory message.
     *
     * @param _name  The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _klass The {@link DNSRRClass} that this resource record pertains to (in the real world, always IN for Internet).
     * @param _ttl   This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _data  The raw data "strings" contained in this resource record.
     * @return The {@link Outcome Outcome&lt;TXT&gt;} with the result of this method.
     */
    public static Outcome<TXT> create(
            final DNSDomainName _name, final DNSRRClass _klass, final int _ttl,
            final List<ByteBuffer> _data ) {

        if( isNull( _name, _klass, _data ) )
            return outcome.notOk( "Missing argument (name, class, or data)" );

        // verify that the data is present, and is between 0 and 255 bytes long, and build decoded ASCII strings...
        List<String> strings = new ArrayList<>();
        for( ByteBuffer data : _data ) {

            // verify the data...
            if( data == null )
                return outcome.notOk( "Missing string data" );
            if( data.limit() > 255 )
                return outcome.notOk( "String data too long: " + data.limit() );

            // build our decoded ASCII string...
            strings.add( new String( data.array(), StandardCharsets.US_ASCII) );
        }

        return outcome.ok( new TXT( _name, _klass, _ttl, 4, _data, strings ) );
    }


    /**
     * Create a new instance of this class from the given parameters, with a {@link DNSRRClass} of "IN" (Internet).  Returns an ok outcome with the
     * newly created instance if there were no problems.  Otherwise, returns a not ok outcome with an explanatory message.
     *
     * @param _name  The {@link DNSDomainName} that the IP address in this class pertains to.
     * @param _ttl   This resource record's time-to-live (in a cache) in seconds, or zero for no caching.
     * @param _data  The raw data "strings" contained in this resource record.
     * @return The {@link Outcome Outcome&lt;TXT&gt;} with the result of this method.
     */
    public static Outcome<TXT> create(
            final DNSDomainName _name, final int _ttl,
            final List<ByteBuffer> _data ) {
        return create( _name, DNSRRClass.IN, _ttl, _data );
    }


    /**
     * Decode the resource record from the given DNS message {@link ByteBuffer}. On entry, the message buffer must be positioned at the first byte
     * of this resource record's data.  On exit, the message buffer will be positioned at the first byte following the last byte encoding this
     * resource record.  Returns an ok outcome with the decoded resource record instance if the decoding encountered no problems.  Otherwise, returns
     * a not ok outcome with an explanatory message about the problem encountered.
     *
     * @param _msgBuffer The {@link ByteBuffer} to decode this resource record from.
     * @return The {@link Outcome} of the decoding operation.
     */
    protected static Outcome<TXT> decode( final ByteBuffer _msgBuffer, final Init _init ) {

        // make our lists to contain the "strings" we decode...
        List<ByteBuffer> data = new ArrayList<>();
        List<String> ascii = new ArrayList<>();

        // we decode "strings" until we use up all our resource record data bytes...
        int bytes = _init.dataLength();
        while( bytes > 0 ) {

            // make sure we have a byte to fetch the length...
            if( _msgBuffer.remaining() < 1 )
                return outcome.notOk( "Message buffer underflow" );

            // get the length of this "string"...
            int stringLength = _msgBuffer.get() & 0xFF;

            // make sure we have the bytes to fetch the "string"...
            if( _msgBuffer.remaining() < stringLength )
                return outcome.notOk( "Message buffer underflow" );

            // get the "string" as bytes...
            ByteBuffer dst = ByteBuffer.allocate( stringLength );
            ByteBuffer src = _msgBuffer.duplicate();
            src.limit( src.position() + stringLength );
            dst.put( src );
            dst.flip();

            // update the message buffer's position...
            _msgBuffer.position( _msgBuffer.position() + stringLength );

            // get a decoded string from the raw bytes...
            String str = new String( dst.array(), StandardCharsets.US_ASCII );

            // add 'em to our arrays...
            data.add( dst.asReadOnlyBuffer() );
            ascii.add( str );

            // decrement our bytes count...
            bytes -= stringLength + 1;  // the +1 is to account for the length byte prefix...
        }

        // when we get here, we should have zero bytes remaining...
        if( bytes != 0 )
            return outcome.notOk( "Resource record data length does not match the decoded data length" );

        // if we made it here, then all is good, and we can leave with a shiny new instance...
        return outcome.ok( new TXT( _init.name(), _init.klass(), _init.ttl(), _init.dataLength(), data, ascii ) );
    }


    /**
     * Encode the resource data for the concrete resource record class.  On entry, the given DNS message {@link ByteBuffer} is positioned at the first
     * byte of the resource data, and the given map of name offsets contains pointers to all the previously encoded domain names.  On exit, the
     * message buffer must be positioned at the first byte following the resource data.  See {@link DNSDomainName#encode(ByteBuffer, Map)
     * DNSDomainName.encode(ByteBuffer,Map&lt;String,Integer&gt;)} for details about the message compression mechanism.  The outcome returned is ok if
     * the encoding was successful, and not ok (with a message) if there was a problem.  If the result was a buffer overflow, the outcome is not ok
     * with a cause of {@link BufferOverflowException}.
     *
     * @param _msgBuffer The {@link ByteBuffer} to encode this resource record into.
     * @param _nameOffsets The map of domain and sub-domain names that have been directly encoded, and their associated offset.
     * @return the {@link Outcome}, either ok or not ok with an explanatory message.
     */
    @Override
    protected Outcome<?> encodeChild( ByteBuffer _msgBuffer, Map<String, Integer> _nameOffsets ) {

        // iterate over all the "strings" as raw bytes...
        for( ByteBuffer src : data ) {

            // make sure we have room in our buffer to encode this "string"...
            if( _msgBuffer.remaining() < src.remaining() + 1 )  // the +1 is to allow for the length prefix...
                return encodeOutcome.notOk( new BufferOverflowException() );

            // encode the length prefix...
            _msgBuffer.put( (byte) src.remaining() );

            // encode the actual bytes...
            _msgBuffer.put( src.array() );
        }

        // if we got here, then all is well...
        return encodeOutcome.ok();
    }


    /**
     * Return a string representing this instance.
     *
     * @return a string representing this instance.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for( String str : ascii ) {
            sb.append( str );
            sb.append( "\n" );
        }
        return type.name() + ": \n" + sb + super.toString();
    }
}
