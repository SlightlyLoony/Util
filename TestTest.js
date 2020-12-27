/*
 *  Test configuration file for com.dilatush.util.test.TestTest.java
 */

// create constructor functions for Java ArrayList and HashMap...
var HashMap   = Java.type( "java.util.HashMap" );
var ArrayList = Java.type( "java.util.ArrayList" );

// returns a HashMap with the properties of the given JavaScript object
function makeMap( obj ) {
    var map = new HashMap();
    for( prop in obj ) {
        if( obj.hasOwnProperty( prop ) )
            map.put( prop, obj[prop] );
    }
    return map;
}

// returns an ArrayList with the values of the given JavaScript array
function makeList( array ) {
    var list = new ArrayList();
    array.forEach( function( item ) {list.add( item ); });
    return list;
}


// returns a Java TestEnabler of the given name, with the properties in the given JavaScript object
function makeEnabler( name, properties ) {

    // if name contains a period, assume it's a fully qualified type name; otherwise assume it's "com.dilatush.util.test.???TestEnabler",
    // where "???" is this name...
    var typeName = (name.indexOf( "." ) >= 0 )? name : "com.dilatush.util.test." + name + "TestEnabler";

    var props = makeMap( properties );

    return new (Java.type( typeName ) )( props );
}


// returns a Java CompositeTestEnabler with the properties in the given JavaScript object, and containing the Java test enablers in the given
// JavaScript array
function makeComposite( properties, enablers ) {
    var props = makeMap( properties );
    var enablerList = makeList( enablers );
    return new (Java.type( "com.dilatush.util.test.CompositeTestEnabler" ))( props, enablerList );
}


function makeJavaScript( properties, script ) {
    var props = makeMap( properties );
    return new (Java.type( "com.dilatush.util.test.JavaScriptTestEnabler" ) )( props, script );
}


config.enabled = true;
config.scenario = "s1";


config.scenarios = makeMap( {
    s1: {
        te1: makeComposite( { "offset": 5, "scale": 4.55}, [
            makeEnabler( "Periodic",  { "_phase1_": 3000, "_phase2_": 3000, "_startAs_": true } ),
            makeEnabler( "Counted", { "_maxCount_": 2, "_startAs_": true } )
        ]),
        te2: makeJavaScript( { "p1": 7, "p2": 3 },
            "var properties = null;            \n\
             var count = 0;                    \n\
             var set = function( props ) {     \n\
                 properties = props;           \n\
             }                                 \n\
             var init = function() {           \n\
                 count = 0;                    \n\
             }                                 \n\
             var enabled = function() {        \n\
                 var p1 = properties['p1'];    \n\
                 var p2 = properties['p2'];    \n\
                 count++;                      \n\
                 return (count % p1 == 0)      \n\
                    && (count % p2 == 0);      \n\
             }" )
    }
})


config;