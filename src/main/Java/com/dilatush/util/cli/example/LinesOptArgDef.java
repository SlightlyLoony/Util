package com.dilatush.util.cli.example;

import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.argdefs.OptArgDef;
import com.dilatush.util.cli.argdefs.OptArgNames;
import com.dilatush.util.cli.parsers.BooleanParser;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class LinesOptArgDef extends OptArgDef {

    /**
     * Creates a new instance of this class with the given values.
     */
    public LinesOptArgDef() {

        super(
                "lines",
                "Count the lines in the specified files.",
                "Count the lines in the specified files.",
                1,
                null,
                Boolean.class,
                ParameterMode.DISALLOWED,
                null,
                new BooleanParser(),
                null,
                new OptArgNames( "l;lines" ),
                "false",
                null,
                null
        );
    }
}
