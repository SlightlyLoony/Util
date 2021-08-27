package com.dilatush.util.dns.rr;

//   +---------------------------+
//   | See RFC 1035 for details. |
//   +---------------------------+

import com.dilatush.util.Outcome;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

/**
 * Instances of this class represent an IPv4 Internet address.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class A extends DNSResourceRecord {

    public final Inet4Address address;


    public static Outcome<A> decode( final ByteBuffer _msgBuffer, final Init _init ) {

    }
}
