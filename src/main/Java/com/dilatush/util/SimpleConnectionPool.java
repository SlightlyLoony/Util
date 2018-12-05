package com.dilatush.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a very simple connection pool for MySQL connections (JDBC).  The pool has a configurable size (that is, maximum number of connections
 * cached), and a configurable limit on how many connections may be extant at any moment.  Connections are created as they are needed, and when
 * released (closed) by the client, they are returned to the cache for reuse by another client (or the same client later).  Cached connections are
 * stored in a FIFO queue, with the oldest cached connection being the next one used.  If the maximum number of cached connections would be exceeded
 * by a newly released connection being cached, then the oldest cached connection is closed.  If the maximum number of extant connections would be
 * exceeded by a request for a connection when the cache is empty, the call for a new connection will be blocked until another client releases a
 * connection, and a warning is logged.  This behavior limits the memory consumption caused by leaking connections, and also helps identify leaking
 * (i.e., unclosed) connections.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SimpleConnectionPool {


    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    final static private long TICKLER_PERIOD = 10 * 1000;

    final private String        host;
    final private String        password;
    final private String        userName;
    final private int           maxCached;
    final private int           maxExtant;
    final private AtomicInteger created;  // count of connections created...
    final private Timer         timer;

    final private LinkedBlockingDeque<PooledConnection> cache;  // cache of unused, available connections...
    final private Set<PooledConnection> extant;                 // set of all created connections...


    public SimpleConnectionPool( final String _host, final String _password, final String _userName, final int _maxCached, final int _maxExtant ) {

        host      = _host;
        password  = _password;
        userName  = _userName;
        maxCached = _maxCached;
        maxExtant = _maxExtant;
        timer     = new Timer( "Tickler", true );

        created   = new AtomicInteger( 0 );

        cache     = new LinkedBlockingDeque<>();
        extant    = new HashSet<>();

        // schedule our 10 second "tickler"...
        timer.scheduleAtFixedRate( new Tickler(), TICKLER_PERIOD, TICKLER_PERIOD );

        // add our shutdown hook...
        Runtime.getRuntime().addShutdownHook( new Shutdown() );
    }


    /**
     * Returns a MySQL {@link Connection} instance, which is actually a {@link PooledConnection} instance.  This connection may be freshly created,
     * or it may be an already-existing, but no longer used connection that has been cached.  If the limit of connections created has been reached,
     * this method can block for an arbitrary length of time, waiting for another thread to release a connection for reuse.
     *
     * @return the connection
     * @throws SQLException on any SQL problem
     * @throws InterruptedException if interrupted while waiting for a connection to be released.
     */
    public Connection getConnection() throws SQLException, InterruptedException {

        // if we can we get a connection from the cache, return it...
        PooledConnection connection = cache.pollLast();
        if( connection != null ) {
            LOGGER.finer( "Reusing cached connection" + toString() );
            connection.obtained();
            return connection;
        }

        // create a new connection, if we still allowed to create more of them?
        if( created.accumulateAndGet(1, (a, b) ->  (a >= maxExtant) ? a : a + b ) <= maxExtant ) {
            connection = createConnection();
            synchronized( extant ) { extant.add( connection ); }
            LOGGER.finer( "Creating new connection" + toString() );
            connection.obtained();
            return connection;
        }

        // no connections cached, can't create a new one -- so we wait until another thread releases a connection...
        long startWait = System.currentTimeMillis();
        LOGGER.finer( "Waiting for released connection" + toString() );
        connection = cache.takeLast();  // this blocks until a cached connection is available...
        long waitTime = System.currentTimeMillis() - startWait;
        LOGGER.finer( "Got released connection after " + waitTime + "ms" + toString() );
        connection.obtained();
        return connection;
    }


    /**
     * Release the given connection back to the pool for reuse.  The caller is responsible for finishing (committing or rolling back) any
     * transactions made while it was using the connection.
     *
     * @param _connection the connection to release.
     */
    public void release( final PooledConnection _connection ) {

        // if we've reached the limit for number of cached connections, toss the last one away...
        if( cache.size() >= maxCached ) {
            LOGGER.finer( "Discarding cached connection, too many" + toString() );
            PooledConnection connection = cache.pollLast();
            discardConnection( connection );
        }

        // poke the connection into the cache...
        cache.addFirst( _connection );
        LOGGER.finer( "Cached released connection" + toString() );
    }


    @Override
    public String toString() {
        return " (created: " + created.get() + ", cached: " + cache.size() + ")";
    }


    public String getHost() {
        return host;
    }


    public String getPassword() {
        return password;
    }


    public String getUserName() {
        return userName;
    }


    private PooledConnection createConnection() throws SQLException {
        try {
            Class.forName( "com.mysql.cj.jdbc.Driver" ).newInstance();
            String sqlURL = "jdbc:mysql://" + host + "?useSSL=false";
            return new PooledConnection( this, DriverManager.getConnection( sqlURL, userName, password ) );
        }
        catch( Exception _e ) {
            LOGGER.log( Level.SEVERE, "Failed to create JDBC connection to MySQL", _e );
            throw new SQLException( "Failed to create JDBC connection" );
        }
    }


    /**
     * Make sure the given connection is actually closed, but without propagating any exceptions (as there may actually be something wrong
     * with the connection).
     *
     * @param _connection the connection to discard
     */
    private void discardConnection( final PooledConnection _connection ) {

        // ignore any nulls that might accidentally be thrown our way...
        if( _connection == null )
            return;

        try {

            // it's not closed, so we need to do it ourselves...
            if( ! _connection.isClosed() )
                _connection.reallyClose();

            // remove it from our set of extant connections...
            synchronized( extant ) { extant.remove( _connection ); }
        }

        // if we get ANY error while closing, we just leave...
        catch( SQLException _e ) {
            // naught to do here...
        }

        // update our count of created connections...
        created.decrementAndGet();
    }


    private class Tickler extends TimerTask {

        /**
         * Runs every 10 seconds.  Get the last connection in the cache, if there is one, and check it for validity.  Discard invalid connections,
         * and re-insert valid ones. Also, if a connection hasn't been used in ten minutes, close it.
         */
        @Override
        public void run() {

            LOGGER.finer( "Tickler running" + SimpleConnectionPool.this.toString() );

            // if we can't even get a cached connection, just leave...
            PooledConnection connection = cache.pollLast();
            if( connection == null )
                return;

            // if the connection hasn't been used for ten minutes, then discard it...
            if( (System.currentTimeMillis() - connection.getObtained()) > 10 * 60 * 1000 ) {
                discardConnection( connection );
                LOGGER.finer( "Discarded connection disused for over ten minutes" + SimpleConnectionPool.this.toString() );
                return;
            }

            // if the connection is closed for some reason, or if it isn't valid, or if testing it causes an exception, then discard it...
            try {
                if( connection.isClosed() || !connection.isValid( 1 ) ) {
                    discardConnection( connection );
                    LOGGER.finer( "Discarded closed or invalid connection" + SimpleConnectionPool.this.toString() );
                    return;
                }
            }
            catch( SQLException _e ) {
                discardConnection( connection );
                LOGGER.finer( "Discarded connection that errored on checking" + SimpleConnectionPool.this.toString() );
                return;
            }

            // otherwise, getting here means we have a good connection, so toss it back into the cache...
            cache.addFirst( connection );
            LOGGER.finer( "Re-cached validated, tickled connection" + SimpleConnectionPool.this.toString() );
        }
    }


    /**
     * Implements a simple shutdown hook to close all extant connections...
     */
    private class Shutdown extends Thread {

        @Override
        public void run() {

            LOGGER.fine( "Shutdown hook: closing all MySQL connections" );

            // close all the extant connections, ignoring any exceptions...
            extant.forEach( connection -> {
                try {
                    connection.reallyClose();
                }
                catch( SQLException _e ) {
                    // do nothing; we're just gonna ignore problems during shutdown...
                }
            } );
        }
    }
}
