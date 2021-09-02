package com.dilatush.util.dns.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Instances of this class efficiently manage a collection of timeouts.  Methods allow for adding new timeouts and checking timeouts to see if they've
 * actually timed out.
 */
public class Timeouts {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // The list of active timeouts, in order of their expiration times (earlier times first)...
    private final List<Timeout> timeouts = new ArrayList<>();


    public synchronized void add( final Timeout _newTimeout ) {

        LOGGER.finest( "Adding timeout" );

        // Walk backwards down the list, looking for a timeout expiring no later than the new timeout; we want to insert the
        // new one right after that.  We walk backwards on the theory that if the list is large, the insertion point will be closer to the end than
        // to the beginning of the list...
        for( int i = timeouts.size() - 1; i >= 0; i-- ) {

            // if the timeout from the list expires no later than the new timeout...
            if( timeouts.get( i ).getExpiration() <= _newTimeout.getExpiration() ) {

                // insert the new one right after it, and we're done...
                timeouts.add( i + 1, _newTimeout );
                return;
            }
        }

        // if the list was empty, or all the timeouts in it expire after the new one, then the new one needs to be the first in the list...
        timeouts.add( 0, _newTimeout );
    }


    /**
     * Check to see if any of the timeouts have expired, and if so, handle them.
     */
    public synchronized void check() {

        LOGGER.finest( "Checking timeouts" );

        // walk the list of timeouts until we run into one that has not expired yet...
        int i;
        for( i = 0; i < timeouts.size(); i++ ) {
            if( !timeouts.get( i ).hasExpired() )
                break;
        }

        // clear all the expired timeouts off the beginning of the list...
        if( i > 0)
            LOGGER.finest( "Deleting " + i + " timeouts" );
        timeouts.subList( 0, i ).clear();
    }
}
