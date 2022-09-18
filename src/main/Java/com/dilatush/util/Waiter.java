package com.dilatush.util;

import java.util.concurrent.Semaphore;

public class Waiter<T> {


    private final Semaphore semaphore;

    private T result;


    public Waiter() {
        semaphore = new Semaphore( 0 );
    }


    public T waitForCompletion() {

        try {
            semaphore.acquire();
            return result;
        }
        catch( InterruptedException _e ) {
            return null;
        }
    }


    public void complete( final T _result ) {
        result = _result;
        semaphore.release();
    }
}
