package com.dilatush.util.test;

import com.dilatush.util.AConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.dilatush.util.Checks.notEmpty;
import static com.dilatush.util.General.isNull;

/**
 * Implements a singleton class that manages the test framework.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestManager {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static TestManager instance;

    private final Map<String, ProxyTestEnabler> enablers;

    private Map<String, Map<String, TestEnabler>> scenarios;
    private boolean enabled;
    private String scenario;


    /**
     * Creates a new instance of this class.
     */
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
                else {
                    LOGGER.warning( "Test scenario is not in the configuration: " + scenario );
                }
            }
        }
        return result;
    }


    /**
     * Disable all testing.  This will set all registered test enablers to {@link FalseTestEnabler}, set the scenario name to empty, and clear the
     * enabled flag.
     */
    public void disable() {

        // iterate over all the registered test enablers, setting them to default (false) enablers...
        FalseTestEnabler fte = new FalseTestEnabler( new HashMap<>() );
        for( ProxyTestEnabler pte : enablers.values() ) {
            pte.setTestEnabler( fte );
        }

        enabled = false;
        scenario = "";
        LOGGER.info( "Testing disabled" );
    }


    /**
     * Enable testing with the test scenario of the given name.  If testing was already enabled, invoking this method has the effect of
     * changing the test scenario to the one of the given name.
     *
     * @param _scenario The name of the test scenario to enable.
     */
    public void enable( final String _scenario ) {

        // handle the case where testing is enabled already...
        if( enabled )
            disable();

        // record the new scenario's name...
        scenario = _scenario;

        // get our new scenario...
        Map<String, TestEnabler> newScenario = scenarios.get( _scenario );
        if( newScenario == null ) {
            LOGGER.warning( "Tried to enable a scenario that's not in the configuration: " + _scenario );
            return;
        }

        // iterate over the test enabler names in the new scenario...
        for( String enablerName : newScenario.keySet() ) {

            // if there is a test enabler by that name...
            if( enablers.containsKey( enablerName ) ) {

                // get the test enabler configured for the new scenario...
                TestEnabler newEnabler = newScenario.get( enablerName );

                // set the configured test enabler...
                enablers.get( enablerName ).setTestEnabler( newEnabler );
            }
        }
        init();
        LOGGER.info( "Enabled test scenario: " + _scenario );
    }


    /**
     * Initialize all the test enablers in the current scenario.
     */
    public void init() {
        enablers.forEach( (k,v) -> v.init() );
    }


    /**
     * Returns <code>true</code> if testing is enabled.
     *
     * @return <code>true</code> if testing is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }


    /**
     * Returns the name of the current test scenario.
     *
     * @return the name of the current test scenario
     */
    @SuppressWarnings( "unused" )
    public String getScenario() {
        return scenario;
    }


    /**
     * Sets the configuration for the singleton instance of the test manager.  This method should be called just once, before any tested code
     * executes.
     *
     * @param _config The configuration for this instance.
     */
    public void setConfig( final Config _config ) {
        enabled   = _config.enabled;
        scenario  = _config.scenario;
        scenarios = _config.scenarios;
        if( isNull( scenario ) )
            scenario = "";
        init();
    }


    /**
     * Returns the singleton instance of this class, instantiating it first if necessary.
     *
     * @return the singleton instance of this class.
     */
    public static synchronized TestManager getInstance() {
        if( instance == null )
            instance = new TestManager();
        return instance;
    }


    /**
     * Configuration for {@link TestManager}.
     */
    public static class Config extends AConfig {

        public boolean enabled;
        public String scenario;
        public Map<String, Map<String, TestEnabler>> scenarios;


        /**
         * Implemented by subclasses to verify that their fields are valid.  When possible, this should be accomplished by a series of invocations of
         * {@link #validate(Validator, List, String)}, one or more times for each field in the configuration.
         *
         * @param _messages The list of configuration error messages.  Each error found in {@code verify()} should add an explanatory message to
         *                  this list.
         */
        @Override
        public void verify( final List<String> _messages ) {
            validate( () -> (scenarios != null), _messages, "The scenarios map may not be null" );
        }
    }
}

