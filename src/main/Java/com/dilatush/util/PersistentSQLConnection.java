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

    final private ThreadLocal<Connection> perThreadConnection;


    /**
     * Creates a new instance of this class with the given SQL user name and password, assuming beast.dilatush.com.
     *
     * @param _host  the host of the SQL server
     * @param _userName the SQL user name to use for this connection
     * @param _password the SQL password to use for this connection
     */
    public PersistentSQLConnection( final String _host, final String _userName, final String _password ) {
        perThreadConnection = new ThreadLocal<>();
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
    public Connection getConnection() {

        // get the connection, if any, for this thread...
        Connection connection = perThreadConnection.get();

        // first we do our best to ensure we have a valid connection...
        try {
            // if we don't have a connection ready to go, then create it...
            if( (connection == null) || !connection.isValid( 500 ) ) {
                connection = createConnection();
                perThreadConnection.set( connection );
            }
        }
        catch( SQLException _e ) {
            connection = createConnection();
            perThreadConnection.set( connection );
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


    public String getHost() {
        return host;
    }


    public String getPassword() {
        return password;
    }


    public String getUserName() {
        return userName;
    }


    private Connection createConnection() {
        try {
            Class.forName( "com.mysql.cj.jdbc.Driver" ).newInstance();
            String sqlURL = "jdbc:mysql://" + host + "?autoReconnect=true&useSSL=false";
            return DriverManager.getConnection( sqlURL, userName, password );
        }
        catch( Exception _e ) {
            LOG.log( Level.SEVERE, "Could not get SQL connection", _e );
            System.exit( 1 );
            return null;  // makes the compiler's error-checking happy...
        }
    }
}
