package com.dilatush.util.dns.agent;

import com.dilatush.util.Checks;
import com.dilatush.util.ExecutorService;
import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolver.AgentParams;
import com.dilatush.util.dns.cache.DNSCache;
import com.dilatush.util.dns.message.*;
import com.dilatush.util.dns.rr.A;
import com.dilatush.util.dns.rr.AAAA;
import com.dilatush.util.dns.rr.DNSResourceRecord;
import com.dilatush.util.dns.rr.NS;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.dilatush.util.dns.agent.DNSTransport.TCP;
import static com.dilatush.util.dns.agent.DNSTransport.UDP;
import static java.util.logging.Level.*;

/**
 * Instances of this class contain the elements and state of a DNS query, and provide methods that implement the resolution of that query.
 */
public class DNSIterativeQuery extends DNSQuery {

    private static final Logger LOGGER                           = General.getLogger();
    private static final long   ITERATIVE_NAME_SERVER_TIMEOUT_MS = 5000;
    private static final int    DNS_SERVER_PORT                  = 53;

    private static final Outcome.Forge<QueryResult> queryOutcome = new Outcome.Forge<>();


    private final List<InetAddress> nextIPs;
    private final AtomicInteger     subQueries;

    private       DNSTransport      initialTransport;



    public DNSIterativeQuery( final DNSResolver _resolver, final DNSCache _cache, final DNSNIO _nio, final ExecutorService _executor,
                              final Map<Short,DNSQuery> _activeQueries, final DNSQuestion _question, final int _id,
                              final Consumer<Outcome<QueryResult>> _handler ) {
        super( _resolver, _cache, _nio, _executor, _activeQueries, _question, _id, new ArrayList<>(), _handler );

        nextIPs    = new ArrayList<>();
        subQueries = new AtomicInteger();

        logQuery("New iterative query " + question );
    }


    public Outcome<QueryResult> initiate( final DNSTransport _initialTransport ) {

        Checks.required( _initialTransport, "initialTransport");

        logQuery("Initial query" );
        LOGGER.finer( "Initiating new iterative query - ID: " + id + ", " + question.toString() );

        initialTransport = _initialTransport;

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

        transport = initialTransport;

        // figure out what agent we're going to use...
        agent = new DNSServerAgent( resolver, this, nio, executor, agents.remove( 0 ) );

        LOGGER.finer( "Iterative query - ID: " + id + ", " + question.toString() + ", using " + agent.name );

        DNSMessage.Builder builder = new DNSMessage.Builder();
        builder.setOpCode( DNSOpCode.QUERY );
        builder.setRecurse( false );
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
        LOGGER.finer( "Entered handleResponse (" + _transport + "): " + _responseMsg.toString() );

        // no matter what happens next, we need to shut down the agent...
        agent.close();

        if( _transport != transport ) {
            String msg = "Received message on " + _transport + ", expected it on " + transport;
            LOGGER.log( Level.WARNING, msg );
            logQuery( msg );
            handler.accept( queryOutcome.notOk( msg ) );
            activeQueries.remove( (short) id );
            return;
        }

        responseMessage = _responseMsg;

        // if our UDP response was truncated, retry it with TCP...
        if( (transport == UDP) && _responseMsg.truncated ) {
            logQuery("UDP response was truncated; retrying with TCP" );
            LOGGER.finest( "UDP response was truncated, retrying with TCP" );
            transport = TCP;
            Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );
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
                handleOK();
                return;
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


    private void handleOK() {

        LOGGER.finest( "handleOK() - ID: " + id );

        logQuery("Response was ok: "
                + responseMessage.answers.size() + " answers, "
                + responseMessage.authorities.size() + " authorities, "
                + responseMessage.additionalRecords.size() + " additional records" );

        // add our results to the cache...
        cache.add( responseMessage.answers );
        cache.add( responseMessage.authorities );
        cache.add( responseMessage.additionalRecords );

        // if we have some answers, then we're done...
        if( !responseMessage.answers.isEmpty() ) {
            LOGGER.finest( "Got answers: " + responseMessage.answers.size() );
            handler.accept( queryOutcome.ok( new QueryResult( queryMessage, responseMessage, queryLog )) );
            activeQueries.remove( (short) id );
            return;
        }

        // If we get here, then what we SHOULD have is one or more NS records in the authorities, which is the DNS server telling us that those name servers can take our
        // query further than it could.  We MIGHT also have one or more A or AAAA records in additional records, which are the IP addresses of the name servers in the
        // authorities section.
        //
        // So what we're going to do now is to see if we have NS records with no resolved IP address (from an A or AAAA record).  If we DO have such NS records, then we're
        // going to make sub-queries to resolve them.  Once we've got the responses to those queries, we'll make a list of name servers with IPs, and use them to take the
        // next step on our query's journey.

        // get a set of all the name servers that we just found out about...
        Set<String> allNameServers = new HashSet<>();
        responseMessage.authorities.forEach( (rr) -> {
            if( rr instanceof NS )
                allNameServers.add( ((NS)rr).nameServer.text );
        });

        // build a list of everything we know about the name servers we got in authorities...
        List<DNSResourceRecord> nsInfo = new ArrayList<>( responseMessage.additionalRecords );
        allNameServers.forEach( (ns) -> nsInfo.addAll( cache.get( ns ) ) );

        // now check any IPs we got from the cache or in additional records, building a list of IPs for name servers, and a set of resolved name servers...
        Set<String> resolvedNameServers = new HashSet<>();
        nextIPs.clear();
        nsInfo.forEach( (rr) -> {
            if( (rr instanceof A) || (rr instanceof AAAA) ) {
                if( allNameServers.contains( rr.name.text ) ) {
                    if( resolver.useIPv4() && (rr instanceof A))
                        nextIPs.add( ((A)rr).address );
                    if( resolver.useIPv6() && (rr instanceof AAAA))
                        nextIPs.add( ((AAAA)rr).address );
                    resolvedNameServers.add( rr.name.text );
                }
            }
        } );

        // build a set of the unresolved name servers...
        Set<String> unresolvedNameServers = new HashSet<>( allNameServers );
        unresolvedNameServers.removeAll( resolvedNameServers );

        LOGGER.finest( "Name servers (all, resolved, unresolved): " + allNameServers.size() + ", " + resolvedNameServers.size() + ", " + unresolvedNameServers.size() );

        // if we don't have any unresolved name servers, then we can just start the next query going...
        if( unresolvedNameServers.isEmpty() ) {
            startNextQuery();
            return;
        }

//        // if we have at least one resolved name servers, then we can just start the next query going...
//        if( !resolvedNameServers.isEmpty() ) {
//            startNextQuery();
//            return;
//        }

        // we DO have unresolved name servers, so blast out sub-queries to resolve them

        // send out the sub-queries...
        unresolvedNameServers.forEach( (unresolvedNameServer) -> {

            // get a DNSDomainName instance from the unresolved name server string; we know the outcome will be ok, so we just grab the info...
            DNSDomainName nsDomainName = DNSDomainName.fromString( unresolvedNameServer ).info();

            // fire off the query for the A record...
            if( resolver.useIPv4() ) {
                subQueries.incrementAndGet();
                LOGGER.finest( "Firing " + nsDomainName.text + " A record sub-query " + subQueries.get() + " from query " + id );
                DNSQuestion aQuestion = new DNSQuestion( nsDomainName, DNSRRType.A );
                DNSIterativeQuery iterativeQuery = new DNSIterativeQuery( resolver, cache, nio, executor, activeQueries, aQuestion, resolver.getNextID(), this::handleSubQuery );
                iterativeQuery.initiate( UDP );
            }

            // fire off the query for the AAAA record...
            if( resolver.useIPv6() ) {
                subQueries.incrementAndGet();
                LOGGER.finest( "Firing " + nsDomainName.text + " AAAA record sub-query " + subQueries.get() + " from query " + id );
                DNSQuestion aQuestion = new DNSQuestion( nsDomainName, DNSRRType.AAAA );
                DNSIterativeQuery iterativeQuery = new DNSIterativeQuery( resolver, cache, nio, executor, activeQueries, aQuestion, resolver.getNextID(), this::handleSubQuery );
                iterativeQuery.initiate( UDP );
            }
        } );
    }


    private void startNextQuery() {

        // if we have no IPs to query, we've got a problem...
        if( nextIPs.isEmpty() ) {
            handler.accept( queryOutcome.notOk( "Iterative query; no name server available for: " + question.qname.text ) );
            activeQueries.remove( (short) id );
            return;
        }

        // turn our IP addresses into agent parameters...
        agents.clear();
        nextIPs.forEach( (ip) -> agents.add( new AgentParams( ITERATIVE_NAME_SERVER_TIMEOUT_MS, 0, ip.getHostAddress(), new InetSocketAddress( ip, DNS_SERVER_PORT ) ) ) );

        // figure out what agent we're going to use...
        agent = new DNSServerAgent( resolver, this, nio, executor, agents.remove( 0 ) );

        LOGGER.finer( "Subsequent iterative query - ID: " + id + ", " + question.toString() + ", using " + agent.name );

        // send the next level query...
        transport = initialTransport;
        Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );
        if( sendOutcome.notOk() ) {
            handler.accept( queryOutcome.notOk( "Could not send query: " + sendOutcome.msg(), sendOutcome.cause() ) );
            activeQueries.remove( (short) id );
        }
    }


