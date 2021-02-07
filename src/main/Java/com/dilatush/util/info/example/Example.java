package com.dilatush.util.info.example;

/**
 * Simple example of how to use the info package classes.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Example {


    public static void main( final String[] _args ) {

        Owner owner = new Owner();
        User user = new User( owner );

        owner.run();
        user.run();
        owner.tick();
        user.tick();
    }
}
