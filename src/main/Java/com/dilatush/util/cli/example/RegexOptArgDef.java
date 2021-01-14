package com.dilatush.util.cli.example;

import com.dilatush.util.cli.InteractiveMode;
import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.argdefs.OptArgDef;
import com.dilatush.util.cli.argdefs.OptArgNames;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class RegexOptArgDef extends OptArgDef {

    /**
     * Creates a new instance of this class.
     */
    public RegexOptArgDef() {

        super(
                "regex",
                "The regular expression to look for when counting REGEX.",
                "The regular expression to look for when counting REGEX hits.  The regular expression can be anything that Java can process.",
                1,
                "regex",
                String.class,
                ParameterMode.MANDATORY,
                null,
                null,
                null,
                new OptArgNames( "r;regex" ),
                null,
                InteractiveMode.PLAIN,
                "Enter the regular expression to count: " );
    }
}
