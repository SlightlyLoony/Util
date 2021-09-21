package com.dilatush.util.dns.agent;

import com.dilatush.util.Checks;
import com.dilatush.util.ExecutorService;
import com.dilatush.util.General;
import com.dilatush.util.Outcome;
import com.dilatush.util.dns.DNSResolver;
import com.dilatush.util.dns.DNSResolver.AgentParams;
import com.dilatush.util.dns.cache.DNSCache;
import com.dilatush.util.dns.message.*;
import com.dilatush.util.dns.rr.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.dilatush.util.dns.agent.DNSTransport.UDP;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;

/**
 * Instances of this class contain the elements and state of a DNS query, and provide methods that implement the resolution of that query.
 */
public class DNSIterativeQuery extends DNSQuery {

    private static final Logger LOGGER                           = General.getLogger();
    private static final long   ITERATIVE_NAME_SERVER_TIMEOUT_MS = 5000;
    private static final int    DNS_SERVER_PORT                  = 53;

    private static final Outcome.Forge<QueryResult> queryOutcome = new Outcome.Forge<>();


    private final List<InetAddress>       nextIPs;
    private final AtomicInteger           subQueries;
    private final List<DNSResourceRecord> answers;

    private       DNSTransport      initialTransport;



    public DNSIterativeQuery( final DNSResolver _resolver, final DNSCache _cache, final DNSNIO _nio, final ExecutorService _executor,
                              final Map<Short,DNSQuery> _activeQueries, final DNSQuestion _question, final int _id,
                              final Consumer<Outcome<QueryResult>> _handler ) {
        super( _resolver, _cache, _nio, _executor, _activeQueries, _question, _id, new ArrayList<>(), _handler );

        nextIPs    = new ArrayList<>();
        subQueries = new AtomicInteger();
        answers    = new ArrayList<>();

        queryLog.log("New iterative query " + question );
    }


    public Outcome<?> initiate( final DNSTransport _initialTransport ) {

        Checks.required( _initialTransport, "initialTransport");

        queryLog.log("Initial query" );
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
            if( nsIPs.size() > 0 ) {
                queryLog.log( "Resolved '" + searchDomain.text + "' from cache" );
                break;
            }

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
            if( rho.notOk() ) {
                queryLog.log( "Could not read root hints" );
                return queryOutcome.notOk( rho.msg(), rho.cause(), new QueryResult( queryMessage, null, queryLog ) );
            }

            queryLog.log( "No cache hits; starting from root" );

            // add all the root hint resource records to the cache...
            cache.add( rho.info() );

            // add the root name server IP addresses to our list...
            addIPs( nsIPs, rho.info() );

            // if we STILL have no IPs for name servers, we're dead (this really should never happen until the heat death of the universe)...
            if( nsIPs.isEmpty() ) {
                queryLog.log( "Could not find any root name server IP addresses" );
                return queryOutcome.notOk( "Could not find any root name server IP addresses", null, new QueryResult( queryMessage, null, queryLog ) );
            }
        }

        // turn our IP addresses into agent parameters...
        nsIPs.forEach( (ip) -> agents.add( new AgentParams( ITERATIVE_NAME_SERVER_TIMEOUT_MS, 0, ip.getHostAddress(), new InetSocketAddress( ip, DNS_SERVER_PORT ) ) ) );

