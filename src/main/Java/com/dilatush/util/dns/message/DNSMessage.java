package com.dilatush.util.dns.message;

import com.dilatush.util.Checks;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.rr.DNSResourceRecord;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.*;

import static com.dilatush.util.General.isNull;

//   +----------------------------------------+
//   | See RFC 1035 and RFC 5395 for details. |
//   +----------------------------------------+

/**
 * Instances of this class represent DNS messages of any kind.  Instances of this class may be created either by using an instance of the
 * {@link Builder} class or by decoding a message (presumably received over the wire) in a {@link ByteBuffer} instance.  Instance of this class are
 * immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class DNSMessage {

    // the buffer sizes that we'll try while encoding...
    private static final int[] MAX_DNS_MESSAGE_BUFFER_SIZES = new int[] { 512, 8192 + 2, 16384 + 2, 65536 + 2 };

    private static final Outcome.Forge<DNSMessage> outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<ByteBuffer> encodeOutcome = new Outcome.Forge<>();


    /** The 16-bit unsigned integer ID supplied the application creating the message. */
    public final int                     id;                     // 16-bit integer ID supplied by application...

    /** This field is {@code true} if this message is a response, {@code false} if it is a query. */
    public final boolean                 isResponse;             // true for query, false for response...

    /** The operation code for this message. */
    public final DNSOpCode               opCode;                 // the op code for the message (set in query, copied to response)...

    /** This field is {@code true} in a response if the answers provided are authoritative. */
    public final boolean                 authoritativeAnswer;    // valid in response only...

    /** This field is {@code true} if the message was truncated during transmission. */
    public final boolean                 truncated;              // message was truncated by the transmission channel...

    /** This field is set {@code true} in a query if recursion is desired; in a response it is {@code true} if recursion was used. */
    public final boolean                 recurse;                // true if recursion is desired (set in query, copied to response)...

    /** This field is {@code true} in a response if the server or resolver can use recursion. */
    public final boolean                 canRecurse;             // valid in response only, true if name server can recursively answer queries...

    /** This field is reserved for future use; must be {@code false} until a use is assigned. */
    public final boolean                 z;                      // reserved for future use, must be false (0) in all messages...

    /** This field is {@code true} in responses if the answers and authorities in this response have been authenticated. */
    public final boolean                 authenticated;          // valid in response only; data in answers and authorities has been authenticated...

    /** This field is {@code true} in a query if the resolver will accept non-authenticated answers and authorities. */
    public final boolean                 checkingDisabled;       // valid in query only; true if resolver will accept non-authenticated answers and authorities...

    /** In a response, this field indicates whether the response was ok, or had a problem. */
    public final DNSResponseCode         responseCode;           // valid in responses only; ok or type of error...

    /** In a query, the questions to ask the server.  These questions are copied to the server's response. */
    public final List<DNSQuestion>       questions;              // the questions in this message...

    /** In a response, the resource record answers to the questions asked in the query. */
    public final List<DNSResourceRecord> answers;                // the answers in this message...

    /** In a response, name server resource records for the name servers that provided the answers. */
    public final List<DNSResourceRecord> authorities;            // the authorities in this message...

    /** In a response, resource records of any type that were not the answers actually asked for, but which may contain useful additional information. */
    public final List<DNSResourceRecord> additionalRecords;      // the additional records in this message...


    /**
     * Creates a new instance of this class with the given parameters.  Note that this constructor is private, and is used only by {@link Builder}.
     *
     * @param _id The 16-bit unsigned integer ID supplied the application creating the message.
     * @param _isResponse {@code true} if this message is a response, {@code false} if it is a query.
     * @param _opCode The operation code for this message.
     * @param _authoritativeAnswer {@code true} in a response if the answers provided are authoritative.
     * @param _truncated {@code true} if the message was truncated during transmission.
     * @param _recurse {@code true} in a query if recursion is desired; in a response it is {@code true} if recursion was used.
     * @param _canRecurse {@code true} in a response if the server or resolver can use recursion.
     * @param _z reserved for future use; must be {@code false} until a use is assigned.
     * @param _authenticated {@code true} in responses if the answers and authorities in this response have been authenticated.
     * @param _checkingDisabled {@code true} in a query if the resolver will accept non-authenticated answers and authorities.
     * @param _responseCode In a response, this field indicates whether the response was ok, or had a problem.
     * @param _questions In a query, the questions to ask the server.  These questions are copied to the server's response.
     * @param _answers In a response, the resource record answers to the questions asked in the query.
     * @param _authorities In a response, name server resource records for the name servers that provided the answers.
     * @param _additionalRecords In a response, resource records of any type that were not the answers actually asked for, but which may contain
     *                           useful additional information.
     */
    private DNSMessage(
            final int _id, final boolean _isResponse, DNSOpCode _opCode, final boolean _authoritativeAnswer, final boolean _truncated,
            final boolean _recurse, final boolean _canRecurse, final boolean _z, final boolean _authenticated, final boolean _checkingDisabled,
            final DNSResponseCode _responseCode, final List<DNSQuestion> _questions, final List<DNSResourceRecord> _answers,
            final List<DNSResourceRecord> _authorities, final List<DNSResourceRecord> _additionalRecords) {

        id                  = _id;
        isResponse          = _isResponse;
        opCode              = _opCode;
        authoritativeAnswer = _authoritativeAnswer;
        truncated           = _truncated;
        recurse             = _recurse;
        canRecurse          = _canRecurse;
        z                   = _z;
        authenticated       = _authenticated;
        checkingDisabled    = _checkingDisabled;
        responseCode        = _responseCode;
        questions           = Collections.unmodifiableList( _questions         );
        answers             = Collections.unmodifiableList( _answers           );
        authorities         = Collections.unmodifiableList( _authorities       );
        additionalRecords   = Collections.unmodifiableList( _additionalRecords );
    }


    /**
     * Synthesizes a response message with the given answers based on this message, which must be a query.
     *
     * @param _answers The answers for this response message.
     * @return The synthetic response message.
     */
    public DNSMessage getSyntheticResponse( final List<DNSResourceRecord> _answers ) {

        if( isResponse || (opCode != DNSOpCode.QUERY) )
            throw new UnsupportedOperationException( "Can synthesize responses only for query messages" );

        Checks.required( _answers );

        List<DNSResourceRecord> empty = new ArrayList<>(0);

        return new DNSMessage(
                id, true, opCode, authoritativeAnswer, false, recurse, canRecurse, z, false, checkingDisabled, DNSResponseCode.OK,
                questions, _answers, empty, empty );
    }


    /**
     * <p>Attempt to encode this instance into the wire format for a DNS message, into a {@link ByteBuffer} instance.  If the attempt is successful,
     * an ok {@link Outcome Outcome&lt;ByteBuffer&gt;} is returned containing the {@link ByteBuffer} with the encoded instance.  If the attempt was
     * unsuccessful, a not ok outcome is returned, with a message explaining the problem encountered.</p>
     * <p>Internally this method first attempts to encode into a 512-byte buffer (the maximum size of a UDP DNS packet).  If that attempt fails, it
     * will try several successively larger buffers until either it fits, or we've determined that it cannot fit into the 65538 bytes that is the
     * maximum size for a TCP DNS packet.</p>
     *
     * @return the {@link Outcome Outcome&lt;ByteBuffer&gt;} with the results of the encoding attempt.
     */
    public Outcome<ByteBuffer> encode() {

        // try successively larger buffers to encode into...
        bufferSizes:
        for( int maxDnsMessageBufferSize : MAX_DNS_MESSAGE_BUFFER_SIZES ) {

            // create the buffer to contain our result...
            ByteBuffer msgBuffer = ByteBuffer.allocate( maxDnsMessageBufferSize );  // by default, it's big-endian...

            // create an empty offsets map for the domain name compression mechanism...
            Map<String,Integer> offsets = new HashMap<>();

            // we don't need to check whether we have enough space for the header, as we know we just allocated this thing...
            // so we can just jump into encoding the header...

            // stuff the app-supplied id away...
            msgBuffer.putShort( (short) id );

            // fabricate and stuff away our sixteen bits of flags and codes, working from the LSBs up...
            int x;
            x = responseCode.code;
            x |= checkingDisabled ? 0x0010 : 0;
            x |= authenticated ? 0x0020 : 0;
            x |= z ? 0x0040 : 0;
            x |= canRecurse ? 0x0080 : 0;
            x |= recurse ? 0x0100 : 0;
            x |= truncated ? 0x0200 : 0;
            x |= authoritativeAnswer ? 0x0400 : 0;
            x |= (opCode.code << 11);
            x |= isResponse ? 0x8000 : 0;
            msgBuffer.putShort( (short) x );

            // stuff away our four counts for questions and resource records...
            msgBuffer.putShort( (short) questions.size() );
            msgBuffer.putShort( (short) answers.size() );
            msgBuffer.putShort( (short) authorities.size() );
            msgBuffer.putShort( (short) additionalRecords.size() );

            Outcome<?> result;

            // encode our questions...
            for( DNSQuestion question : questions ) {
                result = question.encode( msgBuffer, offsets );
                if( result.ok() ) continue;
                if( result.cause() instanceof BufferOverflowException ) continue bufferSizes;
                return encodeOutcome.notOk( result.msg() );
            }

            // encode our answers...
            for( DNSResourceRecord answer : answers ) {
                result = answer.encode( msgBuffer, offsets );
                if( result.ok() ) continue;
                if( result.cause() instanceof BufferOverflowException ) continue bufferSizes;
                return encodeOutcome.notOk( result.msg() );
            }

            // encode our authorities...
            for( DNSResourceRecord authority : authorities ) {
                result = authority.encode( msgBuffer, offsets );
                if( result.ok() ) continue;
                if( result.cause() instanceof BufferOverflowException ) continue bufferSizes;
                return encodeOutcome.notOk( result.msg() );
            }

            // encode our additional records...
            for( DNSResourceRecord additionalRecord : additionalRecords ) {
                result = additionalRecord.encode( msgBuffer, offsets );
                if( result.ok() ) continue;
                if( result.cause() instanceof BufferOverflowException ) continue bufferSizes;
                return encodeOutcome.notOk( result.msg() );
            }

            // if we make it here, then we've encoded the whole thing - flip the buffer and skedaddle...
            msgBuffer.flip();
            return encodeOutcome.ok( msgBuffer );
        }

        // if we get here, that means we couldn't fit the message even into the largest possible TCP message...
        return encodeOutcome.notOk( "Message is too large to encode (over 64k bytes)" );
    }


    /**
     * Attempt to decode the DNS message in wire format contained in the given message {@link ByteBuffer}.  The buffer must have a limit of the
     * received message's size, and a position of zero.  If the attempt was successful, an ok {@link Outcome Outcome&lt;DNSMessage&gt;} is returned,
     * containing the decoded DNS message.  If unsuccessful, a not ok outcome is returned with a message explaining the problem encountered.
     *
     * @param _msgBuffer The {@link ByteBuffer} containing the DNS message to be decoded.
     * @return The {@link Outcome Outcome&lt;DNSMessage&gt;} with the results of the decoding attempt.
     */
    public static Outcome<DNSMessage> decode( final ByteBuffer _msgBuffer ) {

        // make sure we actually HAVE a message buffer...
        if( isNull( _msgBuffer) )
            return outcome.notOk( "Missing message buffer" );

        // instantiate a builder for us to build the decoded message as we go...
        Builder builder = new Builder();

        // if the message buffer is smaller than the DNS message header, we have a problem...
        if( _msgBuffer.remaining() < 12 )
            return outcome.notOk( "Message buffer is smaller than message header" );

        // decode the 16-bit ID value...
        builder.setId( 0xFFFF & _msgBuffer.getShort() );

        // decode the 16-bits of flags and codes, from the MSB on down...
        int flags = 0xFFFF & _msgBuffer.getShort();
        builder.setResponse(                  (flags & 0x8000) != 0 );
        builder.setOpCode(                    DNSOpCode.fromCode( 0x0F & (flags >> 11) ) );
        builder.setAuthoritativeAnswer(       (flags & 0x0400) != 0 );
        builder.setTruncated(                 (flags & 0x0200) != 0 );
        builder.setRecurse(                   (flags & 0x0100) != 0 );
        builder.setCanRecurse(                (flags & 0x0080) != 0 );
        builder.setZ(                         (flags & 0x0040) != 0 );
        builder.setAuthenticated(             (flags & 0x0020) != 0 );
        builder.setCheckingDisabled(          (flags & 0x0010) != 0 );
        builder.setResponseCode(              DNSResponseCode.fromCode( flags & 0x0F ) );

        // decode the four 16-bit question and resource record counts...
        int quc = 0xFFFF & _msgBuffer.getShort();
        int anc = 0xFFFF & _msgBuffer.getShort();
        int auc = 0xFFFF & _msgBuffer.getShort();
        int adc = 0xFFFF & _msgBuffer.getShort();

        // decode any questions...
        for( int i = 0; i < quc; i++ ) {
            Outcome<DNSQuestion> questionOutcome = DNSQuestion.decode( _msgBuffer );
            if( questionOutcome.notOk() )
                return outcome.notOk( questionOutcome.msg() );
            builder.addQuestion( questionOutcome.info() );
        }

        // decode any answer resource records...
        for( int i = 0; i < anc; i++ ) {
            Outcome<? extends DNSResourceRecord> answerOutcome = DNSResourceRecord.decode( _msgBuffer );
            if( answerOutcome.notOk() )
                return outcome.notOk( answerOutcome.msg() );
            builder.addAnswer( answerOutcome.info() );
        }

        // decode any authorities resource records...
        for( int i = 0; i < auc; i++ ) {
            Outcome<? extends DNSResourceRecord> authorityOutcome = DNSResourceRecord.decode( _msgBuffer );
            if( authorityOutcome.notOk() )
                return outcome.notOk( authorityOutcome.msg() );
            builder.addAuthority( authorityOutcome.info() );
        }

        // decode any additional resource records...
        for( int i = 0; i < adc; i++ ) {
            Outcome<? extends DNSResourceRecord> additionalOutcome = DNSResourceRecord.decode( _msgBuffer );
            if( additionalOutcome.notOk() )
                return outcome.notOk( additionalOutcome.msg() );
            builder.addAdditionalRecord( additionalOutcome.info() );
        }

        // if we get here, then everything decoded ok - so skedaddle with the results...
        return outcome.ok( builder.getMessage() );
    }


    /**
     * Instances of this class are used to build instances of {@link DNSMessage}.  Instances of this class are <i>not</i> immutable and are
     * <i>not</i> threadsafe.
     */
    public static class Builder {

        private       int                     id;                     // 16-bit integer ID supplied by application...
        private       boolean                 isResponse;             // true for response, false for query...
        private       DNSOpCode               opCode;                 // the op code for the message (set in query, copied to response)...
        private       boolean                 authoritativeAnswer;    // valid in response only...
        private       boolean                 truncated;              // message was truncated by the transmission channel...
        private       boolean                 recurse;                // true if recursion is desired (set in query, copied to response)...
        private       boolean                 canRecurse;             // valid in response only, true if name server can recursively answer queries...
        private       boolean                 z;                      // reserved for future use, must be false (0) in all messages...
        private       boolean                 authenticated;          // valid in response only; data in answers and authorities has been authenticated...
        private       boolean                 checkingDisabled;       // valid in query only; true if resolver will accept non-authenticated answers and authorities...
        private       DNSResponseCode         responseCode;           // valid in responses only; ok or type of error...
        private final List<DNSQuestion>       questions;              // the questions in this message...
        private final List<DNSResourceRecord> answers;                // the answers in this message...
        private final List<DNSResourceRecord> authorities;            // the authorities in this message...
        private final List<DNSResourceRecord> additionalRecords;      // the additional records in this message...


        /**
         * Create a new instance of this class, with all boolean fields initialized to {@code false}, the operation code initialized to
         * {@link DNSOpCode#QUERY}, the response code initialized to {@link DNSResponseCode#OK}, and zero questions, answers, authorities, and
         * additional records.
         */
        public Builder() {

            opCode            = DNSOpCode.QUERY;
            responseCode      = DNSResponseCode.OK;
            questions         = new ArrayList<>();
            answers           = new ArrayList<>();
            authorities       = new ArrayList<>();
            additionalRecords = new ArrayList<>();
        }


        /**
         * Returns a new instance of {@link DNSMessage} created from the current state of this instance.
         *
         * @return The new instance of {@link DNSMessage}.
         */
        public DNSMessage getMessage() {
            return new DNSMessage(
                    id, isResponse, opCode, authoritativeAnswer, truncated, recurse, canRecurse, z, authenticated,
                    checkingDisabled, responseCode, questions, answers, authorities, additionalRecords );
        }


        /**
         * Add the given {@link DNSQuestion} to the questions in this instance.
         *
         * @param _question The {@link DNSQuestion} question to add.
         */
        public void addQuestion( final DNSQuestion _question ) {
            questions.add( _question );
        }


        /**
         * Add the given {@link DNSResourceRecord} to the answers in this instance.
         *
         * @param _resourceRecord The {@link DNSResourceRecord} answer to add.
         */
        public void addAnswer( final DNSResourceRecord _resourceRecord ) {
            answers.add( _resourceRecord );
        }


        /**
         * Add the given {@link DNSResourceRecord} to the authorities in this instance.
         *
         * @param _resourceRecord The {@link DNSResourceRecord} authority to add.
         */
        public void addAuthority( final DNSResourceRecord _resourceRecord ) {
            authorities.add( _resourceRecord );
        }


        /**
         * Add the given {@link DNSResourceRecord} to the additional records in this instance.
         *
         * @param _resourceRecord The {@link DNSResourceRecord} additional record to add.
         */
        public void addAdditionalRecord( final DNSResourceRecord _resourceRecord ) {
            additionalRecords.add( _resourceRecord );
        }


        /**
         * Set the ID to the given value (default is zero).
         *
         * @param _id The value to set the ID to.
         */
        public void setId( final int _id ) {
            id = _id;
        }


        /**
         * Set the response/query flag ({@code true} for a response, {@code false} for a query; default is {@code false} for query).
         *
         * @param _response The response/query flag.
         */
        public void setResponse( final boolean _response ) {
            isResponse = _response;
        }


        /**
         * Set the {@link DNSOpCode} for this message (default is {@link DNSOpCode#QUERY}).
         *
         * @param _opCode The {@link DNSOpCode}.
         */
        public void setOpCode( final DNSOpCode _opCode ) {
            opCode = _opCode;
        }


        /**
         * Set the authoritative answer flag ({@code true} for a response with an authoritative answer; default is {@code false}).
         *
         * @param _authoritativeAnswer The authoritative answer flag.
         */
        public void setAuthoritativeAnswer( final boolean _authoritativeAnswer ) {
            authoritativeAnswer = _authoritativeAnswer;
        }


        /**
         * Set the truncated message flag ({@code true} for a response that has been truncated; default is {@code false}).
         *
         * @param _truncated The truncated message flag.
         */
        public void setTruncated( final boolean _truncated ) {
            truncated = _truncated;
        }


        /**
         * Set the recurse flag ({@code true} in a query when recursive resolution is wanted, which is usually the case; default is {@code false}).
         *
         * @param _recurse The recurse flag.
         */
        public void setRecurse( final boolean _recurse ) {
            recurse = _recurse;
        }


        /**
         * In a response, set the "can recurse" flag ({@code true} if the server can resolve recursively; default is {@code false}).
         *
         * @param _canRecurse The "can recurse" flag.
         */
        public void setCanRecurse( final boolean _canRecurse ) {
            canRecurse = _canRecurse;
        }


        /**
         * Set the "z" flag, which is currently not used and must be {@code false} until and if the standards are changed.  The default is
         * {@code false}.
         *
         * @param _z The "z" flag.
         */
        public void setZ( final boolean _z ) {
            z = _z;
        }


        /**
         * In a response, set the authenticated flag ({@code true} if the answers and authorities are authenticated; default is {@code false}).
         *
         * @param _authenticated The authenticated flag.
         */
        public void setAuthenticated( final boolean _authenticated ) {
            authenticated = _authenticated;
        }


        /**
         * In a query, set the checking disabled flag ({@code true} if the client will not check for authenticated answers and authorities; default
         * is {@code false}).
         *
         * @param _checkingDisabled The checking disabled flag.
         */
        public void setCheckingDisabled( final boolean _checkingDisabled ) {
            checkingDisabled = _checkingDisabled;
        }


        /**
         * In a response, set the {@link DNSResponseCode} (default is {@link DNSResponseCode#OK}).
         *
         * @param _responseCode The DNS response code.
         */
        public void setResponseCode( final DNSResponseCode _responseCode ) {
            responseCode = _responseCode;
        }
    }
}
