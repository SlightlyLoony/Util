import com.dilatush.util.test.*;
import com.dilatush.util.config.Configurator;
import com.dilatush.util.config.AConfig;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class TestTest implements Configurator {

    public void config( final AConfig _config ) {

        TestManager.Config config = (TestManager.Config) _config;

        /*
         * Set to true to enable the testing framework, false otherwise.  Defaults to false.
         */
        config.enabled = true;


        /*
         * Sets the name of the default test scenario, which should of course be configured in the scenarios below.  Defaults to none.
         */
        config.scenario = "s1";


        /*
         * A map of test scenarios.  Each scenario is itself a map of test enabler names (which must match the names registered by test code) to test enabler
         * instances.
         */
        config.scenarios = new HashMap<>();
        Map<String,TestEnabler> s1 = new HashMap<>();
        config.scenarios.put( "s1", s1 );
        Map<String,TestEnabler> s2 = new HashMap<>();
        config.scenarios.put( "s2", s2 );

        // s1 test enablers...
        TestEnabler te1a = new PeriodicTestEnabler( Map.of( "_phase1_", 3000, "_phase2_", 3000, "_startAs_", true ) );
        TestEnabler te1b = new CountedTestEnabler( Map.of( "_maxCount_", 2, "_startAs_", true ) );
        TestEnabler te1 = new CompositeTestEnabler( Map.of( "offset", 5, "scale", 4.55 ), Arrays.asList( te1a, te1b ) );
        TestEnabler te2 = new DelayTestEnabler( Map.of( "_delay_", 2500, "_startAs_", false ) );
        s1.put( "te1", te1 );
        s1.put( "te2", te2 );

        // s2 test enablers...
        te1a = new PeriodicTestEnabler( Map.of( "_phase1_", 5000, "_phase2_", 7000, "_startAs_", true ) );
        te1b = new CountedTestEnabler( Map.of( "_maxCount_", 2, "_startAs_", true ) );
        te1 = new CompositeTestEnabler( Map.of( "offset", 6, "scale", 48.55 ), Arrays.asList( te1a, te1b ) );
        te2 = new DelayTestEnabler( Map.of( "_delay_", 1212, "_startAs_", false ) );
        s2.put( "te1", te1 );
        s2.put( "te2", te2 );
    }
}