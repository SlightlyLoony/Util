package com.dilatush.util.dns;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.rr.DNSResourceRecord;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import static com.dilatush.util.General.isNull;

//   +----------------------------------------+
//   | See RFC 1035 and RFC 5395 for details. |
//   +----------------------------------------+

/**
 * Instances of this class represent DNS messages (of any kind).
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class DNSMessage {

    private static final int MAX_DNS_MESSAGE_SIZE = 512;
    private static final Outcome.Forge<DNSMessage> outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<ByteBuffer> encodeOutcome = new Outcome.Forge<>();


    public final int                     id;                     // 16 bit integer ID supplied by application...
    public final boolean                 isResponse;             // true for query, false for response...
    public final DNSOpCode               opCode;                 // the op code for the message (set in query, copied to response)...
    public final boolean                 authoritativeAnswer;    // valid in response only...
    public final boolean                 truncated;              // message was truncated by the transmission channel...
    public final boolean                 recurse;                // true if recursion is desired (set in query, copied to response)...
    public final boolean                 canRecurse;             // valid in response only, true if name server can recursively answer queries...
    public final boolean                 z;                      // reserved for future use, must be false (0) in all messages...
    public final boolean                 authenticated;          // valid in response only; data in answers and authorities has been authenticated...
    public final boolean                 checkingDisabled;       // valid in query only; true if resolver will accept non-authenticated answers and authorities...
    public final DNSResponseCode         responseCode;           // valid in responses only; ok or type of error...
    public final List<DNSQuestion>       questions;              // the questions in this message...
    public final List<DNSResourceRecord> answers;                // the answers in this message...
    public final List<DNSResourceRecord> authorities;            // the authorities in this message...
    public final List<DNSResourceRecord> additionalRecords;      // the additional records in this message...


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


    public Outcome<ByteBuffer> encode() {

        ByteBuffer msgBuffer = ByteBuffer.allocate( MAX_DNS_MESSAGE_SIZE );  // by default, it's big-endian...
        Map<String,Integer> offsets = new HashMap<>();

        // we don't need to check whether we have enough space for the header, as we know we just allocated this thing...
        // so we can just jump into encoding the header...

        // stuff the app-supplied id away...
        msgBuffer.putShort( (short) id );

        // fabricate and stuff away our sixteen bits of flags and codes, working from the LSBs up...
        int x;
        x  = responseCode.code;
        x |= checkingDisabled    ? 0x0010 : 0;
        x |= authenticated       ? 0x0020 : 0;
        x |= z                   ? 0x0040 : 0;
        x |= canRecurse          ? 0x0080 : 0;
        x |= recurse             ? 0x0100 : 0;
        x |= truncated           ? 0x0200 : 0;
        x |= authoritativeAnswer ? 0x0400 : 0;
        x |= (opCode.code << 11);
        x |= isResponse ? 0x8000 : 0;
        msgBuffer.putShort( (short)x );

        // stuff away our four counts for questions and resource records...
        msgBuffer.putShort( (short)questions.size()         );
        msgBuffer.putShort( (short)answers.size()           );
        msgBuffer.putShort( (short)authorities.size()       );
        msgBuffer.putShort( (short)additionalRecords.size() );

        Outcome<?> result;

        // encode our questions...
        for( DNSQuestion question : questions ) {
            result = question.encode( msgBuffer, offsets );
            if( result.notOk() )
                return encodeOutcome.notOk( result.msg() );
        }

        // encode our answers...
        for( DNSResourceRecord answer : answers ) {
            result = answer.encode( msgBuffer, offsets );
            if( result.notOk() )
                return encodeOutcome.notOk( result.msg() );
        }

        // encode our authorities...
        for( DNSResourceRecord authority : authorities ) {
            result = authority.encode( msgBuffer, offsets );
            if( result.notOk() )
                return encodeOutcome.notOk( result.msg() );
        }

        // encode our additional records...
        for( DNSResourceRecord additionalRecord : additionalRecords ) {
            result = additionalRecord.encode( msgBuffer, offsets );
            if( result.notOk() )
                return encodeOutcome.notOk( result.msg() );
        }

        // if we make it here, then we've encoded the whole thing - flip the buffer and skedaddle...
        msgBuffer.flip();
        return encodeOutcome.ok( msgBuffer );
    }


    public static Outcome<DNSMessage> decode( final ByteBuffer _msgBuffer ) {

        if( isNull( _msgBuffer) )
            return outcome.notOk( "Missing message buffer" );

        Builder builder = new Builder();

        if( _msgBuffer.remaining() < 12 )
            return outcome.notOk( "Message buffer is smaller than message header" );

        builder.setId( 0xFFFF & _msgBuffer.getShort() );
        int flags = 0xFFFF & _msgBuffer.getShort();
        builder.setResponse( (flags & 0x8000) != 0 );
        builder.setOpCode( DNSOpCode.fromCode( 0x0F & (flags >> 11) ) );
        builder.setAuthoritativeAnswer( (flags & 0x0400) != 0 );
        builder.setTruncated( (flags & 0x0200) != 0 );
        builder.setRecurse( (flags & 0x0100) != 0 );
        builder.setCanRecurse( (flags & 0x0080) != 0 );
        builder.setZ( (flags & 0x0040) != 0 );
        builder.setAuthenticated( (flags & 0x0020) != 0 );
        builder.setCheckingDisabled( (flags & 0x0010) != 0 );
        builder.setResponseCode( DNSResponseCode.fromCode( flags & 0x0F ) );

        int quc = 0xFFFF & _msgBuffer.getShort();
        int anc = 0xFFFF & _msgBuffer.getShort();
        int auc = 0xFFFF & _msgBuffer.getShort();
        int adc = 0xFFFF & _msgBuffer.getShort();

        for( int i = 0; i < quc; i++ ) {
            Outcome<DNSQuestion> questionOutcome = DNSQuestion.decode( _msgBuffer );
            if( questionOutcome.notOk() )
                return outcome.notOk( questionOutcome.msg() );
            builder.addQuestion( questionOutcome.info() );
        }

        for( int i = 0; i < anc; i++ ) {
            Outcome<? extends DNSResourceRecord> answerOutcome = DNSResourceRecord.decode( _msgBuffer );
            if( answerOutcome.notOk() )
                return outcome.notOk( answerOutcome.msg() );
            builder.addAnswer( answerOutcome.info() );
        }

        for( int i = 0; i < auc; i++ ) {
            Outcome<? extends DNSResourceRecord> authorityOutcome = DNSResourceRecord.decode( _msgBuffer );
            if( authorityOutcome.notOk() )
                return outcome.notOk( authorityOutcome.msg() );
            builder.addAuthority( authorityOutcome.info() );
        }

        for( int i = 0; i < adc; i++ ) {
            Outcome<? extends DNSResourceRecord> additionalOutcome = DNSResourceRecord.decode( _msgBuffer );
            if( additionalOutcome.notOk() )
                return outcome.notOk( additionalOutcome.msg() );
            builder.addAdditionalRecord( additionalOutcome.info() );
        }

        return outcome.ok( builder.getMessage() );
    }


    public static void main( final String[] _args ) throws IOException {

        Builder b = new Builder();
        b.setId( 666 );
        b.setOpCode( DNSOpCode.QUERY );
        b.setRecurse( true );

        DNSQuestion q = DNSQuestion.create( DNSDomainName.fromString( "www.jamulblog.com" ).info(), DNSRRType.CNAME ).info();
        //DNSQuestion q1 = DNSQuestion.create( DNSDomainName.fromString( "www.paradiseweather.info" ).info(), DNSRRType.CNAME ).info();
        b.addQuestion( q );
        //b.addQuestion( q1 );
        DNSMessage m = b.getMessage();

        Outcome<ByteBuffer> eo = m.encode();
        ByteBuffer bb = eo.info();

        InetAddress server = InetAddress.getByName( "1.1.1.1" );
        byte[] packetBytes = new byte[bb.limit()];
        bb.get( packetBytes );
        DatagramPacket packet = new DatagramPacket( packetBytes, packetBytes.length, server, 53 );
        DatagramSocket socket = new DatagramSocket();
        socket.send( packet );

        byte[] buff = new byte[512];
        packet = new DatagramPacket( buff, buff.length );
        socket.receive( packet );
        int len = packet.getLength();
        ByteBuffer rb = ByteBuffer.wrap( buff, 0, len );

        Outcome<DNSMessage> decodeOutcome = DNSMessage.decode( rb );
        DNSMessage received = decodeOutcome.info();

        b.hashCode();
    }


    public static class Builder {

        private int                     id;                     // 16 bit integer ID supplied by application...
        private boolean                 isResponse;             // true for response, false for query...
        private DNSOpCode               opCode;                 // the op code for the message (set in query, copied to response)...
        private boolean                 authoritativeAnswer;    // valid in response only...
        private boolean                 truncated;              // message was truncated by the transmission channel...
        private boolean                 recurse;                // true if recursion is desired (set in query, copied to response)...
        private boolean                 canRecurse;             // valid in response only, true if name server can recursively answer queries...
        private boolean                 z;                      // reserved for future use, must be false (0) in all messages...
        private boolean                 authenticated;          // valid in response only; data in answers and authorities has been authenticated...
        private boolean                 checkingDisabled;       // valid in query only; true if resolver will accept non-authenticated answers and authorities...
        private DNSResponseCode         responseCode;           // valid in responses only; ok or type of error...
        private List<DNSQuestion>       questions;              // the questions in this message...
        private List<DNSResourceRecord> answers;                // the answers in this message...
        private List<DNSResourceRecord> authorities;            // the authorities in this message...
        private List<DNSResourceRecord> additionalRecords;      // the additional records in this message...


        public Builder() {

            opCode            = DNSOpCode.QUERY;
            responseCode      = DNSResponseCode.OK;
            questions         = new ArrayList<>();
            answers           = new ArrayList<>();
            authorities       = new ArrayList<>();
            additionalRecords = new ArrayList<>();
        }


        public DNSMessage getMessage() {
            return new DNSMessage(
                    id, isResponse, opCode, authoritativeAnswer, truncated, recurse, canRecurse, z, authenticated,
                    checkingDisabled, responseCode, questions, answers, authorities, additionalRecords );
        }


        public void addQuestion( final DNSQuestion _question ) {
            questions.add( _question );
        }


        public void addAnswer( final DNSResourceRecord _resourceRecord ) {
            answers.add( _resourceRecord );
        }


        public void addAuthority( final DNSResourceRecord _resourceRecord ) {
            authorities.add( _resourceRecord );
        }


        public void addAdditionalRecord( final DNSResourceRecord _resourceRecord ) {
            additionalRecords.add( _resourceRecord );
        }


        public void setId( final int _id ) {
            id = _id;
        }


        public void setResponse( final boolean _response ) {
            isResponse = _response;
        }


        public void setOpCode( final DNSOpCode _opCode ) {
            opCode = _opCode;
        }


        public void setAuthoritativeAnswer( final boolean _authoritativeAnswer ) {
            authoritativeAnswer = _authoritativeAnswer;
        }


        public void setTruncated( final boolean _truncated ) {
            truncated = _truncated;
        }


        public void setRecurse( final boolean _recurse ) {
            recurse = _recurse;
        }


        public void setCanRecurse( final boolean _canRecurse ) {
            canRecurse = _canRecurse;
        }


        public void setZ( final boolean _z ) {
            z = _z;
        }


        public void setAuthenticated( final boolean _authenticated ) {
            authenticated = _authenticated;
        }


        public void setCheckingDisabled( final boolean _checkingDisabled ) {
            checkingDisabled = _checkingDisabled;
        }


        public void setResponseCode( final DNSResponseCode _responseCode ) {
            responseCode = _responseCode;
        }


        public void setQuestions( final List<DNSQuestion> _questions ) {
            questions = _questions;
        }


        public void setAnswers( final List<DNSResourceRecord> _answers ) {
            answers = _answers;
        }


        public void setAuthorities( final List<DNSResourceRecord> _authorities ) {
            authorities = _authorities;
        }


        public void setAdditionalRecords( final List<DNSResourceRecord> _additionalRecords ) {
            additionalRecords = _additionalRecords;
        }
    }
}
