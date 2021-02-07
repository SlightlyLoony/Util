package com.dilatush.util.info;

/**
 * Instances of this class contain a readable and writable instance of {@link Info}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class InfoBox<T> {

    // cannot be null...
    private volatile Info<T> info = new Info<>( null );


    /**
     * Return the contained {@link Info} instance.  This method will never return {@code null}.
     *
     * @return the contained {@link Info} instance
     */
    public Info<T> get() {
        return info;
    }


    /**
     * Set the contained {@link Info} instance to the given {@link Info} instance, which may not be {@code null}.
     *
     * @param _info The {@link Info} instance to be contained.
     */
    public void set( final Info<T> _info ) {

        // fail fast if some rock tries to set a null...
        if( _info == null )
            throw new IllegalArgumentException( "Attempt made to set a null Info instance" );

        info = _info;
    }
}
