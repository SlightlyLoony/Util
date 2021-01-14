package com.dilatush.util.cli.argdefs;

import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.parsers.ParameterParser;
import com.dilatush.util.cli.parsers.PathParser;
import com.dilatush.util.cli.validators.ParameterValidator;
import com.dilatush.util.cli.validators.ReadableFileValidator;

import java.io.File;

import static com.dilatush.util.cli.ParameterMode.MANDATORY;
import static com.dilatush.util.cli.ParameterMode.OPTIONAL;

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


    /**
     * Returns an instance of {@link PosArgDef} with the given values, allowing an unlimited number of appearances on the command line, a type of
     * {@link File}, at least one appearance mandatory, and the files validated as readable.
     *
     * @param _referenceName The reference name for this argument.
     * @param _summary The summary help for this argument.
     * @param _detail The detailed help for this argument.
     * @param _helpName The help name for this argument's parameter.
     * @return an instance of {@link PosArgDef} with the given values
     */
    public static PosArgDef getMultiReadableFilePosArgDef(final String _referenceName, final String _summary, final String _detail,
                                                          final String _helpName ) {
        return new PosArgDef(
                _referenceName, _summary, _detail, 0, _helpName, File.class, MANDATORY, null, new PathParser(), new ReadableFileValidator()
        );
    }


    /**
     * Returns a string with the parameter name surrounded by square brackets if optional, and by angle brackets if mandatory.
     *
     * @return the argument description string
     */
    public String getArgumentDescription() {
        String result = (parameterMode == OPTIONAL) ? "[" + helpName + "]" : "<" + helpName + ">";
        if( maxAllowed != 1 )
            result = result + "*";
        return result;
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
