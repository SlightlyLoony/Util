package com.dilatush.util.dns.agent;

import com.dilatush.util.Outcome;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSOpCode;
import com.dilatush.util.dns.message.DNSQuestion;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.dilatush.util.General.isNull;

/**
 * Instances of this class contain the elements and state of a DNS query, and provide methods that implement the resolution of that query.
 */
public class DNSQuery {

    private static final Outcome.Forge<DNSQuery> queryOutcome = new Outcome.Forge<>();


    private final DNSServerAgent               agent;

    private final DNSResolution                resolution;

    private final DNSQuestion                  question;

    private final DNSQueryTimeout              timeout;

    private final Consumer<Outcome<DNSQuery>>  handler;

    private       DNSTransport                 transport;

    private       DNSMessage                   queryMessage;

    private       ByteBuffer                   encodedQueryMessage;

    private       DNSMessage                   responseMessage;

    private       long                         startTime;

    private       long                         endTime;


    private DNSQuery( final DNSServerAgent _agent, final DNSResolution _resolution, final DNSQuestion _question, final long _timeoutMillis, final DNSTransport _transport,
                      final Consumer<Outcome<DNSQuery>> _handler, final DNSMessage _queryMessage ) {

        agent           = _agent;
        resolution      = _resolution;
        question        = _question;
        timeout         = new DNSQueryTimeout( _timeoutMillis, this::onTimeout );
        transport       = _transport;
        handler         = _handler;
        queryMessage    = _queryMessage;
        responseMessage = null;
        startTime       = System.currentTimeMillis();
        endTime         = startTime;
    }


    protected static Outcome<DNSQuery> initiate( final DNSServerAgent _agent, final DNSResolution _resolution, final DNSQuestion _question, final long _timeoutMillis,
                                                 final DNSTransport _transport, final Consumer<Outcome<DNSQuery>> _handler, final int _id ) {

        if( isNull( _agent, _resolution, _question, _transport, _handler ) )
            return queryOutcome.notOk( "Missing required parameter" );

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( _resolution == DNSResolution.RECURSIVE );
        builder.setId( _id );
        builder.addQuestion( _question );

        DNSMessage queryMsg = builder.getMessage();

        DNSQuery query = new DNSQuery( _agent, _resolution, _question, _timeoutMillis, _transport, _handler, queryMsg );

        _agent.setQueryMapping( query );

        Outcome<ByteBuffer> encodeOutcome = queryMsg.encode();
        if( encodeOutcome.notOk() )
            return queryOutcome.notOk( encodeOutcome.msg(), encodeOutcome.cause() );

        query.setEncodedQueryMessage( encodeOutcome.info() );

        Outcome<?> sendOutcome = query.sendQuery();

        if( sendOutcome.notOk() )
            return queryOutcome.notOk( sendOutcome.msg(), sendOutcome.cause() );

        _agent.addTimeout( query.timeout );

        return queryOutcome.ok( query );
    }


    protected void setResponse( final DNSMessage _response ) {
        responseMessage = _response;
        endTime = System.currentTimeMillis();
    }


    protected Outcome<?> sendQuery() {
        return switch( transport ) {
            case UDP -> agent.udpChannel.send( encodedQueryMessage );
            case TCP -> agent.tcpChannel.send( encodedQueryMessage );
        };
    }


    protected boolean cancelTimeout() {
        return timeout.cancel();
    }


    protected int getID() {
        return queryMessage.id;
    }

    public void setTransport( final DNSTransport _transport ) {
        transport = _transport;
    }

    public void setEncodedQueryMessage( final ByteBuffer _encodedQueryMessage ) {
        encodedQueryMessage = _encodedQueryMessage;
    }

    public DNSTransport getTransport() {
        return transport;
    }


    protected void onCompletion() {
        agent.removeQueryMapping( queryMessage.id );
        handler.accept( queryOutcome.ok( this ) );
    }


    protected void onProblem( final String _msg, final Throwable _cause ) {
        agent.removeQueryMapping( getID() );
        handler.accept( queryOutcome.notOk( _msg, _cause, this ) );
    }


    private void onTimeout() {
        agent.removeQueryMapping( queryMessage.id );
        handler.accept( queryOutcome.notOk( "Timeout", new TimeoutException(), this ) );
    }


    public String toString() {
        return "DNSQuery: " + responseMessage.answers.size() + " answers in " + (endTime - startTime) + " milliseconds";
    }
}