        return query();
    }


    protected Outcome<?> query() {

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

        queryLog.log("Sending iterative query for " + question + " to " + agent.name + " via " + transport );

        Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );

        if( sendOutcome.notOk() )
            return queryOutcome.notOk( sendOutcome.msg(), sendOutcome.cause() );

        return queryOutcome.ok( new QueryResult( queryMessage, null, queryLog ) );
    }


    protected void handleOK() {

        basicOK();

        // if we have some answers, then let's see if we're done, or if we're resolving a CNAME chain...
        if( !responseMessage.answers.isEmpty() ) {

            LOGGER.finest( "Got some answers: " + responseMessage.answers.size() );

            // There are several possible scenarios here, which must be checked in the order given:
            // 1. There are one or more answers, and they're all the desired type (or the desired type is ANY).  In this case, we accumulate all the answers and we're done.
            // 2. There are two or more answers, consisting of one or more CNAME records followed by one or more answers of the desired type.  In this case, we accumulate
            //    all the answers, and we're done.
            // 3. There are one or more answers, all of which are CNAME records.  In this case, the last CNAME is a referral, we accumulate all the answers, check for a CNAME
            //    loop (which is an error, and then fire off a sub-query to resolve the referral.  The results of the sub-query are evaluated exactly as the results of the
            //    first query.
            // 4. There are one or more answers which are neither CNAME records nor the desired type.  This is an error.

            // If the desired type is ANY, then this response is our answer.
            if( question.qtype == DNSRRType.ANY ) {
                handler.accept( queryOutcome.ok( new QueryResult( queryMessage, responseMessage, queryLog )) );
                activeQueries.remove( (short) id );
                return;
            }

            // do a little analysis, so we can figure out what to do...
            final int[] cnameCount   = {0};
            final int[] desiredCount = {0};
            final int[] wrongCount   = {0};
            final boolean[] bogus = {false};
            responseMessage.answers.forEach( (rr) -> {
                if( rr instanceof CNAME ) {
                    cnameCount[ 0 ]++;
                    if( desiredCount[0] > 0 ) {
                        bogus[0] = true;
                    }
                }
                else if( rr.type == question.qtype ) {
                    desiredCount[0]++;
                }
                else {
                    wrongCount[0]++;
                    bogus[0] = true;
                }
            } );


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
        String[] nsDomain = new String[1];
        responseMessage.authorities.forEach( (rr) -> {
            if( rr instanceof NS ) {
                allNameServers.add( ((NS)rr).nameServer.text );
                nsDomain[0] = rr.name.text;
            }
        });

        // if we have no name server records, then we've got a real problem...
        if( allNameServers.isEmpty() ) {
            queryLog.log( "No name server records received" );
            handler.accept(
                    queryOutcome.notOk(
                            "No name server records received from " + agent.name,
                            null,
                            new QueryResult( queryMessage, responseMessage, queryLog ) )
            );
        }

        queryLog.log( "Got " + allNameServers.size() + " name server(s) for '" + nsDomain[0] + "'" );

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
                DNSIterativeQuery iterativeQuery = new DNSIterativeQuery( resolver, cache, nio, executor, activeQueries, aQuestion, resolver.getNextID(), this::handleNSResolutionSubQuery );
                iterativeQuery.initiate( UDP );
            }

            // fire off the query for the AAAA record...
            if( resolver.useIPv6() ) {
                subQueries.incrementAndGet();
                LOGGER.finest( "Firing " + nsDomainName.text + " AAAA record sub-query " + subQueries.get() + " from query " + id );
                DNSQuestion aQuestion = new DNSQuestion( nsDomainName, DNSRRType.AAAA );
                DNSIterativeQuery iterativeQuery = new DNSIterativeQuery( resolver, cache, nio, executor, activeQueries, aQuestion, resolver.getNextID(), this::handleNSResolutionSubQuery );
                iterativeQuery.initiate( UDP );
            }
        } );
    }


    private void startNextQuery() {

        // if we have no IPs to query, we've got a problem...
        if( nextIPs.isEmpty() ) {
            handler.accept( queryOutcome.notOk( "Iterative query; no name server available for: " + question.qname.text, null,
                    new QueryResult( queryMessage, null, queryLog )) );
            activeQueries.remove( (short) id );
            return;
        }

        // turn our IP addresses into agent parameters...
        agents.clear();
        nextIPs.forEach( (ip) -> agents.add( new AgentParams( ITERATIVE_NAME_SERVER_TIMEOUT_MS, 0, ip.getHostAddress(), new InetSocketAddress( ip, DNS_SERVER_PORT ) ) ) );

        // figure out what agent we're going to use...
        agent = new DNSServerAgent( resolver, this, nio, executor, agents.remove( 0 ) );

        String logMsg = "Subsequent iterative query: " + question.toString() + ", using " + agent.name;
        LOGGER.finer( logMsg );
        queryLog.log( logMsg );

        // send the next level query...
        transport = initialTransport;
        Outcome<?> sendOutcome = agent.sendQuery( queryMessage, transport );
        if( sendOutcome.notOk() ) {
            handler.accept( queryOutcome.notOk( "Could not send query: " + sendOutcome.msg(), sendOutcome.cause(),
                    new QueryResult( queryMessage, null, queryLog )) );
            activeQueries.remove( (short) id );
        }
    }


    /**
     * Handle the outcome of a sub-query.  Note that if the executor is configured with multiple threads, then it's possible for multiple threads to execute this method
     * concurrently; hence the synchronization.
     *
     * @param _outcome The {@link Outcome Outcome&lt;QueryResult&gt;} of the sub-query.
     */
    private void handleNSResolutionSubQuery( Outcome<QueryResult> _outcome ) {

        LOGGER.log( FINER, "Entered handleSubQuery, " + (_outcome.ok() ? "ok" : "not ok") );
        String logMsg = "Query " + _outcome.info().query().toString()
                + ((_outcome.info().response() != null) ? "\nResponse: " + _outcome.info().response() : "");
        LOGGER.log( FINEST, logMsg );

        synchronized( this ) {

            // if we got a good result, then add any IPs we got to the next IPs list...
            DNSMessage response = _outcome.info().response();
            if( _outcome.ok() && (response != null) )
                addIPs( nextIPs, response.answers );

            // whatever happened, log the sub-query...
            queryLog.log( "Sub-query" );
            queryLog.addSubQueryLog( _outcome.info().log() );
        }

        // decrement our counter, and if we're done, try sending the next query...
        int queryCount = subQueries.decrementAndGet();
        LOGGER.fine( "Sub-query count: " + queryCount );
        if( queryCount == 0 )
            startNextQuery();
    }


    public String toString() {
        return "DNSQuery: " + responseMessage.answers.size() + " answers";
    }
}
