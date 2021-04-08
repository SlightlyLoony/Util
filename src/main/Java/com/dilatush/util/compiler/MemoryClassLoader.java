package com.dilatush.util.compiler;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MemoryClassLoader extends ClassLoader {

    public Class load( final String _className, final byte[] _classDef ) {
        return defineClass( _className, _classDef, 0, _classDef.length );
    }
}
