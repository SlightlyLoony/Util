package com.dilatush.util.info.example;

import com.dilatush.util.info.Info;
import com.dilatush.util.info.InfoBox;
import com.dilatush.util.info.InfoView;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Source {

    private final InfoBox<Double>  source;
    public  final InfoView<Double> readOnlySource;


    public Source() {
        source = new InfoBox<>();                   // initializing the place where the source can update the actual information...
        source.set( new Info<>( 12.345 ) );     // publish some information..
        readOnlySource = new InfoView<>( source );  // make it publicly available to read, but not to write...
    }

    public void tick() {
        source.set( new Info<>( 54.321 ) );     // update our published information...
    }
}
