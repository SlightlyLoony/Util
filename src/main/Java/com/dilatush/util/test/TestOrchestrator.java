package com.dilatush.util.test;

import com.dilatush.util.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Instances of this class control the timing of test injectors' enabling and test patterns.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestOrchestrator {

    private final ScheduledExecutorService scheduledExecutorService;
    @SuppressWarnings("rawtypes")
    private final Map<String, TestInjector> map;


    public TestOrchestrator( final ScheduledExecutorService _scheduledExecutorService ) {
        scheduledExecutorService = _scheduledExecutorService;
        map = new ConcurrentHashMap<>();
    }


    /*
       "testOrchestration": [
         {"type": "pattern", "name": "TempReader.readBattery", "setMS":    1000, "pattern":  "0x00010001" },
         {"type": "enable",  "name": "TempReader.readBattery", "enableMS": 1001, "disableMS":  30000 }
       ]
     */


    /**
     * Schedule test events defined by a JSON configuration file.  The file must have a root level element "testOrchestration" that defines an array
     * of tests, as in the example below:
     * <code>
     *        "testOrchestration": [
     *          {"type": "pattern", "name": "TempReader.readBattery", "setMS":    1000, "pattern":  "0x00010001" },
     *          {"type": "enable",  "name": "TempReader.readBattery", "enableMS": 1001, "disableMS":  30000 }
     *        ]
     * </code>
     * @param _config the configuration file
     * @throws IllegalStateException if there are any problems with the configuration file
     */
    public void schedule( final Config _config ) {

        try {
            JSONArray tests = _config.getJSONArray( "testOrchestration" );
            for( int i = 0; i < tests.length(); i++ ) {

                JSONObject test = tests.getJSONObject( i );
                String testType = test.getString( "type" );
                String testName = test.getString( "name" );
                if( "pattern".equals( testType ) ) {

                    long setMS = test.getLong( "setMS" );
                    Object pattern = test.get( "pattern" );
                    schedule( testName, setMS, pattern );
                }
                else if( "enable".equals( testType ) ) {

                    long enableMS = test.getLong( "enableMS" );
                    long disableMS = test.getLong( "disableMS" );
                    schedule( testName, enableMS, disableMS );
                }
                else
                    throw new IllegalStateException( "Unknown test orchestration type: " + testType );
            }
        }
        catch( JSONException _e ) {
            throw new IllegalStateException( "Problem with test orchestration JSON", _e );
        }
    }

    /**
     * Register the given test injector with this orchestrator, under the given name.
     *
     * @param _testInjector the test injector to register
     * @param _name the name to register the test injector under
     */
    @SuppressWarnings("rawtypes")
    public void registerTestInjector( final TestInjector _testInjector, final String _name ) {

        if( map.containsKey( _name ) )
            throw new IllegalStateException( "Test injector " + _name + " already exists" );

        map.put( _name, _testInjector );
    }


    /**
     * Schedules the test injector with the given name to be enabled with the given milliseconds from the invocation time, and disabled with the
     * given milliseconds from the enable time.
     *
     * @param _name the name of the test injector to schedule
     * @param _msToEnable the milliseconds from the invocation time to enable the test injector
     * @param _msToDisable the milliseconds from the enable time to disable the test injector
     * @throws IllegalStateException if test injector does not exist, or either milliseconds value is negative
     */
    public void schedule( final String _name, final long _msToEnable, final long _msToDisable ) {

        @SuppressWarnings("rawtypes")
        TestInjector testInjector = map.get( _name );
        if( testInjector == null )
            throw new IllegalStateException( "Test injector " + _name + " does not exist" );

        if( (_msToDisable < 0 ) || (_msToEnable < 0) )
            throw new IllegalStateException( "Negative enable or disable times" );

        scheduledExecutorService.schedule( new EnableTask( testInjector ), _msToEnable, TimeUnit.MILLISECONDS );
        scheduledExecutorService.schedule( new DisableTask( testInjector ), _msToDisable + _msToEnable, TimeUnit.MILLISECONDS );
    }


    /**
     * Schedules the test injector with the given name to have its test pattern set to the given value at the given time from the invocation time.
     *
     * @param _name the name of the test injector to schedule
     * @param _msToSet the milliseconds from the invocation time to set the test pattern
     * @param _testPattern the test pattern to set at the given time
     * @throws IllegalStateException if test injector does not exist or if the given milliseconds are negative
     */
    public void schedule( final String _name, final long _msToSet, final Object _testPattern ) {

        @SuppressWarnings("rawtypes")
        TestInjector testInjector = map.get( _name );
        if( testInjector == null )
            throw new IllegalStateException( "Test injector " + _name + " does not exist" );

        if( _msToSet < 0 )
            throw new IllegalStateException( "Negative set test pattern time" );

        scheduledExecutorService.schedule( new SetTestPatternTask( testInjector, _testPattern ), _msToSet, TimeUnit.MILLISECONDS );
    }


    /**
     * Simple task that sets the test pattern of a test injector.
     */
    @SuppressWarnings("rawtypes")
    private static class SetTestPatternTask implements Runnable {
        private final TestInjector testInjector;
        private final Object testPattern;
        private SetTestPatternTask( final TestInjector _testInjector, final Object _testPattern )
        { testInjector = _testInjector; testPattern = _testPattern; }
        @Override public void run() { testInjector.set( testPattern ); }
    }


    /**
     * Simple task that enables a test injector.
     */
    @SuppressWarnings("rawtypes")
    private static class EnableTask implements Runnable {
        private final TestInjector testInjector;
        private EnableTask( final TestInjector _testInjector ) { testInjector = _testInjector; }
        @Override public void run() { testInjector.enable(); }
    }


    /**
     * Simple task that disables a test injector.
     */
    @SuppressWarnings("rawtypes")
    private static class DisableTask implements Runnable {
        private final TestInjector testInjector;
        private DisableTask( final TestInjector _testInjector ) { testInjector = _testInjector; }
        @Override public void run() { testInjector.disable(); }
    }
}
