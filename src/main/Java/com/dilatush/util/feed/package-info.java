package com.dilatush.util.feed;

/**
 * <p>This package implements "feeds", which are similar to streams - except that they have asynchronous (non-blocking) interfaces as well as synchronous (blocking) interfaces,
 * and they use {@link java.nio.ByteBuffer}s instead of bytes or byte arrays, and {@link com.dilatush.util.Outcome}s instead of {@link java.lang.Exception}s.  This makes them
 * eminently suitable for use with asynchronous components such as the {@link com.dilatush.util.networkingengine.NetworkingEngine}.</p>
 */