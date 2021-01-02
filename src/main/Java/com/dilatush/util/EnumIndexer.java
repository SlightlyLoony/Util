package com.dilatush.util;

import java.util.HashMap;

import static com.dilatush.util.General.isNull;

/**
 * Provides a simple way to extend Java's standard enum construct by providing arbitrary indexes, identifying strings, and descriptive information
 * per value, and also methods to retrieve an enum via the index or identifying string.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EnumIndexer<E> {


    private final HashMap< Integer, E        > byIndex = new HashMap<>();
    private final HashMap< String,  E        > byID    = new HashMap<>();
    private final HashMap< E,       EnumInfo > infoMap = new HashMap<>();


    /**
     * Add a new enum to the indexer.  Generally this method would be invoked from the enum's constructor, and the parameters would be taken from
     * the enum's list of values.  For instance:<pre>{@code
     *     NONE         ( 0, "NONE",          "normal value for a synchronized clock"                  ),
     *     ADD_SECOND   ( 1, "ADD_SECOND",    "insert a leap second after 23:59:59 of the current day" ),
     *     DELETE_SECOND( 2, "DELETE_SECOND", "delete the second 23:59:59 of the current day"          ),
     *     ALARM        ( 3, "ALARM",         "the clock has never been synchronized"                  );
     *
     *     private static final EnumIndexer<LeapSecondMode> indexer = new EnumIndexer<>();
     *
     *
     *     LeapSecondMode( final int _index, final String _id, final String _description ) {
     *         add( _index, _id, _description );
     *     }
     *
     *
     *     private void add( final int _index, final String _id, final String _description ) {
     *         indexer.add( this, _index, _id, _description );
     *     }
     * }</pre>
     * Note that the indexer may only be invoked from a method, not a constructor, which is why the <code>add</code> method is needed.
     *
     * @param _enum the enum being added to the indexer
     * @param _index the index of the enum being added (must be unique per enum)
     * @param _id the identifying string of the enum being added (must be unique per enum)
     * @param _description a descriptive string for the enum being added (does not have to be unique)
     */
    public final void add( final E _enum, final int _index, final String _id, final String _description ) {

        // a bit of sanity checking...
        if( isNull( _enum, _description, _id ) )
            throw new IllegalArgumentException( "Value missing when adding an enum to EnumIndexer" );
        if( byIndex.containsKey( _index ) )
            throw new IllegalArgumentException( "Duplicate index when adding an enum to EnumIndexer" );
        if( byID.containsKey( _id ) )
            throw new IllegalArgumentException( "Duplicate identifying string when adding an enum to EnumIndexer" );

        byIndex.put( _index, _enum );
        byID.put( _id, _enum );
        infoMap.put( _enum, new EnumInfo( _index, _id, _description ) );
    }


    /**
     * Returns the enum that has the given identifying string, or <code>null</code> if there is no such enum.
     *
     * @param _id the identifying string to look for
     * @return the enum with the given identifying string
     */
    public final E fromID( final String _id ) {
        return byID.get( _id );
    }


    /**
     * Returns the enum that has the given index, or <code>null</code> if there is no such enum.
     *
     * @param _index the index to look for
     * @return the enum with the given index
     */
    public final E fromIndex( final int _index ) {
        return byIndex.get( _index );
    }


    /**
     * Returns information (id, index, and detail) associated with the given enum, or <code>null</code> if there is no information available for
     * the given enum.
     *
     * @param _enum the enum to get information for
     * @return the information associated with the given enum
     */
    public final EnumInfo info( final E _enum ) {
        return infoMap.get( _enum );
    }
}
