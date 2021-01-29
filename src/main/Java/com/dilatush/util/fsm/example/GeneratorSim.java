package com.dilatush.util.fsm.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a console simulator for the generator control panel.  Note that if you run this from within an IDE, you will likely lose keystrokes
 * as the IDE will poll for them.  It still works, but it may take you multiple tries.  When it DOES work, you'll see the feedback from the echo
 * output.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class GeneratorSim implements Generator {


    private Consumer<Event> listener;


    /**
     * Run this generator simulator.
     */
    public void run() {

        BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );

        help();

        boolean done = false;
        while( !done ) {

            String line;
            try {
                line = br.readLine();
            }
            catch( IOException _e ) {
                line = "q";
            }

            if( isEmpty( line ) )
                continue;

            char c = line.toUpperCase().charAt( 0 );
            switch( c ) {

                case 'Q':
                    done = true;
                    outGen( "Quit" );
                    break;

                case 'N':
                    outGen( "On" );
                    listener.accept( Event.ON );
                    break;

                case 'F':
                    outGen( "Off" );
                    listener.accept( Event.OFF );
                    break;

                case 'A':
                    outGen( "Auto" );
                    listener.accept( Event.AUTO );
                    break;

                case 'D':
                    outGen( "Grid Down" );
                    listener.accept( Event.DOWN );
                    break;

                case 'U':
                    outGen( "Grid Up" );
                    listener.accept( Event.UP );
                    break;

                case 'X':
                    outGen( "Fixed" );
                    listener.accept( Event.FIXED );
                    break;

                default:
                    help();
                    break;
            }
        }
    }


    private void help() {
        out( "Generator simulator" );
        out( "Type a single letter (case insensitive) and enter for any command, even while log is scrolling" );
        out( "Q to quit" );
        out( "N for generator 'ON'" );
        out( "F for generator 'OFF'" );
        out( "A for generator 'AUTO'" );
        out( "D for grid down" );
        out( "U for grid up" );
        out( "X for fixed" );
    }


    private void out( final String _msg ) {
        System.out.println( _msg );
    }


    /**
     * Turn the engine running indicator on or off.
     *
     * @param _mode the mode for the engine running indicator
     */
    @Override
    public void runningIndicator( final Mode _mode ) {
        outGen( "running indicator: " + _mode );
    }


    /**
     * Turn the generating indicator on or off.
     *
     * @param _mode the mode for the generating indicator
     */
    @Override
    public void generatingIndicator( final Mode _mode ) {
        outGen( "generating indicator: " + _mode );
    }


    /**
     * Turn the failure indicator on or off.
     *
     * @param _mode the mode for the failure indicator
     */
    @Override
    public void failureIndicator( final Mode _mode ) {
        outGen( "failure indicator: " + _mode );
    }


    /**
     * Turn the automatic transfer switch (ATS) on (generator connected to load) or off (grid connected to load).
     *
     * @param _mode the mode for the ATS
     */
    @Override
    public void ats( final Mode _mode ) {
        outGen( "ATS: " + ((_mode == Mode.ON) ? "load on generator" : "load on grid" ));
    }


    /**
     * Set the listener for events from the generator.
     *
     * @param _listener the event listener
     */
    @Override
    public void setListener( final Consumer<Event> _listener ) {
        listener = _listener;
    }


    private final DateTimeFormatter ldtf = DateTimeFormatter.ofPattern( "HH:mm:ss.SSS " );

    private void outGen( final String _msg ) {
        System.out.println( ldtf.format( ZonedDateTime.now() ) + "Generator: " +  _msg );
    }
}
