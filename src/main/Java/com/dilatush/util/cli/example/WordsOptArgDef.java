package com.dilatush.util.cli.example;

import com.dilatush.util.cli.InteractiveMode;
import com.dilatush.util.cli.ParameterMode;
import com.dilatush.util.cli.argdefs.OptArgDef;
import com.dilatush.util.cli.argdefs.OptArgNames;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class WordsOptArgDef extends OptArgDef {

    /**
     * Creates a new instance of this class with the given values.
     */
    public WordsOptArgDef() {

        super(
                "word",
                "Specify a word to be counted (multiple allowed).",
                "Specify a word to be counted.  Any number of words may be specified in separate arguments.  By default the word matching is " +
                        "insensitive to case, but that can be changed (see -s, --case_sensitive).  If no words are specified, then ALL words " +
                        "will be counted.  Only complete matches will be counted; for instance 'oh' will not match 'ohio'.  The specified word " +
                        "must be bounded by whitespace or standard punctuation (period, comma, colon, etc.) to be counted.",
                0,
                "word",
                String.class,
                ParameterMode.MANDATORY,
                null,
                null,
                null,
                new OptArgNames( "w;word" ),
                null,
                InteractiveMode.DISALLOWED,
                null
        );
    }
}
