package com.dilatush.util;

/**
 * Keeps the working directory for a program.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class WorkingDir {

    private static WorkingDir INSTANCE = new WorkingDir();

    private volatile String workingDir;


    private WorkingDir() {
        workingDir = "";
    }


    private void setImpl( final String _workingDir ) {
        workingDir = _workingDir;
    }


    private String getImpl() {
        return workingDir;
    }


    public static void set( final String _workingDir ) {
        INSTANCE.setImpl( _workingDir );
    }


    public static String get() {
        return INSTANCE.getImpl();
    }
}
