package com.dilatush.util.test;

import com.dilatush.util.AConfig;

import java.util.HashMap;
import java.util.Map;

import static com.dilatush.util.Checks.notEmpty;

/**
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestManager {

    private static TestManager instance;

    private final Map<String, ProxyTestEnabler> enablers;

    private Map<String, Map<String, TestEnabler>> scenarios;
    private boolean enabled;
    private String scenario;


    private TestManager(){

        enablers = new HashMap<>();
    }


    /**
     * <p>Register a new test enabler with the test manager, under the given name.  If a test enabler with the same name has already been registered,
     * throws a {@link IllegalArgumentException}.</p>
     *
     * @param _name The name of the test enabler to register.
     * @return the registered test enabler
     */
    public TestEnabler register( final String _name ) {

        // make sure we've got a reasonable argument...
        notEmpty( _name );
        if( enablers.containsKey( _name ) )
            throw new IllegalArgumentException( "Duplicate test enabler name: " + _name );

        // create our proxy and put it into our registrations...
        ProxyTestEnabler result = new ProxyTestEnabler();
        enablers.put( _name, result );

        // default to a false test enabler...
        result.setTestEnabler( new FalseTestEnabler( new HashMap<>() ) );

        // if we're enabled and we have a configuration for the current scenario, use it...
        if( enabled ) {
            if( scenario != null ) {
                Map<String, TestEnabler> s = scenarios.get( scenario );
                if( s != null ) {
                    TestEnabler te = s.get( _name );
                    if( te != null ) {
                        result.setTestEnabler( te );
                        result.init();
                    }
                }
            }
        }
        return result;
    }


    public void setConfig( final Config _config ) {
        enabled = _config.enabled;
        scenario = _config.scenario;
        scenarios = _config.scenarios;
    }


    public static synchronized TestManager getInstance() {
        if( instance == null )
            instance = new TestManager();
        return instance;
    }


    public static class Config extends AConfig {

        public boolean enabled;
        public String scenario;
        public Map<String, Map<String, TestEnabler>> scenarios;


        /**
         * Implemented by subclasses to verify that their fields are valid.  When possible, this should be accomplished by a series of invocations of
         * {@link #validate(Validator, String)}, one or more times for each field in the configuration.
         */
        @Override
        protected void verify() {

        }
    }
}

