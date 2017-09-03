package com.dilatush.util;

import java.util.List;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Collections {

    /**
     * Returns true if the given {@link List} instance is non-null, and the given index is in range for it.  To return true, the given {@link List}
     * must exist, and the given index must be zero or greater, but less than the current size of the given {@link List}.
     *
     * @param _list
     *      the {@link List} to check range on.
     * @param _index
     *      the index to check.
     * @return
     *      true if the given {@link List} instance is non-null, and the given index is in range for it.
     */
    public static boolean inRange( final List _list, final int _index ) {
        return (_list != null) && ((_index >= 0) && (_index < _list.size()));
    }
}
