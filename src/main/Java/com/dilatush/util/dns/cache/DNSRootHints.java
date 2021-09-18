package com.dilatush.util.dns.cache;

import com.dilatush.util.Checks;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.General.getLogger;
import static com.dilatush.util.Strings.isEmpty;
import static java.util.regex.Pattern.*;

/**
 * Instances of this class manage DNS root name server "hints".  These are publicly downloadable via HTTP.  Methods are provided to read and write a local file (to provide
 * persistence, mainly for startup), to read the original file via HTTP, and to cache the results locally.
 */
@SuppressWarnings( "unused" )
public class DNSRootHints {

    private static final Logger LOGGER = getLogger();

    public  static final String DEFAULT_ROOT_HINTS_FILE_NAME = "ROOT_HINTS.TXT";
    public  static final String DEFAULT_ROOT_HINTS_URL_STRING = "https://www.internic.net/domain/named.root";

    private static final Pattern DATE_PATTERN = compile( ".*last update: +([A-Z][a-z]+ +[1-9][0-9]?, +20[0-9][0-9]).*", DOTALL    );
    private static final Pattern RR_PATTERN   = compile( "^((?:[A-Z-]*\\.)+) +([1-9][0-9]+) +([A-Z]+) +([^ ]*)$",       MULTILINE );

    private static final Outcome.Forge<?>                       outcome       = new Outcome.Forge<>();
    private static final Outcome.Forge<String>                  stringOutcome = new Outcome.Forge<>();
    private static final Outcome.Forge<List<DNSResourceRecord>> rrlOutcome    = new Outcome.Forge<>();
    private static final Outcome.Forge<DNSResourceRecord>       rrOutcome     = new Outcome.Forge<>();

    private final String urlString;
    private final String rootHintsFileName;

    // this always contains the most recently read version, whether from file or URL...
    private String rootHintsString;


    /**
     * Creates a new instance of this class with the given URL and root hints file name.
     *
     * @param _urlString The URL to find the root hints file at.
     * @param _rootHintsFileName The name (and path) of the root hints file.
     */
    public DNSRootHints( final String _urlString, final String _rootHintsFileName ) {

        Checks.required( _urlString, _rootHintsFileName );

        urlString = _urlString;
        rootHintsFileName = _rootHintsFileName;
    }


    /**
     * Creates a new instance of this class with the default URL and root hints file name.
     */
    public DNSRootHints() {
        this( DEFAULT_ROOT_HINTS_URL_STRING, DEFAULT_ROOT_HINTS_FILE_NAME );
    }


    /**
     * Read the root hints file from the URL into a string.
     *
     * @return the {@link Outcome Outcome&lt;String&gt;} result.
     */
    public Outcome<String> readURL() {

        try{
            URL url = new URL( urlString );
            InputStream is = url.openStream();
            rootHintsString = Streams.toString( is, StandardCharsets.US_ASCII );
            LOGGER.finer( "Read root hints from URL: " + urlString );
            return stringOutcome.ok( rootHintsString );
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem reading URL: " + _e.getMessage(), _e );
            return stringOutcome.notOk( "Problem reading URL: " + _e.getMessage(), _e );
        }

    }


    /**
     * Read the root hints file from the local file system.
     *
     * @return the {@link Outcome Outcome&lt;String&gt;} result.
     */
    public Outcome<String> readFile() {

        try {
            Path rhPath = Path.of( rootHintsFileName );

            // if we don't have a usable file, return an error...
            if( !Files.exists( rhPath ) || !Files.isReadable( rhPath ) || (Files.size( rhPath ) < 500 ) )
                return stringOutcome.notOk( "Root hints file does not exist, is not readable, or is too short to be valid" );

            // ok, it's safe to actually read it...
            rootHintsString = Files.readString( rhPath, StandardCharsets.US_ASCII );
            LOGGER.finer( "Read root hints from file: " + rootHintsFileName );
            return stringOutcome.ok( rootHintsString );
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem reading root hints file: " + _e.getMessage(), _e );
            return stringOutcome.notOk( "Problem reading root hints file: " + _e.getMessage(), _e );
        }
    }


