package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.parsers.ParameterParser;
import com.dilatush.util.cli.parsers.PathParser;
import com.dilatush.util.cli.validators.ParameterValidator;
import com.dilatush.util.cli.validators.ReadableFileValidator;

import java.io.File;

import static com.dilatush.util.cli.ParameterMode.MANDATORY;

/**
 * Define a positional argument.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class PosArgDef extends ArgDef {


    /**
     * Creates a new instance of this class with the given values.
     *
     * @param _referenceName The reference name for this argument.
     * @param _summary The summary description of this argument.
     * @param _detail The detailed description of this argument.
     * @param _maxAllowed The maximum number of appearances on the command line for this argument.
     * @param _helpName The name for this argument's parameter in help text.
     * @param _type The type of this argument's parameter.
     * @param _parameterMode The parameter mode for this argument's parameter.
     * @param _defaultValue The default value for this argument's parameter.
     * @param _parser The optional parser for this argument's parameter.
     * @param _validator The optional validator for this argument's parameter.
     */
    public PosArgDef( final String _referenceName, final String _summary, final String _detail, final int _maxAllowed,
                      final String _helpName, final Class<?> _type, final ParameterMode _parameterMode, final String _defaultValue,
                      final ParameterParser _parser, final ParameterValidator _validator ) {

        super( _referenceName, _summary, _detail, _maxAllowed, _helpName, _type, _parameterMode, _defaultValue, _parser, _validator );
    }


    public static PosArgDef getMultiReadableFilePosArgDef(final String _referenceName, final String _summary, final String _detail,
                                                          final String _helpName ) {
        return new PosArgDef(
                _referenceName, _summary, _detail, 0, _helpName, File.class, MANDATORY, null, new PathParser(), new ReadableFileValidator()
        );
    }


    /**
     * Returns {@code true} if this definition is for a positional argument that may appear other than one time on the command line.
     *
     * @return {@code true} if this definition is for a positional argument that may appear other than one time on the command line
     */
    public boolean isNotUnitary() {

        return (maxAllowed != 1) || (parameterMode != MANDATORY);
    }
}
