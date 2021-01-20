package com.dilatush.util;

import java.util.*;

/**
 * Static container class for utility functions related to Java threads.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Threads {

    private static final int MAX_CACHE_REFRESHES_PER_SECOND = 10;

    /**
     * Returns a list of all thread instances belonging to the current thread's {@link ThreadGroup} and its child {@link ThreadGroup}s.  This will
     * always exclude the threads belonging to the system thread group ({@code ReferenceHandler}, {@code Finalizer}, and {@code Signal Dispatcher},
     * generally).  Note that repeated calls to this method are quite likely to return different results as threads are created and destroyed.  In
     * some applications threads are created and maintained for the entire lifetime of the application, and this method would always return the same
     * results.  In other applications, hundreds of threads might be created and destroyed per second, and repeated calls to this method would
     * <i>never</i> return the same results.
     *
     * @return the list of {@link Thread}s found when this method was called
     */
    public static List<Thread> getApplicationThreads() {

        // get the thread group that the current thread belongs to...
        ThreadGroup ourThreadGroup = Thread.currentThread().getThreadGroup();

        // enumerate all the threads, iteratively to make certain we got them all...
        int actuallyRead;
        int wiggle = 10;  // we start with a small amount of wiggle room (over the estimated number of threads)...
        Thread[] ourThreads;
        do {
            ourThreads = new Thread[ourThreadGroup.activeCount() + wiggle];
            actuallyRead = ourThreadGroup.enumerate( ourThreads );
            wiggle <<= 2;  // double our wiggle value...
        }
        // if the number we read is the same as our array length, we likely missed some...
        while( actuallyRead == ourThreads.length );

        // toss our array into a list and we're done...
        return new ArrayList<>( Arrays.asList( Arrays.copyOfRange( ourThreads, 0, actuallyRead ) ) );
    }


    /**
     * Interrupts all threads other than the current {@link Thread} that are members of the current thread's {@link ThreadGroup} or its child
     * groups.
     */
    public static void interruptAllOtherThreads() {

        // get a list of all the candidate threads...
        List<Thread> threads = getApplicationThreads();

        // iterate over the list to interrupt all of them other than the current thread...
        long current = Thread.currentThread().getId();
        for( Thread thread : threads ) {
            if( thread.getId() != current )
                thread.interrupt();
        }
    }


    // cache of thread ID to thread mappings...
    private final static Map<Long,Thread> idToThread = new HashMap<>();

    // tracks the system time of cache refreshes we've made...
    private final static LinkedList<Long> cacheRefreshes = new LinkedList<>();


    /**
     * Refreshes the thread ID to thread mappings cache unless we're refreshing too often.
     */
    private static synchronized void refreshThreadCache() {

        // delete any old refreshes off our list
        long oldest = System.currentTimeMillis() - 1000;  // the time one second ago...
        while( (cacheRefreshes.size() > 0) && (cacheRefreshes.getFirst() < oldest) )
            cacheRefreshes.removeFirst();

        // if we refreshing at too high a rate, just bail out...
        if( cacheRefreshes.size() >= MAX_CACHE_REFRESHES_PER_SECOND )
            return;

        // iterate over all our current threads, poking them into our cache...
        idToThread.clear();
        for( Thread thread : getApplicationThreads()) {
            idToThread.put( thread.getId(), thread );
        }

        // track the fact that we just did a refresh...
        cacheRefreshes.addLast( System.currentTimeMillis() );
    }


    /**
     * Returns the {@link Thread} instance with the given ID, or {@code null} if that instance could not be located.  This method will first try
     * the cache of thread ID to thread mappings.  If that fails, it will attempt to refresh the cache - but if the refreshing is being rate-limited
     * this may do nothing.  After the attempted cache refresh, it will try the cache again, which might return either the successfully resolved
     * {@link Thread} instance, or {@code null}.
     *
     * @param _ID The thread ID of the thread to be returned.
     * @return the {@link Thread} instance with the given thread ID, or {@code null} if it could not be located
     */
    private static synchronized Thread getThreadByID( final long _ID ) {

        // check the cache...
        Thread result = idToThread.get( _ID );

        // if the cache had the answer, leave with it...
        if( result != null )
            return result;

        // otherwise, try refreshing the cache...
        refreshThreadCache();

        // return whatever the cache has (or hasn't)...
        return idToThread.get( _ID );
    }


    /**
     * <p>Returns the name of the thread with the given ID, or the string version of the given ID if the name cannot be resolved.  There are several
     * reasons why the name might not be resolved:</p>
     * <ul>
     *     <li>A thread with the given ID no longer exists (or never existed).</li>
     *     <li>The thread with the given ID is not in the current thread's thread group or its child groups.</li>
     *     <li>Refresh of the cache of thread ID to thread mappings is being rate-limited.</li>
     * </ul>
     *
     * @param _threadID The {@link Thread} ID of the thread to get the name of.
     * @return the name of the thread, or the string version of its ID
     */
    public static String getThreadName( final long _threadID ) {
        Thread thread = getThreadByID( _threadID );
        return (thread == null) ? Long.toString( _threadID ) : thread.getName();
    }
}
