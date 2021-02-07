package com.dilatush.util.info;

/**
 * Implemented by classes that can read an {@link Info} object.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface InfoViewer<T> {

    /**
     * Return the {@link Info} instance that this viewer has access to.
     *
     * @return the {@link Info} instance that this viewer has access to
     */
    Info<T> get();
}
