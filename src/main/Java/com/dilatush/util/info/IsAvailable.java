package com.dilatush.util.info;

/**
 * Implemented by information classes that need to provide their own response to {@link Info#isAvailable}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface IsAvailable {

    /**
     * Return {@code true} if the information is available.
     *
     * @return {@code true} if the information is available
     */
    boolean isAvailable();
}
