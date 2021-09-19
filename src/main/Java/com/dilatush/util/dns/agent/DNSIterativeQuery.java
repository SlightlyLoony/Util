package com.dilatush.util.dns.agent;

import com.dilatush.util.Checks;
import com.dilatush.util.ExecutorService;
import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolver.AgentParams;
import com.dilatush.util.dns.cache.DNSCache;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.message.DNSMessage;
import com.dilatush.util.dns.message.DNSOpCode;
import com.dilatush.util.dns.message.DNSQuestion;
import com.dilatush.util.dns.rr.DNSResourceRecord;
import com.dilatush.util.dns.rr.NS;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.dilatush.util.dns.agent.DNSTransport.TCP;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;

/**
 * Instances of this class contain the elements and state of a DNS query, and provide methods that implement the resolution of that query.
 */
public class DNSIterativeQuery extends DNSQuery {

    private static final Logger LOGGER                           = General.getLogger();
    private static final long   ITERATIVE_NAME_SERVER_TIMEOUT_MS = 5000;
    private static final int    DNS_SERVER_PORT                  = 53;

    private static final Outcome.Forge<QueryResult> queryOutcome = new Outcome.Forge<>();



    public DNSIterativeQuery( final DNSResolver _resolver, final DNSCache _cache, final DNSNIO _nio, final ExecutorService _executor,
                              final Map<Short,DNSQuery> _activeQueries, final DNSQuestion _question, final int _id,
                              final Consumer<Outcome<QueryResult>> _handler ) {
        super( _resolver, _cache, _nio, _executor, _activeQueries, _question, _id, new ArrayList<>(), _handler );

        logQuery("New iterative query " + question );
    }


    public Outcome<QueryResult> initiate( final DNSTransport _initialTransport ) {

        Checks.required( _initialTransport, "initialTransport");

        logQuery("Initial query" );

        transport = _initialTransport;

        // we need to figure out the starting nameservers, and make agents for them...
        // we know the actual question wasn't cached, as we wouldn't have queried at all if it was - so we start looking with its parent domain,
        // unless we're already at the root domain...
        DNSDomainName searchDomain = question.qname.isRoot() ? question.qname : question.qname.parent();

        List<InetAddress> nsIPs = new ArrayList<>();

        // check our search domain, and its parents if necessary, until we have some name servers to go ask questions of...
        while( nsIPs.size() == 0 ) {

            // check the cache for name server (NS) records for the domain we're checking...
            List<DNSResourceRecord> ns = cache.get( searchDomain )
                    .stream()
                    .filter( (rr) -> rr instanceof NS )
                    .collect( Collectors.toList());

            // let's see if we have an IP address for any name servers we got...
            ns.forEach( (rr) -> addIPs( nsIPs, cache.get( ((NS)rr).nameServer ) ) );

            // if we have at least one IP address, then we're done...
            if( nsIPs.size() > 0 )
                break;

            // no IPs yet, but if our search domain isn't the root, we can check its parent...
            if( !searchDomain.isRoot() ) {
                searchDomain = searchDomain.parent();
                continue;
            }

            // no IPs yet, and we're searching the root - this means one of:
            // -- we're not caching anything
            // -- the root name servers expired and were purged
            // either way, we need to read the root hints to get the root name servers...
            Outcome<List<DNSResourceRecord>> rho = cache.getRootHints();

            // if we couldn't read the root hints, we're in trouble...
            if( rho.notOk() )
                return queryOutcome.notOk( rho.msg(), rho.cause() );

            // add all the root hint resource records to the cache...
            cache.add( rho.info() );

            // add the root name server IP addresses to our list...
            addIPs( nsIPs, rho.info() );

            // if we STILL have no IPs for name servers, we're dead (this really should never happen until the heat death of the universe)...
            if( nsIPs.isEmpty() )
                return queryOutcome.notOk( "Could not find any root name server IP addresses" );
        }

        // turn our IP addresses into agent parameters...
        nsIPs.forEach( (ip) -> agents.add( new AgentParams( ITERATIVE_NAME_SERVER_TIMEOUT_MS, 0, ip.getHostAddress(), new InetSocketAddress( ip, DNS_SERVER_PORT ) ) ) );

        return query();
    }


    protected Outcome<QueryResult> query() {

        // figure out what agent we're going to use...
        agent = new DNSServerAgent( resolver, this, nio, executor, agents.remove( 0 ) );

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( true );
        builder.setId( id & 0xFFFF );
        builder.addQuestion( question );

        queryMessage = builder.getMessage();

        logQuery("Sending iterative query to " + agent.name + " via " + transport );

        Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );

