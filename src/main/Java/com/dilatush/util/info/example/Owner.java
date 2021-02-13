package com.dilatush.util.info.example;

import com.dilatush.util.info.Info;
import com.dilatush.util.info.InfoView;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Owner {

    public  final Info<Double> info;

    private final Source source;


    public Owner() {

        source = new Source();                                  // instantiate our data source, privately...
        info = new InfoView<Double>( source.readOnlySource );   // create our proxy for the source's data...
    }


    public void run() {
        System.out.println( "Owner sees " + info.getInfo() );
    }

    public void tick() {
        source.tick();
        System.out.println( "Owner sees " + info.getInfo() );
    }
}