    /**
     * Handle the outcome of a sub-query.  Note that if the executor is configured with multiple threads, then it's possible for multiple threads to execute this method
     * concurrently; hence the synchronization.
     *
     * @param _outcome The {@link Outcome Outcome&lt;QueryResult&gt;} of the sub-query.
     */
    private void handleSubQuery( Outcome<QueryResult> _outcome ) {

        LOGGER.log( FINER, "Entered handleSubQuery, " + (_outcome.ok() ? "ok" : "not ok") );
        LOGGER.log( FINEST, "Query " + _outcome.info().query().toString() + "\nResponse: " + _outcome.info().response().toString() );

        synchronized( this ) {

            // if we got a good result, then add any IPs we got to the next IPs list...
            if( _outcome.ok() ) {
                DNSMessage response = _outcome.info().response();
                addIPs( nextIPs, response.answers );
            }

            // whatever happened, log the sub-query...
            logQuery( "Sub-query" );
            _outcome.info().log().forEach( ( lr ) -> logQuery( "    " + lr.toString() ) );
        }

        // decrement our counter, and if we're done, try sending the next query...
        int queryCount = subQueries.decrementAndGet();
        LOGGER.fine( "Sub-query count: " + queryCount );
        if( queryCount == 0 )
            startNextQuery();
    }


    protected void handleResponseProblem( final String _msg, final Throwable _cause ) {
        logQuery("Problem with response: " + _msg + ((_cause != null) ? " - " + _cause.getMessage() : "") );
        while( !agents.isEmpty() ) {
            Outcome<QueryResult> qo = query();
            if( qo.ok() )
                return;
        }
        logQuery("No more DNS servers to try" );
        handler.accept( queryOutcome.notOk( _msg, _cause, new QueryResult( queryMessage, null, queryLog ) ) );
        activeQueries.remove( (short) id );
    }


    public String toString() {
        return "DNSQuery: " + responseMessage.answers.size() + " answers";
    }
}
