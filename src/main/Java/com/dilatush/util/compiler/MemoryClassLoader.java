package com.dilatush.util.compiler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a simple class loader that can load classes from binary data (in a byte array), and find those classes.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MemoryClassLoader extends ClassLoader {

    // where we store the classes we've laoded...
    private final Map<String,Class<?>> classMap = new ConcurrentHashMap<>();


    /**
     * Load the class with the given binary name from the given byte array.
     *
     * @param _className The binary name of the class being loaded.
     * @param _classDef The bytes comprising the class' definition (i.e., the 'object code' resulting from compilation).
     * @return the {@link Class} for the class loaded
     */
    public Class<?> load( final String _className, final byte[] _classDef ) {
        Class<?> result = defineClass( _className, _classDef, 0, _classDef.length );
        classMap.put( _className, result );
        return result;
    }


    /**
     * Find the class with the given binary name.  If the class cannot be found, throws a {@link ClassNotFoundException}.
     *
     * @param _name The binary name of the class to find.
     * @return the {@link Class} for the class found
     * @throws ClassNotFoundException if the class cannot be found
     */
    public Class<?> findClass( final String _name ) throws ClassNotFoundException {
        Class<?> result = classMap.get( _name );
        if( result == null )
            throw new ClassNotFoundException( "Can't find class with binary name: " + _name );
        return result;
    }
}
