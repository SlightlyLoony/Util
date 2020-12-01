package com.dilatush.util.test;

/**
 * Implemented by classes that act as test data injectors.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface TestInjector<E> {

    /**
     * If this test injector is enabled, transform the given data by this test injector's test pattern, and return the result.  If this test injector
     * is disabled, return the given data.
     *
     * @param _data the data to be transformed or returned
     * @return the possibly transformed test data
     */
    E inject( final E _data );


    /**
     * Enables this test injector.
     */
    void enable();


    /**
     * Disables this test injector.
     */
    void disable();


    /**
     * Set this test injector's test pattern, which may be of any type that a particular test injector accepts.
     *
     * @param _testPattern the test pattern.
     */
    void set( final Object _testPattern );
}
