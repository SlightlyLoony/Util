package com.dilatush.util;

import javax.tools.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class RuntimeCompiler {

    public static void main( final String[] _args ) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        long start = System.currentTimeMillis();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager( collector, null, null );

        // compile task...
        File[] task = new File[] { new File( "src/main/Java/com/dilatush/util/info/IsAvailable.java" ), new File( "script/Test.java" ) };
        Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromFiles( Arrays.asList( task ) );
        JavaCompiler.CompilationTask compilationTask = compiler.getTask( null, fileManager, collector, null, null, compilationUnits1 );
        // TODO: control where the class file gets written...
        boolean success = compilationTask.call();

        // load what we just compiled...
        URL url = new File( "script" ).toURI().toURL();
        URLClassLoader loader = URLClassLoader.newInstance( new URL[]{ url } );
        Class<?> klass = Class.forName( "Test", true, loader );
        Object what = klass.getDeclaredConstructor().newInstance();

        // now run it...
        Method method = klass.getMethod( "run" );
        method.invoke( what );

        long duration = System.currentTimeMillis() - start;
        System.out.println( "Runtime: " + duration );

        new Object().hashCode();
    }

}
