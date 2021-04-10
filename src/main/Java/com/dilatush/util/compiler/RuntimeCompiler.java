package com.dilatush.util.compiler;

import com.dilatush.util.Outcome;

import javax.tools.*;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Instances of this class accept Java source code (from files or strings), compile them, and (assuming success) load them into a class loader, which
 * is the actual result of the compilation.  This class loader can then be used to instantiate the classes that were compiled, call methods on those
 * instances, etc.  In the event of compilation failure, the compiler's diagnostic output is returned.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class RuntimeCompiler {


    private static final Outcome.Forge<MemoryClassLoader> OUTCOME = new Outcome.Forge<>();  // factory for Outcomes...

    private final List<JavaFileObject> sources = new ArrayList<>();  // a list of the source "files" to compile...


    /**
     * Compile the Java sources that have been added to this instance, and return an {@link Outcome} with the result of the operation.  If the outcome
     * is ok, then a {@link MemoryClassLoader} instance is returned as the info in the outcome.  This class loader will already be loaded with the
     * compiled classes, and invoking {@link MemoryClassLoader#findClass(String)} will return any particular {@link Class} instance that was compiled.
     *
     * @return the {@link Outcome}, with {@link MemoryClassLoader} upon success
     */
    public Outcome<MemoryClassLoader> compile() {

        // some places to collect diagnostic messages...
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Writer writer = new StringWriter();

        // the actual compilation steps...
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        MemoryFileManager fileManager = new MemoryFileManager( compiler.getStandardFileManager( collector, null, null ) );
        JavaCompiler.CompilationTask compilationTask = compiler.getTask(
                writer,                 // accepts additional diagnostic output from the compiler; usually empty in my experience...
                fileManager,            // the file manager that reads and writes class files...
                collector,              // the per-file diagnostic message collector; this is the main source of diagnostics...
                getCompilerOptions(),   // the command-line options for the compiler...
                null,                   // the binary names of classes to be processed for annotations...
                sources                 // the source code "files" to be compiled...
        );
        boolean success = compilationTask.call();

        // if we were not successful, collect all the diagnostic information that the compiler put out...
        if( !success ) {
            StringBuilder sb = new StringBuilder();
            collector.getDiagnostics().forEach( (diagnostic) -> {
                sb.append( diagnostic.toString().substring( 1 ) );   // skip the first character, which is always a "/" for some reason...
                sb.append( '\n' );
            });
            String additional = writer.toString();
            if( !isEmpty( additional ) ) {
                sb.append( "Additional compiler message(s):\n" );
                sb.append( additional );
            }

            return OUTCOME.notOk( sb.toString() );
        }

        // load the classes we just compiled...
        MemoryClassLoader loader = new MemoryClassLoader();
        fileManager.getClasses().forEach( loader::load );

        return OUTCOME.ok( loader );
    }


    /**
     * Add the given {@link MemoryFileObject} as a Java source code source.
     *
     * @param _source the {@link MemoryFileObject} to add
     */
    public void addSource( final MemoryFileObject _source ) {
        sources.add( _source );
    }


    /**
     * Add the given array of {@link MemoryFileObject} instances as Java source code sources.
     *
     * @param _sources the array of {@link MemoryFileObject} instances to add
     */
    public void addSources( final MemoryFileObject[] _sources ) {
        sources.addAll( Arrays.asList( _sources ) );
    }


    /**
     * Add the given collection of {@link MemoryFileObject} instances as Java source code sources.
     *
     * @param _sources the {@link MemoryFileObject} to add
     */
    public void addSources( final Collection<MemoryFileObject> _sources ) {
        sources.addAll( _sources );
    }


    /**
     * Return {@code true} if the running JVM is enabled to run preview code.
     *
     * @return {@code true} if the running JVM is enabled to run preview code
     */
    private boolean isPreviewEnabled() {

        // see what the compiler options are...
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();

        return arguments.contains( "--enable-preview" );
    }


    /**
     * Return the Java specification version for the running JVM, as a string.
     *
     * @return the Java specification version for the running JVM, as a string
     */
    private String getJavaSpecificationVersion() {
        return System.getProperty( "java.specification.version" );
    }


    /**
     * Return a list of the option to use when running the compiler.
     *
     * @return a list of the option to use when running the compiler
     */
    private List<String> getCompilerOptions() {

        // first get the options we always want...
        List<String> options = new ArrayList<>();
        options.add( "-g" );            // generate all debugging information...
        options.add( "-nowarn" );       // disable warnings...

        // if preview is enabled, add these options...
        if( isPreviewEnabled() ) {
            options.add( "--enable-preview" );               // enable running with preview code...
            options.add( "-source" );                        // specify the source version, required with --enable-preview...
            options.add( getJavaSpecificationVersion() );    // the version part of the -source...
        }

        return options;
    }
}
