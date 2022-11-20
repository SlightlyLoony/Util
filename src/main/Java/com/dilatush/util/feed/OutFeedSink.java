package com.dilatush.util.feed;

public interface OutFeedSink extends OutFeed {

    /**
     * Return {@code true} if a write operation is already in progress.
     *
     * @return {@code true} if a write operation is already in progress.
     */
    boolean isWriting();
}
