package com.dilatush.util.info.example;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class User {

    private final Owner  owner;


    public User( final Owner _owner ) {
        owner = _owner;
    }


    public void run() {
        System.out.println( "User sees " + owner.infoViewer.get().info );
    }


    public void tick() {
        System.out.println( "User sees " + owner.infoViewer.get().info );
    }
}
