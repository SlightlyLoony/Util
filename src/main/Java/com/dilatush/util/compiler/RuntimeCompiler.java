package com.dilatush.util.compiler;

import javax.tools.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class RuntimeCompiler {

    // TODO: come up with a way to do includes for secrets (file with list of named secrets, replaced)

    public static void main( final String[] _args ) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        // see what the compiler options are...
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();

        com.dilatush.test.Config config = new com.dilatush.test.Config();

        long start = System.currentTimeMillis();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager( collector, null, null );
        MemoryFileManager fileManager = new MemoryFileManager( stdFileManager );

        // compile task...
        List<JavaFileObject> sources = new ArrayList<>();
        sources.add( new MemoryFileObject( "Test", new File( "script/Test.java" ) ) );
        List<String> options = new ArrayList<>();
        options.add( "-g" );
        options.add( "-nowarn" );
        options.add( "--enable-preview" );
        options.add( "-source" );
        options.add( "15" );

        Writer writer = new StringWriter();

        JavaCompiler.CompilationTask compilationTask = compiler.getTask( writer, fileManager, collector, options, null, sources );
        boolean success = compilationTask.call();
        if( !success ) {
            collector.getDiagnostics().forEach( (diagnostic) -> {
                System.out.println( diagnostic.toString() );
            });
        }

        // get the class binary...
        byte[] test = fileManager.getClasses().get( "Test" );

        // load what we just compiled...
        MemoryClassLoader mcl = new MemoryClassLoader();
        Class<?> klass = mcl.load( "Test", test );
        Object what = klass.getDeclaredConstructor().newInstance();

        // now run it...
        Method method = klass.getMethod( "config", config.getClass() );
        method.invoke( what, config );

        long duration = System.currentTimeMillis() - start;
        System.out.println( "Runtime: " + duration );

        new Object().hashCode();
    }


}
