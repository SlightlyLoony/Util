package com.dilatush.util.compiler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MemoryClassLoader extends ClassLoader {

    private final Map<String,Class<?>> classMap = new ConcurrentHashMap<>();


    public Class<?> load( final String _className, final byte[] _classDef ) {
        Class<?> result = defineClass( _className, _classDef, 0, _classDef.length );
        classMap.put( _className, result );
        return result;
    }


    public Class<?> findClass( final String _name ) throws ClassNotFoundException {
        Class<?> result = classMap.get( _name );
        if( result == null )
            throw new ClassNotFoundException( "Can't find class with binary name: " + _name );
        return result;
    }
}
