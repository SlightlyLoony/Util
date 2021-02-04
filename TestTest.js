/*
 *  Test configuration file for com.dilatush.util.test.TestTest.java
 */
load( 'TestHelpers.js' );


/*
 * The init( config ) function that will be called by AConfig.init().
 */
function init( config ) {

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
    config.scenarios = makeMap( {
        s1: {
            te1: makeComposite( { "offset": 5, "scale": 4.55 }, [
                makeEnabler( "Periodic", { "_phase1_": 3000, "_phase2_": 3000, "_startAs_": true } ),
                makeEnabler( "Counted", { "_maxCount_": 2, "_startAs_": true } )
            ] ),
            te2: makeJavaScript( { "p1": 7, "p2": 3, "count": 17 }, 'load( "SampleJavaScriptTestEnablerScript.js" )' )
        },
        s2: {
            te1: makeComposite( { "offset": 4, "scale": 5.55 }, [
                makeEnabler( "Periodic", { "_phase1_": 4000, "_phase2_": 4000, "_startAs_": true } ),
                makeEnabler( "Counted", { "_maxCount_": 3, "_startAs_": true } )
            ] ),
            te2: makeJavaScript( { "p1": 7, "p2": 3, "count": 17 }, 'load( "SampleJavaScriptTestEnablerScript.js" )' )
        }
    } );
}