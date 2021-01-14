package com.dilatush.util.cli;

/**
 * Specifies whether argument parameters are allowed.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public enum ParameterMode {

    DISALLOWED,  // an argument parameter is not allowed for this argument (optional arguments only)
    OPTIONAL,    // an argument parameter is optional for this argument
    MANDATORY    // an argument parameter is mandatory for this argument
}
