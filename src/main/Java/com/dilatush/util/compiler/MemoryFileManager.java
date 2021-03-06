package com.dilatush.util.compiler;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of {@link JavaFileManager} that extends {@link StandardJavaFileManager} to support compiling classes to binary (byte arrays)
 * held in memory, and not stored on the file system as they ordinarily would be.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class MemoryFileManager implements JavaFileManager {

    // the StandardJavaFileManager instance we're extending...
    private final StandardJavaFileManager standardJavaFileManager;

    // where we keep the binary image of the compiled classes...
    private final Map<String, ByteArrayOutputStream> classes = new ConcurrentHashMap<>();


    /**
     * Create a new instance of this class that extends the given {@link StandardJavaFileManager}.
     *
     * @param _standardJavaFileManager The {@link StandardJavaFileManager} instance to extend.
     */
    public MemoryFileManager( final StandardJavaFileManager _standardJavaFileManager ) {
        standardJavaFileManager = _standardJavaFileManager;
    }


    /**
     * Return the map of compiled classes (binary class name -> binary class code).
     *
     * @return the map of compiled classes (binary class name -> binary class code)
     */
    public Map<String, byte[]> getClasses() {

        Map<String, byte[]> result = new HashMap<>();
        classes.forEach( ( binaryName, classCode ) -> result.put( binaryName, classCode.toByteArray() ) );
        return result;
    }


    /**
     * Returns a file object for output representing the specified class of the specified kind in the given package-oriented location.
     *
     * Optionally, this file manager might consider the sibling as a hint for where to place the output. The exact semantics of this hint is
     * unspecified. The JDK compiler, javac, for example, will place class files in the same directories as originating source files unless a class
     * file output directory is provided. To facilitate this behavior, javac might provide the originating source file as sibling when calling this
     * method.
     *
     * @param location a package-oriented location
     * @param className the binary name of a class
     * @param kind the kind of file (SOURCE or CLASS)
     * @param sibling a file object to be used as a hint for placement; may be {@code null}
     * @return a file object for output
     * @throws IOException if an I/O error has occurred, or if close() has been called and this file manager cannot be reopened
     */
    @Override
    public JavaFileObject getJavaFileForOutput( final Location location, final String className, final JavaFileObject.Kind kind, final FileObject sibling ) throws IOException {

        // if we're getting a file to output a class, return an output stream to collect the results in memory instead, mapped to the class name...
        if( kind == JavaFileObject.Kind.CLASS ) {

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            classes.put( className, os );

            return new SimpleJavaFileObject( URI.create( className ), kind ) {
                public OutputStream openOutputStream() {
                    return os;
                }
            };

        }
        return standardJavaFileManager.getJavaFileForOutput( location, className, kind, sibling );
    }


    /* I thought I'd have to modify this method, but so far I haven't discovered a need... */
    @Override
    public JavaFileObject getJavaFileForInput( final Location location, final String className, final JavaFileObject.Kind kind ) throws IOException {

        /*
            In my testing, I only ever saw this fetching class files for system modules.  If I ever see missing class files, though,
            this seems like a likely place to begin looking.
         */
        return standardJavaFileManager.getJavaFileForInput( location, className, kind );
    }


    /* Everything after this comment is a straight delegated method... */

    @Override
    public boolean isSameFile( final FileObject a, final FileObject b ) {
        return standardJavaFileManager.isSameFile( a, b );
    }


    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles( final Iterable<? extends File> files ) {
        return standardJavaFileManager.getJavaFileObjectsFromFiles( files );
    }


    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths( final Collection<? extends Path> paths ) {
        return standardJavaFileManager.getJavaFileObjectsFromPaths( paths );
    }


    @Deprecated(since = "13")
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths( final Iterable<? extends Path> paths ) {
        return standardJavaFileManager.getJavaFileObjectsFromPaths( paths );
    }


    public Iterable<? extends JavaFileObject> getJavaFileObjects( final File... files ) {
        return standardJavaFileManager.getJavaFileObjects( files );
    }


    public Iterable<? extends JavaFileObject> getJavaFileObjects( final Path... paths ) {
        return standardJavaFileManager.getJavaFileObjects( paths );
    }


    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings( final Iterable<String> names ) {
        return standardJavaFileManager.getJavaFileObjectsFromStrings( names );
    }


    public Iterable<? extends JavaFileObject> getJavaFileObjects( final String... names ) {
        return standardJavaFileManager.getJavaFileObjects( names );
    }


    public void setLocation( final Location location, final Iterable<? extends File> files ) throws IOException {
        standardJavaFileManager.setLocation( location, files );
    }


    public void setLocationFromPaths( final Location location, final Collection<? extends Path> paths ) throws IOException {
        standardJavaFileManager.setLocationFromPaths( location, paths );
    }


    public void setLocationForModule( final Location location, final String moduleName, final Collection<? extends Path> paths ) throws IOException {
        standardJavaFileManager.setLocationForModule( location, moduleName, paths );
    }


    public Iterable<? extends File> getLocation( final Location location ) {
        return standardJavaFileManager.getLocation( location );
    }


    public Iterable<? extends Path> getLocationAsPaths( final Location location ) {
        return standardJavaFileManager.getLocationAsPaths( location );
    }


    public Path asPath( final FileObject file ) {
        return standardJavaFileManager.asPath( file );
    }


    public void setPathFactory( final StandardJavaFileManager.PathFactory f ) {
        standardJavaFileManager.setPathFactory( f );
    }


    @Override
    public ClassLoader getClassLoader( final Location location ) {
        return standardJavaFileManager.getClassLoader( location );
    }


    @Override
    public Iterable<JavaFileObject> list( final Location location, final String packageName, final Set<JavaFileObject.Kind> kinds, final boolean recurse ) throws IOException {
        return standardJavaFileManager.list( location, packageName, kinds, recurse );
    }


    @Override
    public String inferBinaryName( final Location location, final JavaFileObject file ) {
        return standardJavaFileManager.inferBinaryName( location, file );
    }


    @Override
    public boolean handleOption( final String current, final Iterator<String> remaining ) {
        return standardJavaFileManager.handleOption( current, remaining );
    }


    @Override
    public boolean hasLocation( final Location location ) {
        return standardJavaFileManager.hasLocation( location );
    }


    @Override
    public FileObject getFileForInput( final Location location, final String packageName, final String relativeName ) throws IOException {
        return standardJavaFileManager.getFileForInput( location, packageName, relativeName );
    }


    @Override
    public FileObject getFileForOutput( final Location location, final String packageName, final String relativeName, final FileObject sibling ) throws IOException {
        return standardJavaFileManager.getFileForOutput( location, packageName, relativeName, sibling );
    }


    @Override
    public void flush() throws IOException {
        standardJavaFileManager.flush();
    }


    @Override
    public void close() throws IOException {
        standardJavaFileManager.close();
    }


    @Override
    public Location getLocationForModule( final Location location, final String moduleName ) throws IOException {
        return standardJavaFileManager.getLocationForModule( location, moduleName );
    }


    @Override
    public Location getLocationForModule( final Location location, final JavaFileObject fo ) throws IOException {
        return standardJavaFileManager.getLocationForModule( location, fo );
    }


    @Override
    public <S> ServiceLoader<S> getServiceLoader( final Location location, final Class<S> service ) throws IOException {
        return standardJavaFileManager.getServiceLoader( location, service );
    }


    @Override
    public String inferModuleName( final Location location ) throws IOException {
        return standardJavaFileManager.inferModuleName( location );
    }


    @Override
    public Iterable<Set<Location>> listLocationsForModules( final Location location ) throws IOException {
        return standardJavaFileManager.listLocationsForModules( location );
    }


    @Override
    public boolean contains( final Location location, final FileObject fo ) throws IOException {
        return standardJavaFileManager.contains( location, fo );
    }


    @Override
    public int isSupportedOption( final String option ) {
        return standardJavaFileManager.isSupportedOption( option );
    }
}
