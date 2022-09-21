package com.dilatush.util;

import java.util.concurrent.Semaphore;

/**
 * Instances of this class provide a convenient way to wait for the completion of some task, where the completion is noted by calling a {@code Consumer<T>}.  One might have code
 * like this:
 * <pre>{@code
 *         // create the Waiter instance
 *         Waiter<String> waiter = new Waiter<>();
 *
 *         // starts a process that may take a few seconds
 *         initiateDownload( "https://wikipedia.com", waiter::complete );
 *
 *         // blocks until the download completes, and returns the downloaded text
 *         String text = waiter.waitForCompletion();
 * }</pre>
 * This is particularly convenient for making an asynchronous (i.e., non-blocking) API behave as if it were a synchronous (i.e., blocking) API.
 * @param <T> The type provided by the completion call to a {@code Consumer<T>}, and returned by the {@link #waitForCompletion()} method.
 */
public class Waiter<T> {

    // the semaphore used to block until completion...
    private final Semaphore semaphore;

    // the result, provided by the call to complete(T)...
    private T result;


    /**
     * Creates a new instance of this class.
     */
    public Waiter() {
        semaphore = new Semaphore( 0 );
    }


    /**
     * Blocks until {@link #complete(T)} is called, then returns the instance of {@code T} provided via {@link #complete(T)}.
     *
     * @return The instance of {@code T} provided via {@link #complete(T)}
     */
    public T waitForCompletion() {

        try {
            semaphore.acquire();
            return result;
        }
        catch( InterruptedException _e ) {
            return null;
        }
    }


    /**
     * Calling this method indicates that an action has completed, with the given result.  This will cause {@link #waitForCompletion()} to stop blocking and return the result.
     *
     * @param _result the result of the action that completed.
     */
    public void complete( final T _result ) {
        result = _result;
        semaphore.release();
    }
}
