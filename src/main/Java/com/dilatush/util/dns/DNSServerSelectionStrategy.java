package com.dilatush.util.dns;

import com.dilatush.util.dns.agent.DNSServerAgent;

/**
 * Enumerates the possible strategies for a query to use when selecting a {@link DNSServerAgent} to resolve the query through.  The {@link #PRIORITY}, {@link #SPEED},
 * {@link #RANDOM}, and {@link #ROUND_ROBIN} strategies all fall back to other agents if a selected agent is down.  The {@link #NAMED} strategy uses a named agent and no other.
 */
public enum DNSServerSelectionStrategy {

    /** Agents with higher priorities are selected first. */
    PRIORITY,

    /** Agents with lower latencies (as specified by the timeout value) are selected first.  */
    SPEED,

    /** Agents are selected at random. */
    RANDOM,

    /** Agents are selected round-robin. */
    ROUND_ROBIN,

    /** Only a particular named agent is selected. */
    NAMED;
}
