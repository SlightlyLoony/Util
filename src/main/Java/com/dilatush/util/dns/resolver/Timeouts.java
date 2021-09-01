package com.dilatush.util.dns.resolver;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * Instances of this class efficiently manage a collection of timeouts.  Methods allow for adding new timeouts, removing timeouts, cancelling
 * timeouts, and checking timeouts to see if they've actually timed out.
 */
public class Timeouts {

    private final List<Timeout> timeouts = new ArrayList<>();


    public synchronized void add( final Timeout _newTimeout ) {
        for( int i = timeouts.size() - 1; i >= 0; i-- ) {
            if( _newTimeout.getExpiration() <= timeouts.get( i ).getExpiration() ) {
                timeouts.add( i + 1, _newTimeout );
                return;
            }
        }
        timeouts.add( _newTimeout );
    }


    public synchronized void check() {
        int i;
        for( i = 0; i < timeouts.size(); i++ ) {
            if( !timeouts.get( i ).hasExpired() )
                break;
        }
        timeouts.subList( 0, i ).clear();
    }
}
