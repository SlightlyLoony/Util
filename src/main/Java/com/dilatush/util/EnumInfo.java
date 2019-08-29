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


    public EnumInfo( final int _index, final String _id, final String _description ) {
        index = _index;
        id = _id;
        description = _description;
    }
}
