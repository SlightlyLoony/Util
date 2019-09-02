package com.dilatush.util;

/**
 * A simple POJO that contains information about an enum.
 *
 * Instances of this class are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EnumInfo {

    public final int index;
    public final String id;
    public final String description;


    /**
     * Creates a new instance of this class with the given index, identifying string, and descriptive string.
     *
     * @param _index the index
     * @param _id the identifying string
     * @param _description the descriptive string
     */
    public EnumInfo( final int _index, final String _id, final String _description ) {
        index = _index;
        id = _id;
        description = _description;
    }
}
