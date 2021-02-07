package com.dilatush.util.info;

/**
 * Instances of this class can read an instance of {@link Info} that is contained in an instance of {@link InfoBox}, but cannot write to it.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class InfoView<T> implements InfoViewer<T> {

    private final InfoBox<T> infoBox;


    /**
     * Create a new instance of this class containing the given instance of {@link InfoBox}, which may not be {@code null}.
     *
     * @param _infoBox The {@link InfoBox} to be contained.
     */
    public InfoView( final InfoBox<T> _infoBox ) {

        // fail fast if some turkey tries to contain a null...
        if( _infoBox == null)
            throw new IllegalArgumentException( "Attempted to contain a null InfoBox" );

        infoBox = _infoBox;
    }


    /**
     * Return the {@link Info} instance that this viewer has access to.
     *
     * @return the {@link Info} instance that this viewer has access to
     */
    @Override
    public Info<T> get() {
        return infoBox.get();
    }
}
