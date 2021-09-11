package com.dilatush.util.dns.cache;

import com.dilatush.util.Outcome;
import com.dilatush.util.Streams;
import com.dilatush.util.dns.message.DNSDomainName;
import com.dilatush.util.dns.rr.A;
import com.dilatush.util.dns.rr.AAAA;
import com.dilatush.util.dns.rr.DNSResourceRecord;
import com.dilatush.util.dns.rr.NS;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

public class DNSRootHints {

    private static final String ROOT_HINTS_FILE_NAME = "ROOT_HINTS.TXT";

    private static final Pattern DATE_PATTERN = compile( ".*last update: +([A-Z][a-z]+ +[1-9][0-9]?, +20[0-9][0-9]).*", DOTALL    );
    private static final Pattern RR_PATTERN   = compile( "^((?:[A-Z-]*\\.)+) +([1-9][0-9]+) +([A-Z]+) +([^ ]*)$",       MULTILINE );

    private static final Outcome.Forge<?>                   outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<String>              stringOutcome = new Outcome.Forge<>();
    private static final Outcome.Forge<List<DNSCacheEntry>> rrlOutcome    = new Outcome.Forge<>();
    private static final Outcome.Forge<DNSResourceRecord>   rrOutcome     = new Outcome.Forge<>();

    private DNSRootHints(){}  // prevent instantiation...

    public static Outcome<String> readURL( final String _url ) {

        try{
            URL url = new URL( _url );
            InputStream is = url.openStream();
            String contents = Streams.toString( is, StandardCharsets.US_ASCII );
            return stringOutcome.ok( contents );
        }
        catch( IOException _e ) {
            return stringOutcome.notOk( "Problem reading URL: " + _e.getMessage(), _e );
        }

    }


    public static Outcome<String> readFile( final String _dirPath ) {

        try {
            return stringOutcome.ok( Files.readString( Path.of( _dirPath, ROOT_HINTS_FILE_NAME ), StandardCharsets.US_ASCII ) );
        }
        catch( IOException _e ) {
            return stringOutcome.notOk( "Problem reading root hints file: " + _e.getMessage(), _e );
        }
    }


    public static Outcome<?> writeFile( final String _rootHints, final String _dirPath ) {
        try {
            Files.writeString( Path.of( _dirPath, ROOT_HINTS_FILE_NAME ), _rootHints, StandardCharsets.US_ASCII );
            return outcome.ok();
        }
        catch( IOException _e ) {
            return outcome.notOk( "Problem writing root hints file: " + _e.getMessage(), _e );
        }
    }


    public static Outcome<List<DNSCacheEntry>> decode( final String _rootHints ) {

        Matcher mat = DATE_PATTERN.matcher( _rootHints );
        if( mat.matches() ) {

            String dateStr = mat.group( 1 );

            DateTimeFormatter parser = DateTimeFormatter.ofPattern( "MMMM d, yyyy HH:mm:ss zzz" );
            ZonedDateTime updated;
            try {
                updated = ZonedDateTime.parse( dateStr + " 00:00:00 GMT", parser );
            }
            catch( Exception _e ) {
                return rrlOutcome.notOk( "Could not parse updated date: " + dateStr );
            }
            long updatedMillis = updated.toEpochSecond() * 1000;

            List<DNSCacheEntry> entries = new ArrayList<>();

            mat = RR_PATTERN.matcher( _rootHints );
            while( mat.find() ) {

                String dnStr  = mat.group( 1 );
                String ttlStr = mat.group( 2 );
                String rrtStr = mat.group( 3 );
                String rrdStr = mat.group( 4 );

                Outcome<DNSDomainName> dno = DNSDomainName.fromString( dnStr );
                if( dno.notOk() )
                    return rrlOutcome.notOk( dno.msg(), dno.cause() );
                DNSDomainName dn = dno.info();
                int ttl = Integer.parseInt( ttlStr );  // should be impossible to throw NumberFormatException...

                Outcome<DNSResourceRecord> rro = switch( rrtStr ) {

                    case "A" -> {
                        try {
                            InetAddress address = InetAddress.getByName( rrdStr );
                            address = InetAddress.getByAddress( dnStr, address.getAddress() );
                            Outcome<A> iao = A.create( dn, ttl, (Inet4Address) address );
                            if( iao.notOk() )
                                yield rrOutcome.notOk( "Problem creating A resource record: " + iao.msg(), iao.cause() );
                            yield rrOutcome.ok( iao.info() );
                        }
                        catch( Exception _e ) {
                            yield rrOutcome.notOk( "Problem creating A resource record: " + _e.getMessage(), _e );
                        }
                    }

                    case "AAAA" -> {
                        try {
                            InetAddress address = InetAddress.getByName( rrdStr );
                            address = InetAddress.getByAddress( dnStr, address.getAddress() );
                            Outcome<AAAA> iao = AAAA.create( dn, ttl, (Inet6Address) address );
                            if( iao.notOk() )
                                yield rrOutcome.notOk( "Problem creating AAAA resource record: " + iao.msg(), iao.cause() );
                            yield rrOutcome.ok( iao.info() );
                        }
                        catch( Exception _e ) {
                            yield rrOutcome.notOk( "Problem creating AAAA resource record: " + _e.getMessage(), _e );
                        }
                    }

                    case "NS" -> {
                        Outcome<DNSDomainName> nsdno = DNSDomainName.fromString( rrdStr );
                        if( nsdno.notOk() )
                            yield rrOutcome.notOk( nsdno.msg(), nsdno.cause() );
                        Outcome<NS> nso = NS.create( dn, ttl, nsdno.info() );
                        if( nso.notOk() )
                            yield rrOutcome.notOk( nso.msg(), nso.cause() );
                        yield rrOutcome.ok( nso.info() );
                    }

                    default -> {
                        yield rrOutcome.notOk( "Unexpected resource record type: " + rrtStr );
                    }
                };

                if( rro.notOk() )
                    return rrlOutcome.notOk( rro.msg(), rro.cause() );

                DNSResourceRecord rr = rro.info();
                entries.add( new DNSCacheEntry( rr, updatedMillis + rr.ttl * 1000 ) );
            }
            return rrlOutcome.ok( entries );
        }
        return rrlOutcome.notOk( "Could not find last updated date in root hints file" );
    }


    public static void main( final String[] _args ) {

        Outcome<String> ruo = readURL( "http://www.internic.net/domain/named.root" );
        Outcome<?> wo = writeFile( ruo.info(), "." );
        Outcome<String> ro = readFile( "." );
        Outcome<List<DNSCacheEntry>> rro = decode( ro.info() );

        "".hashCode();
    }
}
