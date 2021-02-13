package com.dilatush.util.info.example;

import com.dilatush.util.info.Info;
import com.dilatush.util.info.InfoView;

import java.util.function.Consumer;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Source {

    public  final Info<Double> readOnlySource;

    private Consumer<Double>   setter;


    public Source() {

        // create the public, read-only data source...
        readOnlySource = new InfoView<>( (gadget) -> setter = gadget, false );

        // use our private setter to publish some actual data...
        setter.accept( 12.345 );
    }

    public void tick() {
        setter.accept( 54.321 );    // update our published information...
    }
}
