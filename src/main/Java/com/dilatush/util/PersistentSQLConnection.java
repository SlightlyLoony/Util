package com.dilatush.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Instances of this class implement a long-term SQL connection, meaning that it will be re-established automatically if it is closed because of
 * timeout, network problem, etc.  Instances of this class are stateful, but threadsafe through synchronization.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class PersistentSQLConnection {


    private static final Logger LOG = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getSimpleName() );

    final private String host;
    final private String password;
    final private String userName;

    private Connection connection;


    /**
     * Creates a new instance of this class with the given SQL user name and password, assuming beast.dilatush.com.
     *
     * @param _host  the host of the SQL server
     * @param _userName the SQL user name to use for this connection
     * @param _password the SQL password to use for this connection
     */
    public PersistentSQLConnection( final String _host, final String _userName, final String _password ) {
        host = _host;
        userName = _userName;
        password = _password;
    }


    /**
     * Creates a new instance of this class with the given SQL user name and password, assuming beast.dilatush.com.
     *
     * @param _userName the SQL user name to use for this connection
     * @param _password the SQL password to use for this connection
     */
    public PersistentSQLConnection( final String _userName, final String _password ) {
        this( "beast.dilatush.com", _userName, _password );
    }


    /**
     * Returns a SQL connection, ready to use.
     *
     * @return the ready to use SQL connection
     */
    public synchronized Connection getConnection() {

        // first we do our best to ensure we have a valid connection...
        try {
            // if we don't have a connection ready to go, then create it...
            if( (connection == null) || !connection.isValid( 500 ) ) {
                createConnection();
            }
        }
        catch( SQLException _e ) {
            createConnection();
        }

        // then we check to make sure it's valid, and if not we make a catastrophe...
        try {
            // if we don't have a connection ready to go, then create it...
            if( (connection == null) || !connection.isValid( 500 ) ) {
                LOG.severe( "Could not get SQL connection" );
                System.exit( 2 );
            }
        }
        catch( SQLException _e ) {
            LOG.log( Level.SEVERE, "Could not get SQL connection", _e );
            System.exit( 3 );
        }

        return connection;
    }


    private void createConnection() {
        try {
            Class.forName( "com.mysql.cj.jdbc.Driver" ).newInstance();
            String sqlURL = "jdbc:mysql://" + host + "?autoReconnect=true&useSSL=false";
            connection = DriverManager.getConnection( sqlURL, userName, password );
        }
        catch( Exception _e ) {
            LOG.log( Level.SEVERE, "Could not get SQL connection", _e );
            System.exit( 1 );
        }
    }
}
