package com.dilatush.util.feed;

/**
 * Implemented by feeds that both provide a feed of bytes from a source and accept a feed of bytes to a destination.  This interface is a convenience for the
 * programmer; it is simply an extension of both {@link InFeed} and {@link OutFeed}.
 */
public interface Feed extends InFeed, OutFeed {
}