    /**
     * Write the given root hints string to the local file system.
     *
     * @param _rootHints The root hints string.
     * @return the {@link Outcome Outcome&lt;?&gt;} result of the write operation.
     */
    public Outcome<?> writeFile( final String _rootHints ) {
        try {
            Files.writeString( Path.of( rootHintsFileName ), _rootHints, StandardCharsets.US_ASCII );
            LOGGER.finer( "Wrote root hints file: " + rootHintsFileName );
            return outcome.ok();
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "Problem writing root hints file: " + _e.getMessage(), _e );
            return outcome.notOk( "Problem writing root hints file: " + _e.getMessage(), _e );
        }
    }


    /**
     * Decode the most recently read root hints file into a list of resource records.
     *
     * @return the {@link Outcome Outcome&lt;List&lt;DNSResourceRecord&gt;&gt;} result of this operation.
     */
    public Outcome<List<DNSResourceRecord>> decode() {

        if( isEmpty( rootHintsString ) )
            return rrlOutcome.notOk( "No root hints have been read" );

        // first we get the date this file was last updated; we use that to compute the time-to-live as of the moment this method was run...
        Matcher mat = DATE_PATTERN.matcher( rootHintsString );
        if( mat.matches() ) {

            String dateStr = mat.group( 1 );

            DateTimeFormatter parser = DateTimeFormatter.ofPattern( "MMMM d, yyyy HH:mm:ss zzz" );
            ZonedDateTime updated;
            try {
                updated = ZonedDateTime.parse( dateStr + " 00:00:00 GMT", parser );
            }
            catch( Exception _e ) {
                LOGGER.log( Level.WARNING, "Could not parse updated date: " + dateStr, _e );
                return rrlOutcome.notOk( "Could not parse updated date: " + dateStr, _e );
            }
            long updatedMillis = updated.toEpochSecond() * 1000;

            // now we decode the three possible types of records in the root hints file: NS, A, and AAAA...
            List<DNSResourceRecord> entries = new ArrayList<>();

            mat = RR_PATTERN.matcher( rootHintsString );
            while( mat.find() ) {

                String dnStr  = mat.group( 1 );
                String ttlStr = mat.group( 2 );
                String rrtStr = mat.group( 3 );
                String rrdStr = mat.group( 4 );

                Outcome<DNSDomainName> dno = DNSDomainName.fromString( dnStr );
                if( dno.notOk() )
                    return rrlOutcome.notOk( dno.msg(), dno.cause() );
                DNSDomainName dn = dno.info();

                // calculate the TTL; if it's negative, we've expired (note that all the TTLs are the same in the root hints file)...
                int ttlBase = Integer.parseInt( ttlStr );  // should be impossible to throw NumberFormatException...
                long longTTL = updatedMillis + (ttlBase * 1000L) - System.currentTimeMillis();

                // if we've expired, we're not going to decode any records; we'll return with an error...
                if( longTTL < 1 ) {
                    LOGGER.finer( "Root hints entries have expired" );
                    return rrlOutcome.notOk( "Root hints entries have expired" );
                }

                // get the seconds from the milliseconds, and check that it's not too large...
                longTTL /= 1000;
                if( (longTTL & 0xFFFFFFFF00000000L) != 0 ) {
                    LOGGER.finer( "TTL in root hints is too large: " + longTTL );
                    return rrlOutcome.notOk( "TTL in root hints is too large: " + longTTL );
                }

                int ttl = (int)longTTL;

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

                    default -> rrOutcome.notOk( "Unexpected resource record type: " + rrtStr );
                };

                if( rro.notOk() )
                    return rrlOutcome.notOk( rro.msg(), rro.cause() );

                entries.add( rro.info() );
            }
            return rrlOutcome.ok( entries );
        }
        return rrlOutcome.notOk( "Could not find last updated date in root hints file" );
    }


    /**
     * Returns a list of resource records in the current root hints file.  This method will first attempt to read the local root hints file.  If that fails, or if the entries have
     * expired, it will read the latest root hints from the URL, update the local file, and return the fresh root hints from that.  If all of those efforts fail, it will return an
     * error.
     *
     * @return the {@link Outcome Outcome&lt;List&lt;DNSResourceRecord&gt;&gt;} result of this operation.
     */
    public Outcome<List<DNSResourceRecord>> current() {

        // if we read our local root hints file, and we can decode it, then we're good to go...
        Outcome<String> rfo = readFile();
        if( rfo.ok() ) {
            Outcome<List<DNSResourceRecord>> dfo = decode();
            if( dfo.ok() )
                return dfo;
        }

        // something's wrong with our local root hints file - it's missing, bogus, or expired - so we'll have to read the URL...
        Outcome<String> ufo = readURL();
        if( ufo.ok() ) {
            Outcome<?> wfo = writeFile( ufo.info() );
            if( wfo.notOk() )
                LOGGER.log( Level.WARNING, wfo.msg(), wfo.cause() );
            Outcome<List<DNSResourceRecord>> dfo = decode();
            if( dfo.ok() )
                return dfo;
        }

        // if we get here, there's something seriously wrong - we have no valid root hints, so iterative resolution is going to fail...
        return rrlOutcome.notOk( "Cannot read valid root hints" );
    }
}
