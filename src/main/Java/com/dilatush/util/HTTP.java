package com.dilatush.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Static container class for HTTP-related utility functions.
 */
@SuppressWarnings( "unused" )
public class HTTP {

    private static final Outcome.Forge<String>      FORGE_STRING = new Outcome.Forge<>();
    private static final Outcome.Forge<HJSONObject> FORGE_JSON   = new Outcome.Forge<>();


    /**
     * Returns the outcome of an HTTP request to the given URL and expecting an application/text response.  If successful, returns a string result.
     *
     * @param _url The URL to send the request to, like "http://news.com/stuff".
     * @return The outcome of the request.  If ok, the returned string is contained in the outcome.  If not ok, includes an explanatory message and
     * possible the exception that caused it.
     */
    @SuppressWarnings( "JavadocLinkAsPlainText" )
    public static Outcome<String> requestText( final String _url ) {
        return request( _url, "application/text" );
    }


    /**
     * Returns the outcome of an HTTP request to the given URL and expecting an application/json response.  If successful, returns a string result.
     *
     * @param _url The URL to send the request to, like "http://news.com/json".
     * @return The outcome of the request.  If ok, the returned string is contained in the outcome.  If not ok, includes an explanatory message and
     * possible the exception that caused it.
     */
    @SuppressWarnings( "JavadocLinkAsPlainText" )
    public static Outcome<String> requestJSONText( final String _url ) {
        return request( _url, "application/json" );
    }


    /**
     * Returns the outcome of an HTTP request to the given URL and expecting an application/json response.  If successful, returns a string result.
     *
     * @param _url The URL to send the request to, like "http://news.com/json".
     * @return The outcome of the request.  If ok, the returned string is contained in the outcome.  If not ok, includes an explanatory message and
     * possible the exception that caused it.
     */
    @SuppressWarnings( "JavadocLinkAsPlainText" )
    public static Outcome<HJSONObject> requestJSON( final String _url ) {

        try {

            // get the JSON text...
            var reqOut = request( _url, "application/json" );
            if( reqOut.notOk() ) return FORGE_JSON.notOk( reqOut );

            // parse the text into a JSON object and return that...
            var json = new HJSONObject( reqOut.info() );
            return FORGE_JSON.ok( json );
        }
        catch( Exception _e ) {
            return FORGE_JSON.notOk( "Problem parsing JSON from " + _url, _e );
        }
    }


    /**
     * Returns the outcome of an HTTP request to the given URL and expecting an application/json response.  If successful, returns a string result.
     *
     * @param _url The URL to send the request to, like "http://news.com".
     * @param _accept The accept header MIME code, like "application/text".
     * @return The outcome of the request.  If ok, the returned string is contained in the outcome.  If not ok, includes an explanatory message and
     * possible the exception that caused it.
     */
    @SuppressWarnings( "JavadocLinkAsPlainText" )
    public static Outcome<String> request( final String _url, final String _accept ) {

        /* The initial implementation of this method used the new HttpClient.  That implementation used dozens of threads for what was
         * intended to be a simple utility method.  Went back to the old way, synchronous and single threaded.
         */

        try {
            // build the request...
            var url = new URL( _url );
            var httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestProperty( "Accept", _accept );
            httpConn.setRequestMethod( "GET" );
            httpConn.setInstanceFollowRedirects(false);

            // make the request...
            int status = httpConn.getResponseCode();

            // if we didn't get a 200 response code, we've got an error...
            if( status != 200 )
                return FORGE_STRING.notOk( "Not OK response status when reading " + _url + ", status code: " + status );

            // read the response...
            BufferedReader in = new BufferedReader( new InputStreamReader( httpConn.getInputStream() ) );
            String inputLine;
            StringBuilder content = new StringBuilder();
            while( (inputLine = in.readLine()) != null ) {
                content.append( inputLine );
            }
            in.close();

            // cleanup...
            httpConn.disconnect();

            // and we're done...
            return FORGE_STRING.ok( content.toString() );
        }
        catch( Exception _e ) {
            return FORGE_STRING.notOk( "Problem reading " + _url, _e );
        }
    }
}