        if( sendOutcome.notOk() )
            return queryOutcome.notOk( sendOutcome.msg(), sendOutcome.cause() );

        return queryOutcome.ok( new QueryResult( queryMessage, null, queryLog ) );
    }


    protected void handleResponse( final DNSMessage _responseMsg, final DNSTransport _transport ) {

        logQuery("Received response via " + _transport );

        // no matter what happens next, we need to shut down the agent...
        agent.close();

        if( _transport != transport ) {
            String msg = "Received message on " + _transport + ", expected it on " + transport;
            LOGGER.log( Level.WARNING, msg );
            logQuery( msg );
            agent.close();
            handler.accept( queryOutcome.notOk( msg ) );
            activeQueries.remove( (short) id );
            return;
        }

        responseMessage = _responseMsg;

        // if our UDP response was truncated, retry it with TCP...
        if( (transport == UDP) && _responseMsg.truncated ) {
            logQuery("UDP response was truncated; retrying with TCP" );
            transport = TCP;
            Outcome<?> sendOutcome = agent.sendQuery( queryMessage, TCP );
            if( sendOutcome.notOk() ) {
                handler.accept( queryOutcome.notOk( "Could not send query via TCP: " + sendOutcome.msg(), sendOutcome.cause() ) );
                activeQueries.remove( (short) id );
            }
            return;
        }

        // handle appropriately according to the response code...
        switch( responseMessage.responseCode ) {

            // the question was answered; the response is valid...
            case OK -> {
                logQuery("Response was ok: "
                        + responseMessage.answers.size() + " answers, "
                        + responseMessage.authorities.size() + " authorities, "
                        + responseMessage.additionalRecords.size() + " additional records" );

                // add our results to the cache...
                cache.add( responseMessage.answers );
                cache.add( responseMessage.authorities );
                cache.add( responseMessage.additionalRecords );

                // if we don't have any answers, then we have more work to do...
                if( responseMessage.answers.isEmpty() ) {

                    // gather the IPs of name servers as reported in the additional records...
                    List<InetAddress> ips = new ArrayList<>();
                    addIPs( ips, responseMessage.additionalRecords );

                    // if we had no IPs, we've got a problem...
                    if( ips.isEmpty() ) {
                        handler.accept( queryOutcome.notOk( "Iterative query; no name server available for: " + question.qname.text ) );
                        activeQueries.remove( (short) id );
                    }

                    // turn our IP addresses into agent parameters...
                    agents.clear();
                    ips.forEach( (ip) -> agents.add( new AgentParams( ITERATIVE_NAME_SERVER_TIMEOUT_MS, 0, ip.getHostAddress(), new InetSocketAddress( ip, DNS_SERVER_PORT ) ) ) );

                    // figure out what agent we're going to use...
                    agent = new DNSServerAgent( resolver, this, nio, executor, agents.remove( 0 ) );

                    // send
                    Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );
                    if( sendOutcome.notOk() ) {
                        handler.accept( queryOutcome.notOk( "Could not send query: " + sendOutcome.msg(), sendOutcome.cause() ) );
                        activeQueries.remove( (short) id );
                    }

                    return;
                }

                // if we're in a recursive query, or an iterative query with answers, we need to send the results, and then we're done...
                handler.accept( queryOutcome.ok( new QueryResult( queryMessage, responseMessage, queryLog )) );
            }

            case REFUSED -> {
                if( tryOtherServers( "REFUSED" ) )
                    return;
            }

            case NAME_ERROR -> {
                if( tryOtherServers( "NAME ERROR" ) )
                    return;
            }

            // the question could not be interpreted by the server...
            case FORMAT_ERROR -> {
                if( tryOtherServers( "FORMAT ERROR" ) )
                    return;
            }

            case SERVER_FAILURE -> {
                if( tryOtherServers( "SERVER FAILURE" ) )
                    return;
            }

            case NOT_IMPLEMENTED -> {
                if( tryOtherServers( "NOT IMPLEMENTED" ) )
                    return;
            }
        }

        // if we get here, we need to show that this query is inactive...
        activeQueries.remove( (short) id );
    }


    protected void handleResponseProblem( final String _msg, final Throwable _cause ) {
        logQuery("Problem with response: " + _msg + ((_cause != null) ? " - " + _cause.getMessage() : "") );
        while( !agents.isEmpty() ) {
            Outcome<QueryResult> qo = query();
            if( qo.ok() )
                return;
        }
        logQuery("No more DNS servers to try" );
        handler.accept( queryOutcome.notOk( _msg, _cause ) );
        activeQueries.remove( (short) id );
    }


    public String toString() {
        return "DNSQuery: " + responseMessage.answers.size() + " answers";
    }
}
