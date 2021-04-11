package com.dilatush.util.test;

import com.dilatush.util.config.AConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.dilatush.util.Checks.notEmpty;
import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a singleton class that manages the test framework.  The singleton instance <i>must</i> be configured prior to retrieving its instance.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestManager {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static TestManager instance;   // the one and only instance of this class
    private static Config      config;     // the one and only configuration for this class

    private final Map<String, ProxyTestEnabler>         enablers;  // registered test enabler name -> proxy test enabler
    private final Map<String, Map<String, TestEnabler>> scenarios; // scenario name -> (test enabler name -> test enabler)

    private boolean enabled;
    private String  scenario;


    /**
     * Creates a new instance of this class.
     */
    private TestManager( final Config _config ){

        // make a place to keep track of our enablers...
        enablers = new ConcurrentHashMap<>();

        // stuff away our map of test scenarios...
        scenarios = _config.scenarios;

        // set up our default state and scenario...
        scenario  = _config.scenario;

        // if we had no default scenario, make sure it's not null...
        if( isNull( scenario ) )
            scenario = "";

        // initialize our registered enablers...
        init();

        // if the configuration says we're enabled, and we have a default scenario, do it...
        if( _config.enabled && !isEmpty( scenario ) )
            enable( scenario );
    }


    /**
     * Configure the {@code TestManager}.  This method <i>must</i> be called just once, and it <i>must</i> be called before any invocation of
     * {@link #getInstance}.  Failure to do either will result in a {@link IllegalStateException} being thrown.
     *
     * @param _config The configuration for {@code TestManager}.
     */
    public static synchronized void configure( final Config _config ) {

        // if we've already been configured, squawk...
        if( isNotNull( config ) )
            throw new IllegalStateException( "TestManager has already been configured" );

        // otherwise we configure it...
        config = _config;
    }


    /**
     * Returns the singleton instance of {@code TestManager}.  If {@code TestManager} has not been configured, an {@link IllegalStateException} will
     * be thrown.  If {@code TestManager} has not already been instantiated, invoking this method will do so.
     *
     * @return the singleton instance of {@code TestManager}.
     */
    public static synchronized TestManager getInstance() {

        // if we have our instance, just leave with it...
        if( isNotNull( instance ) )
            return instance;

        // if we don't have a configuration, make loud noises...
        if( isNull( config ) )
            throw new IllegalStateException( "TestManager has not been configured" );

        // make our singleton instance...
        instance = new TestManager( config );
        return instance;
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
        enabled = true;
        LOGGER.info( "Enabled test scenario: " + _scenario );
    }


    /**
     * Returns a list of the names of all the known scenarios.
     *
     * @return the list of scenarios
     */
    public List<String> getScenarios() {
        return new ArrayList<>( scenarios.keySet() );
    }


    /**
     * Returns a list of the names of all the registered enablers.
     *
     * @return the list of enablers
     */
    public List<String> getEnablers() {
        return new ArrayList<>( enablers.keySet() );
    }


    /**
     * Return the named enabler, or {@code null} if it doesn't exist.
     *
     * @param _name The name of the enabler to get.
     * @return the named test enabler
     */
    /*package-private*/ TestEnabler getEnabler( final String _name ) {
        return enablers.get( _name ).getTestEnabler();
    }


    /**
     * Initialize all the registered test enablers.
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

