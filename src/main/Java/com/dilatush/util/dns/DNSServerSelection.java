package com.dilatush.util.dns;

import com.dilatush.util.Checks;

import static com.dilatush.util.dns.DNSServerSelectionStrategy.*;

public class DNSServerSelection {

    public final DNSServerSelectionStrategy strategy;
    public final String agentName;


    private DNSServerSelection( final DNSServerSelectionStrategy _strategy, final String _agentName ) {

        strategy = _strategy;
        agentName = _agentName;
    }


    public static DNSServerSelection random() {
        return new DNSServerSelection( RANDOM, null );
    }


    public static DNSServerSelection roundRobin() {
        return new DNSServerSelection( ROUND_ROBIN, null );
    }


    public static DNSServerSelection priority() {
        return new DNSServerSelection( PRIORITY, null );
    }


    public static DNSServerSelection speed() {
        return new DNSServerSelection( SPEED, null );
    }


    public static DNSServerSelection named( final String _agentName ) {

        Checks.notEmpty( _agentName );
        return new DNSServerSelection( RANDOM, _agentName );
    }
}
