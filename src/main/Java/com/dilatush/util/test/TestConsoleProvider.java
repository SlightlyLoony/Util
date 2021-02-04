package com.dilatush.util.test;

import com.dilatush.util.From;
import com.dilatush.util.Result;
import com.dilatush.util.console.CommandProcessor;
import com.dilatush.util.console.CommandProcessorConsoleProvider;

import java.util.ArrayList;
import java.util.List;

import static com.dilatush.util.Strings.*;

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
        addCommandProcessor( new ScenariosProcessor() );
        addCommandProcessor( new EnablersProcessor() );
        addCommandProcessor( new PropertiesProcessor() );
        addCommandProcessor( new SelectScenarioProcessor() );
        addCommandProcessor( new CurrentProcessor() );
        addCommandProcessor( new DisableProcessor() );
        addCommandProcessor( new SetPropertyProcessor() );
        finish();
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

            // do we know this enabler?
            String enablerName = _words.get( 0 );
            List<String> enablers = testManager.getEnablers();
            int index = chooseEnabler( enablers, enablerName );
            if( index < 0 )
                return;

            // do we know this property?
            ATestEnabler enabler = (ATestEnabler) testManager.getEnabler( enablers.get( index ) );
            List<String> propertyNames = new ArrayList<>( enabler.properties.keySet() );
            int prop = chooseFrom( propertyNames, _words.get( 1 ), true );
            if( prop == NO_KEY_MATCH_FOR_CHOOSER ) {
                writeLine( "There are no properties for test enabler \"" + enablers.get( index ) + "\" named \"" + _words.get( 1 ) + "\"" );
                return;
            }
            if( index == AMBIGUOUS_KEY_MATCH_FOR_CHOOSER ) {
                writeLine( "There are multiple properties of test enabler \"" + enablers.get( index )
                        + "\" with names that begin with: " + _words.get( 1 ) );
                writeLine( "Properties:" );
                int width = longest( propertyNames ) + 2;
                enabler.properties.forEach( (name,value) -> writeLine( "  " + leftJustify( name, width ) + value.toString() ) );
                return;
            }

            // ok, we've got the property - now let's see if we can convert the value to the right class...
            String property = propertyNames.get( prop );
            Class<?> klass = enabler.get( property ).getClass();
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
            enabler.properties.put( property, result.value );
            writeLine( "Set property \"" + property + "\" of test enabler \"" + enablerName + "\" to \"" + valueStr + "\"." );
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

            // do we know this scenario?
            String scenarioName = _words.get( 0 );
            List<String> scenarios = testManager.getScenarios();
            int index = chooseFrom( scenarios, scenarioName, true );
            if( index == NO_KEY_MATCH_FOR_CHOOSER) {
                writeLine( "There are no test scenarios named: " + scenarioName );
                return;
            }
            if( index == AMBIGUOUS_KEY_MATCH_FOR_CHOOSER ) {
                writeLine( "There are multiple test scenarios with names that begin with: " + scenarioName );
                writeLine( "Configured scenarios:" );
                scenarios.forEach( (scenario) -> writeLine( "  " + scenario ) );
                return;
            }

            // ok, we got one scenario, so set it...
            testManager.enable( scenarios.get( index ) );
            writeLine( "Selected scenario \"" + scenarios.get( index ) + "\" and enabled testing." );
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

            // do we know this enabler?
            String enablerName = _words.get( 0 );
            List<String> enablers = testManager.getEnablers();
            int index = chooseEnabler( enablers, enablerName );
            if( index < 0 )
                return;

            // ok, we got one enabler, so show its properties...
            ATestEnabler enabler = (ATestEnabler) testManager.getEnabler( enablers.get( index ) );
            writeLine( "Properties for test enabler: " + enablerName );
            List<String> propertyNames = new ArrayList<>( enabler.properties.keySet() );
            int width = longest( propertyNames ) + 2;
            enabler.properties.forEach( (name,value) -> writeLine( "  " + leftJustify( name, width ) + value.toString() ) );
        }
    }


    private int chooseEnabler( final List<String> _enablers, final String _enablerName ) {
        int index = chooseFrom( _enablers, _enablerName, true );
        if( index == NO_KEY_MATCH_FOR_CHOOSER) {
            writeLine( "There are no enablers named: " + _enablerName );
            return index;
        }
        if( index == AMBIGUOUS_KEY_MATCH_FOR_CHOOSER ) {
            writeLine( "There are more than one registered enablers with names that begin with: " + _enablerName );
            writeLine( "Registered enablers:" );
            _enablers.forEach( (enabler) -> writeLine( "  " + enabler ) );
            return index;
        }
        return index;
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
