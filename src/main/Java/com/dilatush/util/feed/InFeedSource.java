package com.dilatush.util.feed;

public interface InFeedSource extends InFeed {

    /**
     * Return {@code true} if a read operation is already in progress.
     *
     * @return {@code true} if a read operation is already in progress.
     */
    boolean isReading();
}
