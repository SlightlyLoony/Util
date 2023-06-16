package com.dilatush.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


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

        try {
            // create a client
            var client = HttpClient.newHttpClient();

            // create a request
            var request = HttpRequest.newBuilder( URI.create( _url ) )
                    .header("accept", _accept )
                    .build();

            // use the client to send the request
            var response = client.send( request, HttpResponse.BodyHandlers.ofString() );
            var status = response.statusCode();
            if( status != 200 )
                return FORGE_STRING.notOk( "Not OK response status when reading " + _url + ", status code: " + status );
            return FORGE_STRING.ok( response.body() );
        }
        catch( Exception _e ) {
            return FORGE_STRING.notOk( "Problem reading " + _url, _e );
        }
    }
}
