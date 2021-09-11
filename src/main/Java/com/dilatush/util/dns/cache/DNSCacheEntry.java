package com.dilatush.util.dns.cache;

import com.dilatush.util.dns.rr.DNSResourceRecord;

public class DNSCacheEntry {

    public final DNSResourceRecord resourceRecord;

    public final long expiration;


    public DNSCacheEntry( final DNSResourceRecord _resourceRecord, final long _expiration ) {
        resourceRecord = _resourceRecord;
        expiration = _expiration;
    }


    public DNSCacheEntry( final DNSResourceRecord _resourceRecord ) {
        this( _resourceRecord, System.currentTimeMillis() + _resourceRecord.ttl );
    }
}
