package com.dilatush.util.cli;

import com.dilatush.util.cli.argdefs.OptArgDef;

/**
 * Specifies the kind of interactive parameter entry allowed for an argument defined in {@link OptArgDef}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public enum InteractiveMode {

    DISALLOWED,   // interactive mode is not allowed for a particular parameter value
    PLAIN,        // interactive mode in plain text (visible) is allowed for a particular parameter value
    HIDDEN        // interactive mode in obscured text (not visible) is allowed for a particular parameter value
}
