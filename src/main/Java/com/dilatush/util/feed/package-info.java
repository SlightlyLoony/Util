/**
 * <p>This package implements "feeds", which are conceptually similar to streams - except that they have asynchronous (non-blocking) interfaces as well as synchronous (blocking)
 * interfaces, and they use {@link java.nio.ByteBuffer}s instead of bytes or byte arrays, and {@link com.dilatush.util.Outcome}s instead of {@link java.lang.Exception}s.  This
 * makes them eminently suitable for use with asynchronous components such as the {@link com.dilatush.util.networkingengine.NetworkingEngine}.  These three interfaces form the
 * core of the feeds package:</p>
 * <ul>
 *     <li>Implementations of {@link com.dilatush.util.feed.InFeed} accept bytes from some source, optionally transform them somehow, then make the resulting bytes available from
 *     the {@link com.dilatush.util.feed.InFeed}.  Note that the source of the bytes could be another {@link com.dilatush.util.feed.InFeed}.  An example is
 *     {@link com.dilatush.util.feed.BufferedInFeed}, which accepts bytes through its own API, then makes the same bytes available as an {@link com.dilatush.util.feed.InFeed}.</li>
 *     <li>Implementations of {@link com.dilatush.util.feed.OutFeed} accept bytes from the {@link com.dilatush.util.feed.OutFeed}, optionally transform them somehow, then
 *     make the resulting bytes available to to some destination.  Note that the destination of the bytes could be another {@link com.dilatush.util.feed.OutFeed}.  An example is
 *     {@link com.dilatush.util.feed.BufferedOutFeed}, which accepts bytes as an {@link com.dilatush.util.feed.OutFeed}, then makes the same bytes available through its
 *     own API.</li>
 *     <li>The {@link com.dilatush.util.feed.Feed} interface simply extends both {@link com.dilatush.util.feed.InFeed} and {@link com.dilatush.util.feed.OutFeed}.  Implementations
 *     of {@link com.dilatush.util.feed.Feed} act as both an {@link com.dilatush.util.feed.InFeed} and an {@link com.dilatush.util.feed.OutFeed}.  Examples include
 *     {@link com.dilatush.util.feed.BufferedPipedFeed} (which accepts bytes from the {@link com.dilatush.util.feed.OutFeed}, buffers them, and makes the same bytes available from
 *     the {@link com.dilatush.util.feed.InFeed}) and {@link com.dilatush.util.networkingengine.TCPPipe} (which makes bytes read from a network TCP connection available from the
 *     {@link com.dilatush.util.feed.InFeed}, and accepts bytes from the {@link com.dilatush.util.feed.OutFeed} and writes them to a network TCP connection).</li>
 * </ul>
 */
package com.dilatush.util.feed;
