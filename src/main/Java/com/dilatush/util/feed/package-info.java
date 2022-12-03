/**
 * <p>This package implements "feeds", which are similar to streams - except that they have asynchronous (non-blocking) interfaces as well as synchronous (blocking) interfaces,
 * and they use {@link java.nio.ByteBuffer}s instead of bytes or byte arrays, and {@link com.dilatush.util.Outcome}s instead of {@link java.lang.Exception}s.  This makes them
 * eminently suitable for use with asynchronous components such as the {@link com.dilatush.util.networkingengine.NetworkingEngine}.  These interfaces form the core of feeds:</p>
 * <ul>
 *     <li>Implementations of {@link com.dilatush.util.feed.InFeed} accepts bytes from another {@link com.dilatush.util.feed.InFeed}, transforms them somehow, then makes the
 *     transformed bytes available.  An example is </li>
 *     <li></li>
 *     <li>Implementations of {@link com.dilatush.util.feed.InFeedSource} (an extension of {@link com.dilatush.util.feed.InFeed}) make bytes available as an
 *     {@link com.dilatush.util.feed.InFeed}, from any source <i>other than</i> another {@link com.dilatush.util.feed.InFeed}.  For instance,
 *     {@link com.dilatush.util.feed.BufferedInFeedSource} has methods to fill a buffer with bytes that are then available as an {@link com.dilatush.util.feed.InFeed}.  </li>
 *     <li>Implementations of {@link com.dilatush.util.feed.OutFeedSink} (an extension of {@link com.dilatush.util.feed.OutFeed}) make bytes from any source <i>other than</i>
 *     an {@link com.dilatush.util.feed.InFeed}.  For instance, {@link com.dilatush.util.feed.BufferedOutFeedSink} accepts bytes as an {@link com.dilatush.util.feed.OutFeed},
 *     then has methods to empty the buffer those bytes were stored in.</li>
 * </ul>
 */
package com.dilatush.util.feed;
/*
Similarly, {@link com.dilatush.util.networkingengine.TCPInboundPipe} accepts bytes from the inbound side of a TCP connection, and makes those available as an {@link com.dilatush.util.feed.InFeed}.
 */