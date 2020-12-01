package com.dilatush.util.test;

/**
 * Abstract base class for all test injector classes.
 * 
 * @author Tom Dilatush  tom@dilatush.com
 */
public abstract class ATestInjector<E> implements TestInjector<E> {

    protected Object  testPattern;
    protected boolean enabled;


    /**
     * Set this test injector's test pattern, which may be of any type that a particular test injector accepts.
     *
     * @param _testPattern the test pattern.
     */
    @Override
    public void set( final Object _testPattern ) {
        testPattern = _testPattern;
    }


    /**
     * Enables this test injector.
     */
    @Override
    public void enable() {
        enabled = true;
    }


    /**
     * Disables this test injector.
     */
    @Override
    public void disable() {
        enabled = false;
    }
}
