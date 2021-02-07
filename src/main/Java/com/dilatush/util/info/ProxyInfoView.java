package com.dilatush.util.info;

/**
 * Instances of this class provide a transparent proxy for another instance of an {@link InfoViewer}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ProxyInfoView<T> implements InfoViewer<T> {


    private final InfoViewerBox<T> infoViewerBox;


    /**
     * Create a new instance of this class that contains the given {@link InfoViewerBox}, which may not be {@code null}.
     *
     * @param _infoViewerBox The instance of {@link InfoViewerBox} to be contained.
     */
    public ProxyInfoView( final InfoViewerBox<T> _infoViewerBox ) {

        // fail fast if some turkey tries to contain a null...
        if( _infoViewerBox == null)
            throw new IllegalArgumentException( "Attempted to contain a null InfoViewerBox" );

        infoViewerBox = _infoViewerBox;
    }


    /**
     * Return the {@link Info} instance that this viewer has access to.
     *
     * @return the {@link Info} instance that this viewer has access to
     */
    @Override
    public Info<T> get() {
        return infoViewerBox.get().get();
    }
}
