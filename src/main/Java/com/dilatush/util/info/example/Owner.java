package com.dilatush.util.info.example;

import com.dilatush.util.info.InfoViewer;
import com.dilatush.util.info.InfoViewerBox;
import com.dilatush.util.info.ProxyInfoView;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Owner {

    private Source source;
    private final InfoViewerBox<Double> infoViewerBox;
    public  final InfoViewer<Double> infoViewer;


    public Owner() {
        infoViewerBox = new InfoViewerBox<>();                          // make a place where we can update the information viewer, once we instantiate the source...
        infoViewer    = new ProxyInfoView<>( infoViewerBox );           // make the information publicly available...
    }


    public void run() {
        source = new Source();                                          // instantiate our data source, privately...
        infoViewerBox.set( source.readOnlySource );               // update our publicly available information viewer to use our source...
        System.out.println( "Owner sees " + infoViewer.get().info );
    }

    public void tick() {
        source.tick();
        System.out.println( "Owner sees " + infoViewer.get().info );
    }
}
