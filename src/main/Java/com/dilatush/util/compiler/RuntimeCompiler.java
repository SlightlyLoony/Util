package com.dilatush.util.compiler;

import com.dilatush.test.Configurator;

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


    private boolean isPreviewEnabled() {

        // see what the compiler options are...
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();

        return arguments.contains( "--enable-preview" );
    }


    private String getJavaSpecificationVersion() {
        return System.getProperty( "java.specification.version" );
    }


    private List<String> getCompilerOptions() {

        // first get the options we always want...
        List<String> options = new ArrayList<>();
        options.add( "-g" );
        options.add( "-nowarn" );

        // if preview is enabled, add these options...
        if( isPreviewEnabled() ) {
            options.add( "--enable-preview" );
            options.add( "-source" );
            options.add( getJavaSpecificationVersion() );
        }

        return options;
    }

    public static void main( final String[] _args ) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        // see what the compiler options are...
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();

        Properties props = System.getProperties();

        com.dilatush.test.Config config = new com.dilatush.test.Config();

        long start = System.currentTimeMillis();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager( collector, null, null );
        MemoryFileManager fileManager = new MemoryFileManager( stdFileManager );

        // compile task...
        List<JavaFileObject> sources = new ArrayList<>();
        sources.add( new MemoryFileObject( "Test", new File( "script/Test.java" ) ) );
        sources.add( new MemoryFileObject( "Bogus", new File( "script/Bogus.java" ) ) );

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
        byte[] bogus = fileManager.getClasses().get( "Bogus" );

        // load what we just compiled...
        MemoryClassLoader mcl = new MemoryClassLoader();
        Class<?> klass = mcl.load( "Test", test );
        mcl.load( "Bogus", bogus );
        Object what = klass.getDeclaredConstructor().newInstance();
        Configurator configurator = (Configurator) what;

        Class<?> kk = mcl.findClass( "Bogus" );

        // now run it...
        configurator.config( config );
//        Method method = klass.getMethod( "config", Object.class );
//        method.invoke( what, config );

        long duration = System.currentTimeMillis() - start;
        System.out.println( "Runtime: " + duration );

        new Object().hashCode();
    }


}
