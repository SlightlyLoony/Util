package com.dilatush.util;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * This class embeds the Java standard thread pool executor and delegates all the standard {@link ExecutorService} methods to it.  However,
 * this class provides much more convenient constructors and {@code java.time} package for setting keepalive time.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ExecutorService implements java.util.concurrent.ExecutorService {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // our captive thread pool executor...
    private final ThreadPoolExecutor executorService;

    // a counter to let threads have different names...
    private int threadNumber = 0;


    /**
     * Creates an executor service with the given minimum and maximum thread pool size, the given keepalive time for idle threads when more than the
     * {@code _minPoolSize} threads are alive, the given daemon or user thread mode, and the given maximum size of task queue (which may be zero).
     *
     * @param _minPoolSize The minimum size of the thread pool.  This many threads will always be running, even if idle.
     * @param _maxPoolSize The maximum size of the thread pool.  If this is greater than the minimum size, then submitted tasks will create
     *                     new threads to run in, up to the maximum number of threads.  If a task is submitted when the maximum number of threads
     *                     are already busy, it will be queued.
     * @param _keepAliveTime If more than the minimum number of threads are running, this is how long a thread may be idle before it is shutdown.
     * @param _daemon If {@code true}, threads in the thread pool will be created as daemon threads.  Otherwise, they will be created as standard
     *                user threads.
     * @param _maxQueued The maximum number of tasks that may be queued when there are no threads available to run them.
     * @param _callerRuns If {@code true}, then when tasks are submitted when all threads are busy and the queue is full, the task will run in the
     *                    calling thread.  Otherwise, a {@link RejectedExecutionException} is thrown.
     */
    public ExecutorService( final int _minPoolSize, final int _maxPoolSize, final Duration _keepAliveTime,
                            final boolean _daemon, final int _maxQueued, final boolean _callerRuns ) {

        // fail fast if we got some bogus arguments...


        // make a thread factory...
        ThreadFactory threadFactory = (runnable) -> {
            Thread thread = Executors.defaultThreadFactory().newThread( runnable );
            thread.setDaemon( _daemon );
            thread.setName( "ExecutorService" + threadNumber );
            threadNumber++;

            // make sure we've got permission to modify this thread...
            try {
                thread.checkAccess();
            }
            catch( SecurityException _se ) {
                LOGGER.warning( "Thread can't be modified: add 'modifyThread' RuntimePermission to java.policy file" );
            }
            return thread;
        };

        // make the appropriate blocking queue...
        BlockingQueue<Runnable> blockingQueue = (_maxQueued == 0) ? new SynchronousQueue<>() : new ArrayBlockingQueue<>( _maxQueued );

        // get the right rejection handler...
        RejectedExecutionHandler handler = _callerRuns ? new ThreadPoolExecutor.CallerRunsPolicy() : new ThreadPoolExecutor.AbortPolicy();

        // now we can build our executor!
        executorService = new ThreadPoolExecutor( _minPoolSize, _maxPoolSize, _keepAliveTime.toMillis(),
                TimeUnit.MILLISECONDS, blockingQueue, threadFactory, handler );
    }


    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    @Override
    public void execute( final Runnable command ) {
        executorService.execute( command );
    }


    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException {@inheritDoc}
     */
    @Override
    public void shutdown() {
        executorService.shutdown();
    }


    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * @throws SecurityException {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }


    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }


    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     *
     * @return {@code true} if terminating but not yet terminated
     */
    @SuppressWarnings( "unused" )
    public boolean isTerminating() {
        return executorService.isTerminating();
    }


    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }


    @Override
    public boolean awaitTermination( final long timeout, final TimeUnit unit ) throws InterruptedException {
        return executorService.awaitTermination( timeout, unit );
    }


    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    @SuppressWarnings( "unused" )
    public void setThreadFactory( final ThreadFactory threadFactory ) {
        executorService.setThreadFactory( threadFactory );
    }


    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    @SuppressWarnings( "unused" )
    public ThreadFactory getThreadFactory() {
        return executorService.getThreadFactory();
    }


    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    @SuppressWarnings( "unused" )
    public void setRejectedExecutionHandler( final RejectedExecutionHandler handler ) {
        executorService.setRejectedExecutionHandler( handler );
    }


    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    @SuppressWarnings( "unused" )
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return executorService.getRejectedExecutionHandler();
    }


    /**
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle.  If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    @SuppressWarnings( "unused" )
    public void setCorePoolSize( final int corePoolSize ) {
        executorService.setCorePoolSize( corePoolSize );
    }


    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    @SuppressWarnings( "unused" )
    public int getCorePoolSize() {
        return executorService.getCorePoolSize();
    }


    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     *
     * @return {@code true} if a thread was started
     */
    @SuppressWarnings( "unused" )
    public boolean prestartCoreThread() {
        return executorService.prestartCoreThread();
    }


    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     *
     * @return the number of threads started
     */
    @SuppressWarnings( "unused" )
    public int prestartAllCoreThreads() {
        return executorService.prestartAllCoreThreads();
    }


    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     *
     * @return {@code true} if core threads are allowed to time out,
     *         else {@code false}
     *
     * @since 1.6
     */
    @SuppressWarnings( "unused" )
    public boolean allowsCoreThreadTimeOut() {
        return executorService.allowsCoreThreadTimeOut();
    }


    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     *
     * @param value {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException if value is {@code true}
     *         and the current keep-alive time is not greater than zero
     *
     * @since 1.6
     */
    @SuppressWarnings( "unused" )
    public void allowCoreThreadTimeOut( final boolean value ) {
        executorService.allowCoreThreadTimeOut( value );
    }


    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *         less than or equal to zero, or
     *         less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    @SuppressWarnings( "unused" )
    public void setMaximumPoolSize( final int maximumPoolSize ) {
        executorService.setMaximumPoolSize( maximumPoolSize );
    }


    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    @SuppressWarnings( "unused" )
    public int getMaximumPoolSize() {
        return executorService.getMaximumPoolSize();
    }


    /**
     * Sets the time limit for which threads may remain idle before
     * being terminated.  If there are more than the core number of
     * threads currently in the pool, after waiting this amount of
     * time without processing a task, excess threads will be
     * terminated.  This overrides any value set in the constructor.
     *
     * @param time the time to wait.  A time value of zero will cause
     *        excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *         if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    @SuppressWarnings( "unused" )
    public void setKeepAliveTime( final long time, final TimeUnit unit ) {
        executorService.setKeepAliveTime( time, unit );
    }


    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads in excess of the core pool size may remain
     * idle before being terminated.
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    @SuppressWarnings( "unused" )
    public long getKeepAliveTime( final TimeUnit unit ) {
        return executorService.getKeepAliveTime( unit );
    }


    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return executorService.getQueue();
    }


    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     *
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    @SuppressWarnings( "unused" )
    public boolean remove( final Runnable task ) {
        return executorService.remove( task );
    }


    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     */
    @SuppressWarnings( "unused" )
    public void purge() {
        executorService.purge();
    }


    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    @SuppressWarnings( "unused" )
    public int getPoolSize() {
        return executorService.getPoolSize();
    }


    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * @return the number of threads
     */
    @SuppressWarnings( "unused" )
    public int getActiveCount() {
        return executorService.getActiveCount();
    }


    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return the number of threads
     */
    @SuppressWarnings( "unused" )
    public int getLargestPoolSize() {
        return executorService.getLargestPoolSize();
    }


    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     *
     * @return the number of tasks
     */
    @SuppressWarnings( "unused" )
    public long getTaskCount() {
        return executorService.getTaskCount();
    }


    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * @return the number of tasks
     */
    @SuppressWarnings( "unused" )
    public long getCompletedTaskCount() {
        return executorService.getCompletedTaskCount();
    }


    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    @Override
    public String toString() {
        return executorService.toString();
    }


    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     * @param task task
     */
    @Override
    public Future<?> submit( final Runnable task ) {
        return executorService.submit( task );
    }


    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     * @param task task
     * @param result result
     */
    @Override
    public <T> Future<T> submit( final Runnable task, final T result ) {
        return executorService.submit( task, result );
    }


    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     * @param task task
     */
    @Override
    public <T> Future<T> submit( final Callable<T> task ) {
        return executorService.submit( task );
    }


    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> tasks ) throws InterruptedException, ExecutionException {
        return executorService.invokeAny( tasks );
    }


    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny( tasks, timeout, unit );
    }


    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks ) throws InterruptedException {
        return executorService.invokeAll( tasks );
    }


    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit ) throws InterruptedException {
        return executorService.invokeAll( tasks, timeout, unit );
    }


    public static void main( final String[] _args ) throws InterruptedException {

        System.out.println( "Start" );
        ExecutorService es = new ExecutorService( 1, 10, Duration.ofSeconds( 1 ), true, 5, true );
        Runnable r = () -> {
            try {
                sleep( 1000 );
                System.out.println( Thread.currentThread().getName() + ": Woke" );
            }
            catch( InterruptedException _e ) {
                System.out.println( "Interrupted" );
            }
        };
        for( int i = 0; i < 10; i++ )
            es.submit( r );

        sleep( 15000 );
    }
}
