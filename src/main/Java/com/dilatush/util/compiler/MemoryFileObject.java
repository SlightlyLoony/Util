package com.dilatush.util.compiler;

import com.dilatush.util.Outcome;
import com.dilatush.util.Strings;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.net.URI;

import static com.dilatush.util.Files.readToString;
import static com.dilatush.util.Strings.isEmpty;
import static com.dilatush.util.Strings.substitute;
import static javax.tools.JavaFileObject.Kind.SOURCE;

/**
 * Instances of this class contain the source code for a Java class as a simple string.  This class extends {@link SimpleJavaFileObject} so that it
 * may be used as an input to {@link JavaCompiler} for source code "files".  The source code strings may be supplied directly, or they may be read
 * from a file.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class MemoryFileObject extends SimpleJavaFileObject {


    private final static Outcome.Forge<MemoryFileObject> OUTCOME = new Outcome.Forge<>();  // factory for Outcomes...


    private final String source;      // contains the source code for a Java Class...


    /**
     * Constructs a new instance of this class from a string containing the Java source code.  If a substitutions document is included, the
     * given source is potentially modified by the given substitution document.  See {@link Strings#substitute(String,String)} for details on the
     * substitution.
     *
     * @param _binaryName The binary name of the class represented by this file object.
     * @param _source The source code for the class represented by this file object.
     * @param _substitutions A substitution document (see the project README for details). This may be {@code null} if no substitutions are needed.
     */
    private MemoryFileObject( final String _binaryName, final String _source, final String _substitutions ) {

        // create a URI to make the base class happy...
        super( URI.create( "string:///" + _binaryName.replace( '.', '/' ) + SOURCE.extension ), SOURCE );

        source = (_substitutions == null)
                ? _source
                : substitute( _source, _substitutions );
    }


    /**
     * Create a new instance of {@link MemoryFileObject} with the given binary name, Java source code, and optional substitutions document.  If the
     * new instance was successfully created, it is available in the {@link Outcome#info()}.  Otherwise the {@link Outcome#msg()} contains an
     * explanatory message about why the creation failed.  If the optional substitutions document is not {@code null}, the given substitutions will
     * be applied to the Java source code.  See {@link Strings#substitute(String,String)} for more details.
     *
     * @param _binaryName The binary name of the class contained in the Java source code.
     * @param _source The Java source code.
     * @param _substitutions The optional substitutions document.
     * @return the {@link Outcome} with the results of the creation
     */
    public static Outcome<MemoryFileObject> fromString( final String _binaryName, final String _source, final String _substitutions ) {

        if( isEmpty( _binaryName ) || isEmpty( _source ) )
            return OUTCOME.notOk( "Missing binary name or source" );
        return OUTCOME.ok( new MemoryFileObject( _binaryName, _source, _substitutions ) );
    }


    /**
     * Create a new instance of {@link MemoryFileObject} with the given binary name, Java source code.  If the new instance was successfully created,
     * it is available in the {@link Outcome#info()}.  Otherwise the {@link Outcome#msg()} contains an explanatory message about why the creation
     * failed.
     *
     * @param _binaryName The binary name of the class contained in the Java source code.
     * @param _source The Java source code.
     * @return the {@link Outcome} with the results of the creation
     */
    public static Outcome<MemoryFileObject> fromString( final String _binaryName, final String _source ) {
        return fromString( _binaryName, _source, null );
    }


    /**
     * Create a new instance of {@link MemoryFileObject} with the given binary name, Java source code from the contents of the file at the given file
     * path, and optional substitutions document.  If the new instance was successfully created, it is available in the {@link Outcome#info()}.
     * Otherwise the {@link Outcome#msg()} contains an explanatory message about why the creation failed.  If the optional substitutions document is
     * not {@code null}, the given substitutions will be applied to the Java source code.  See {@link Strings#substitute(String,String)} for more
     * details.
     *
     * @param _binaryName The binary name of the class contained in the Java source code.
     * @param _filePath The path to the file that contains the Java source code.
     * @param _substitutions The optional substitutions document.
     * @return the {@link Outcome} with the results of the creation
     */
    public static Outcome<MemoryFileObject> fromFile( final String _binaryName, final String _filePath, final String _substitutions ) {

        if( isEmpty( _binaryName ) || isEmpty( _filePath) )
            return OUTCOME.notOk( "Missing binary name or file path" );
        String source = readToString( new File( _filePath ) );
        if( isEmpty( source ) )
            return OUTCOME.notOk( "File could not be read, or is empty: " + _filePath );
        return OUTCOME.ok( new MemoryFileObject( _binaryName, source, _substitutions ) );
    }


    /**
     * Create a new instance of {@link MemoryFileObject} with the given binary name, Java source code from the contents of the file at the given file
     * path.  If the new instance was successfully created, it is available in the {@link Outcome#info()}.  Otherwise the {@link Outcome#msg()}
     * contains an explanatory message about why the creation failed.
     *
     * @param _binaryName The binary name of the class contained in the Java source code.
     * @param _filePath The path to the file that contains the Java source code.
     * @return the {@link Outcome} with the results of the creation
     */
    public static Outcome<MemoryFileObject> fromFile( final String _binaryName, final String _filePath ) {
        return fromFile( _binaryName, _filePath, null );
    }


    /**
     * Create a new instance of {@link MemoryFileObject} with the given binary name, Java source code from the contents of the given file, and
     * optional substitutions document.  If the new instance was successfully created, it is available in the {@link Outcome#info()}.
     * Otherwise the {@link Outcome#msg()} contains an explanatory message about why the creation failed.  If the optional substitutions document is
     * not {@code null}, the given substitutions will be applied to the Java source code.  See {@link Strings#substitute(String,String)} for more
     * details.
     *
     * @param _binaryName The binary name of the class contained in the Java source code.
     * @param _file The file that contains the Java source code.
     * @param _substitutions The optional substitutions document.
     * @return the {@link Outcome} with the results of the creation
     */
    public static Outcome<MemoryFileObject> fromFile( final String _binaryName, final File _file, final String _substitutions ) {

        if( isEmpty( _binaryName ) || (_file == null) )
            return OUTCOME.notOk( "Missing binary name or file" );
        String source = readToString( _file );
        if( isEmpty( source ) )
            return OUTCOME.notOk( "File could not be read, or is empty: " + _file.getAbsolutePath() );
        return OUTCOME.ok( new MemoryFileObject( _binaryName, source, _substitutions ) );
    }


    /**
     * Create a new instance of {@link MemoryFileObject} with the given binary name, Java source code from the contents of the given file.  If the new
     * instance was successfully created, it is available in the {@link Outcome#info()}. Otherwise the {@link Outcome#msg()} contains an explanatory
     * message about why the creation failed.
     *
     * @param _binaryName The binary name of the class contained in the Java source code.
     * @param _file The file that contains the Java source code.
     * @return the {@link Outcome} with the results of the creation
     */
    public static Outcome<MemoryFileObject> fromFile( final String _binaryName, final File _file ) {
        return fromFile( _binaryName, _file, null );
    }


    /**
     * Return the Java source code as a string.
     *
     * @param _ignoreEncodingErrors Ignore encoding errors if {@code true}.
     * @return the Java source code as a string
     */
    @Override
    public CharSequence getCharContent( boolean _ignoreEncodingErrors ) {
        return source;
    }
}
