package com.dilatush.util.compiler;

import com.dilatush.util.Files;
import com.dilatush.util.Strings;

import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.net.URI;

import static com.dilatush.util.Strings.substitute;
import static javax.tools.JavaFileObject.Kind.SOURCE;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class MemoryFileObject extends SimpleJavaFileObject {


    private final String source;
    private final String binaryName;


    /**
     * Constructs a new instance of this class from a string containing the Java source code.  If a substitutions document is included, the
     * given source is potentially modified by the given substitution document.  See {@link Strings#substitute(String,String)} for details on the
     * substitution.
     *
     * @param _binaryName The binary name of the class represented by this file object.
     * @param _source The source code for the class represented by this file object.
     * @param _substitutions A substitution document (see the project README for details). This may be {@code null} if no substitutions are needed.
     */
    public MemoryFileObject( final String _binaryName, final String _source, final String _substitutions ) {

        // create a URI to make the base class happy...
        super( URI.create( "string:///" + _binaryName.replace( '.', '/' ) + SOURCE.extension ), SOURCE );

        source = (_substitutions == null)
                ? _source
                : substitute( _source, _substitutions );
        binaryName = _binaryName;
    }


    public MemoryFileObject( String _binaryName, final File _source, final String _substitutions ) {
        this( _binaryName, Files.readToString( _source ), _substitutions );
    }


    public MemoryFileObject( final String _binaryName, final String _source ) {
        this( _binaryName, _source, null );
    }


    public MemoryFileObject( String _binaryName, final File _source ) {
        this( _binaryName, Files.readToString( _source ), null );
    }


    @Override
    public CharSequence getCharContent( boolean _ignoreEncodingErrors ) {
        return source;
    }


    public String getSource() {
        return source;
    }


    public String getBinaryName() {
        return binaryName;
    }
}
