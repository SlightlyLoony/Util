package com.dilatush.util.info;

/**
 * Instances of this class contain a readable and writable instance of {@link InfoViewer}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class InfoViewerBox<T> {

    // cannot be null...
    private volatile InfoViewer<T> infoViewer = new InfoView<>( new InfoBox<>() );


    /**
     * Return the contained {@link InfoViewer}.  This method will never return {@code null}.
     *
     * @return the contained {@link InfoViewer}
     */
    public InfoViewer<T> get() {
        return infoViewer;
    }


    /**
     * Sets the contained {@link InfoViewer} to the given {@link InfoViewer}.
     *
     * @param _infoViewer The {@link InfoViewer} to be contained.
     */
    public void set( final InfoViewer<T> _infoViewer ) {

        // fail fast if some rock tries to set a null...
        if( _infoViewer == null )
            throw new IllegalArgumentException( "Attempt made to set a null InfoViewer instance" );

        infoViewer = _infoViewer;
    }
}
