/*
 * Script for a sample JavaScript test enabler.
 */

var properties = null;  // where the properties will live after being "set"...

var count = 0;  // the default value for our count variable...


// The mandatory "set()" function that saves the test enabler properties for use within the script...
function set( props ) {
    properties = props;
}


// The mandatory "init()" function that in our case just resets the counter...
function init() {
    count = 0;
}


// The mandatory "enabled()" function that in our case is computed...
function enabled() {

    // get the value of our two properties...
    var p1 = properties['p1'];
    var p2 = properties['p2'];

    // update the counter...
    count++;

    // now the fun part - the computed return value...
    return (count % p1) <= (count % p2);
}
