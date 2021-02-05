package com.dilatush.util.test;

import com.dilatush.util.From;
import com.dilatush.util.Result;
import com.dilatush.util.console.CommandProcessor;
import com.dilatush.util.console.CommandProcessorConsoleProvider;
import com.dilatush.util.console.ConsoleException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dilatush.util.Strings.leftJustify;
import static com.dilatush.util.Strings.longest;

/**
 * Implements a console provider for the non-interactive test environment, which actually makes it an interactive non-interactive test environment!
 * Somewhere something should explode, methinks.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TestConsoleProvider extends CommandProcessorConsoleProvider {

    private TestManager testManager;

    /**
     * Create a new instance of this class.
     */
    public TestConsoleProvider() {
        super(
                "Inspect and change test scenarios or test enabler properties."
        );
    }


    /**
     * Initialize this instance.
     */
    @Override
    protected void init() {
        testManager = TestManager.getInstance();
        writeLine( "Welcome to the interactive console for the non-interactive test framework!" );
        addCommandProcessor( new ScenariosProcessor()      );
        addCommandProcessor( new EnablersProcessor()       );
        addCommandProcessor( new PropertiesProcessor()     );
        addCommandProcessor( new SelectScenarioProcessor() );
        addCommandProcessor( new CurrentProcessor()        );
        addCommandProcessor( new DisableProcessor()        );
        addCommandProcessor( new SetPropertyProcessor()    );
        addCommandProcessor( new InitProcessor()           );
        finish();
    }


    private void showEnablers( final List<String> _enablers ) {
        writeLine( "Registered enablers:" );
        _enablers.forEach( (enabler) -> writeLine( "  " + enabler ) );
    }


    private void showProperties( final List<String> _properties ) {
        writeLine( "Properties available: " );
        _properties.forEach( (property) -> writeLine( "  " + property ) );
    }


    private void showScenarios( final List<String> _scenarios ) {
        writeLine( "Configured scenarios: " );
        _scenarios.forEach( (scenario) -> writeLine( "  " + scenario ) );
    }


    /*****************************************
     *  C O M M A N D   P R O C E S S O R S
     *****************************************/


    private class InitProcessor extends CommandProcessor {

        protected InitProcessor() {
            super( "init", "initializes an enabler, or all enablers", "init <enabler>*\nIf one or more specific enablers are specified, each of" +
                    " them is initialized.  Otherwise, all enablers are initialized." );
        }


        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {

            // if we got any arguments, then we're initializing specific enablers...
            if( _words.size() > 0 ) {

                // iterate over all our arguments to get our enablers...
                Map<String,ATestEnabler> enablers = new HashMap<>();
                AtomicBoolean fail = new AtomicBoolean( false );
                _words.forEach( (arg) -> {

                    try {
                        // get our enabler...
                        Choice choice = choose( testManager.getEnablers(), arg, "enabler", TestConsoleProvider.this::showEnablers );
                        enablers.put( choice.match, (ATestEnabler) testManager.getEnabler( choice.match ) );
                    }
                    catch( ConsoleException _e ) {
                        // we couldn't match one of the enablers, so we mark the failure
                        // help has already been printed
                        fail.set( true );
                    }
                } );

                // if we failed to match an enabler, just leave...
                if( fail.get() )
                    return;

                // iterate over all the enablers to initialize them, and to tell the user what we did...
                StringBuilder sb = new StringBuilder();
                enablers.forEach( (enablerName, enabler) -> {
                    enabler.init();
                    if( sb.length() > 0 )
                        sb.append( ", " );
                    sb.append( enablerName );
                } );
                writeLine( "Initialized registered enabler" + ((enablers.size() > 1) ? "s" : "") + ": " + sb.toString() );
            }

            // otherwise, we're initializing them all...
            else {
                testManager.init();
                writeLine( "All registered enablers have been initialized." );
            }
        }
    }


    private class SetPropertyProcessor extends CommandProcessor {

        protected SetPropertyProcessor() {
            super( "set", "sets the value of an enabler property",
                    "set <enabler> <property name> <property value>\n" +
                            "Sets the value of the named property of the specified enabler to the given value." );
        }

        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {

            // did we get three arguments?
            if( _words.size() != 3 ) {
                writeLine( "The set property command needs three arguments: <enabler> <property name> <property value>" );
                return;
            }

            try {
                // get our enabler...
                Choice choice = choose( testManager.getEnablers(), _words.get( 0 ), "enabler", TestConsoleProvider.this::showEnablers );
                ATestEnabler enabler = (ATestEnabler) testManager.getEnabler( choice.match );
                String enablerName = choice.match;

                // get our property...
                choice = choose( new ArrayList<>( enabler.properties.keySet() ), _words.get( 1 ),
                        "property", TestConsoleProvider.this::showProperties );
                String propertyName = choice.match;

                // ok, we've got the property - now let's see if we can convert the value to the right class...
                Class<?> klass = enabler.get( propertyName ).getClass();
                String valueStr = _words.get( 2 );
                Result<?> result;
                switch( klass.getName() ) {

                    case "java.lang.String":   result = new Result<>( valueStr, Result.Type.VALID_EXACT, null ); break;
                    case "java.lang.Double":   result = From.doubleFromString( valueStr );                       break;
                    case "java.lang.Float":    result = From.floatFromString( valueStr );                        break;
                    case "java.lang.Long":     result = From.longFromString( valueStr );                         break;
                    case "java.lang.Integer":  result = From.integerFromString( valueStr );                      break;
                    case "java.lang.Short":    result = From.shortFromString( valueStr );                        break;
                    case "java.lang.Byte":     result = From.byteFromString( valueStr );                         break;
                    case "java.lang.Boolean":  result = From.booleanFromString( valueStr );                      break;

                    default: result = new Result<>( "Can't convert unknown type: " + klass.getName() ); break;
                }
                if( !result.valid ) {
                    write( "Could not set property: " );
                    writeLine( result.message );
                    return;
                }
                enabler.properties.put( propertyName, result.value );
                writeLine( "Set property \"" + propertyName + "\" of test enabler \"" + enablerName + "\" to \"" + valueStr + "\"." );
            }
            catch( ConsoleException _e ) {
                // if we get here, it means that either the enabler or the property couldn't be matched...
                // help has already been printed, so we have nothing to do here...
            }
        }
    }


    private class CurrentProcessor extends CommandProcessor {

        protected CurrentProcessor() {
            super( "current", "show the current status of the test manager", "current\nShow the current status of the test manager." );
        }


        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {
            writeLine( "Testing: " + (testManager.isEnabled() ? "enabled" : "disabled" ) );
            writeLine( "Current scenario: " + testManager.getScenario() );
            writeLine( "Configured scenarios:" );
            testManager.getScenarios().forEach( (scenario) -> writeLine( "  " + scenario ) );
            writeLine( "Registered enablers:" );
            testManager.getEnablers().forEach( (enabler) -> writeLine( "  " + enabler ) );
        }
    }


    private class SelectScenarioProcessor extends CommandProcessor {

        protected SelectScenarioProcessor() {
            super( "select", "select the test scenario and enable testing", "select <scenario>\nSelect the test scenario and enable testing." );
        }

        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {

            // did we get one argument?
            if( _words.size() != 1 ) {
                writeLine( "The select command needs one argument: the name of the scenario" );
                return;
            }

            try {
                // get our scenario...
                Choice choice = choose( testManager.getScenarios(), _words.get( 0 ), "scenario", TestConsoleProvider.this::showScenarios );

                // ok, we got one scenario, so set it...
                testManager.enable( choice.match );
                writeLine( "Selected scenario \"" + choice.match + "\" and enabled testing." );
            }
            catch( ConsoleException _e ) {
                // we get here if we couldn't figure out what scenario the user wants
                // help has been printed, so we have nothing to do here...
            }
        }
    }


    private class PropertiesProcessor extends CommandProcessor {

        protected PropertiesProcessor() {
            super( "properties", "lists the properties of an enabler",
                    "properties <enabler>\nLists all the properties of an enabler, with their value." );
        }

        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {

            // did we get one argument?
            if( _words.size() != 1 ) {
                writeLine( "The properties command needs one argument: the name of the enabler" );
                return;
            }

            try {
                // get our enabler...
                Choice choice = choose( testManager.getEnablers(), _words.get( 0 ), "enabler", TestConsoleProvider.this::showEnablers );
                ATestEnabler enabler = (ATestEnabler) testManager.getEnabler( choice.match );
                String enablerName = choice.match;

                // ok, we got one enabler, so show its properties...
                writeLine( "Properties for test enabler: " + enablerName );
                List<String> propertyNames = new ArrayList<>( enabler.properties.keySet() );
                int width = longest( propertyNames ) + 2;
                enabler.properties.forEach( (name,value) -> writeLine( "  " + leftJustify( name, width ) + value.toString() ) );
            }
            catch( ConsoleException _e ) {
                // we get here if we couldn't figure out which enabler the user wants
                // help has already been printed, so we have nothing to do here...
            }
        }
    }


    private class EnablersProcessor extends CommandProcessor {

        protected EnablersProcessor() {
            super( "enablers", "lists all the registered enablers", "enablers\nLists all the registered enablers." );
        }


        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {
            writeLine( "Registered enablers:" );
            testManager.getEnablers().forEach( (enabler) -> writeLine( "  " + enabler ) );
        }
    }


    private class ScenariosProcessor extends CommandProcessor {

        protected ScenariosProcessor() {
            super( "scenarios", "lists all the configured scenarios", "scenarios\nLists all the configured scenarios." );
        }


        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {
            writeLine( "Configured scenarios:" );
            testManager.getScenarios().forEach( (scenario) -> writeLine( "  " + scenario ) );
        }
    }


    private class DisableProcessor extends CommandProcessor {

        protected DisableProcessor() {
            super( "disable", "disables all testing", "disable\nDisables all testing." );
        }


        @Override
        protected void onCommandLine( final String _line, final List<String> _words ) {
            writeLine( "Disabling all testing." );
            testManager.disable();
        }
    }
}
