package com.dilatush.util;

import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * This class embeds the Java standard scheduled executor and delegates all the standard {@link ScheduledExecutorService} methods to it.  However,
 * this class provides much more convenient constructors and provides scheduling methods that use the {@code java.time} package's classes.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class ScheduledExecutor implements ScheduledExecutorService {

    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // the standard scheduled executor service that we're delegating to...
    private final ScheduledExecutorService service;

    // a counter to let the threads have different names...
    private int threadNumber = 0;


    /**
     * Create a new instance of this class with the given number of executor threads and daemon status.
     *
     * @param _threads The number of executor threads to use.
     * @param _daemon If {@code true}, the executor threads will be daemon threads; otherwise they will be standard user threads.
     */
    public ScheduledExecutor( final int _threads, final boolean _daemon ) {

        // make a thread factory...
        ThreadFactory threadFactory = (runnable) -> {
            Thread thread = Executors.defaultThreadFactory().newThread( runnable );
            thread.setDaemon( _daemon );
            thread.setName( "ScheduledExecutor" + threadNumber );
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

        service = new ScheduledThreadPoolExecutor( _threads, threadFactory );
    }


    /**
     * Create a new instance of this class with a single daemon executor thread.
     */
    public ScheduledExecutor() {
        this( 1, true );
    }


    /**
     * Create a new instance of this class with the given number of daemon executor threads.
     *
     * @param _threads The number of executor threads to use.
     */
    public ScheduledExecutor( final int _threads ) {
        this( _threads, true );
    }


    /**
     * Create a new instance of this class with a single executor thread that is a daemon thread if the argument is {@code true}.
     *
     * @param _daemon If {@code true}, the executor thread will be a daemon thread; otherwise it will be a standard user thread.
     */
    public ScheduledExecutor( final boolean _daemon ) {
        this( 1, _daemon );
    }


    /**
     * Creates and executes a one-shot action that executes after the given delay.
     *
     * @param _command The task to execute.
     * @param _delay The time from now to delay execution.
     * @param _unit The time unit of the delay parameter.
     * @return a ScheduledFuture representing pending completion of the task and whose {@code get()} method will return {@code null} upon completion
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is {@code null}
     */
    @Override
    public ScheduledFuture<?> schedule( final Runnable _command, final long _delay, final TimeUnit _unit ) {
        return service.schedule( _command, _delay, _unit );
    }


    /**
     * Creates and executes a one-shot action that executes after the given delay.
     *
     * @param _command The task to execute.
     * @param _delay The time from now to delay execution.
     * @return a ScheduledFuture representing pending completion of the task and whose {@code get()} method will return {@code null} upon completion
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is {@code null}
     */
    public ScheduledFuture<?> schedule( final Runnable _command, final Duration _delay ) {
        return service.schedule( _command, _delay.toMillis(), TimeUnit.MILLISECONDS );
    }


    /**
     * Creates and executes a {@link ScheduledFuture} that executes after the given delay.
     *
     * @param _callable The function to execute.
     * @param _delay The time from now to delay execution.
     * @param _unit the time unit of the delay parameter
     * @return a {@link ScheduledFuture} that can be used to extract result or cancel
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if callable is {@code null}
     */
    @Override
    public <V> ScheduledFuture<V> schedule( final Callable<V> _callable, final long _delay, final TimeUnit _unit ) {
        return service.schedule( _callable, _delay, _unit );
    }


    /**
     * Creates and executes a {@link ScheduledFuture} that executes after the given delay.
     *
     * @param <V> The return value type of the function to execute.
     * @param _callable The function to execute.
     * @param _delay The time from now to delay execution.
     * @return a {@link ScheduledFuture} that can be used to extract result or cancel
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if callable is {@code null}
     */
    public <V> ScheduledFuture<V> schedule( final Callable<V> _callable, final Duration _delay ) {
        return service.schedule( _callable, _delay.toMillis(), TimeUnit.MILLISECONDS );
    }


    /**
     * Creates and executes a periodic action that executes first after the given initial delay, and subsequently with the given period; that
     * is executions will commence after {@code _initialDelay} then {@code _initialDelay + _period}, then {@code _initialDelay + 2 * _period}, and so
     * on.  If  any execution of the task encounters an exception, subsequent executions are suppressed.  Otherwise, the task will only terminate via
     * cancellation or termination of the executor.  If any execution of this task takes longer than its period, then subsequent executions may start
     * late, but will not concurrently execute.
     *
     * @param _command The task to execute.
     * @param _initialDelay The time to delay first execution.
     * @param _period The period between successive executions.
     * @param _unit The time unit of the {@code _initialDelay} and {@code _period} arguments.
     * @return a {@link ScheduledFuture} representing pending completion of the task, and whose {@code get()} method will throw an exception upon
     *         cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if period less than or equal to zero
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate( final Runnable _command, final long _initialDelay, final long _period, final TimeUnit _unit ) {
        return service.scheduleAtFixedRate( _command, _initialDelay, _period, _unit );
    }


    /**
     * Creates and executes a periodic action that executes first after the given initial delay, and subsequently with the given period; that
     * is executions will commence after {@code _initialDelay} then {@code _initialDelay + _period}, then {@code _initialDelay + 2 * _period}, and so
     * on.  If  any execution of the task encounters an exception, subsequent executions are suppressed.  Otherwise, the task will only terminate via
     * cancellation or termination of the executor.  If any execution of this task takes longer than its period, then subsequent executions may start
     * late, but will not concurrently execute.
     *
     * @param _command The task to execute.
     * @param _initialDelay The time to delay first execution.
     * @param _period The period between successive executions.
     * @return a {@link ScheduledFuture} representing pending completion of the task, and whose {@code get()} method will throw an exception upon
     *         cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if period less than or equal to zero
     */
    public ScheduledFuture<?> scheduleAtFixedRate( final Runnable _command, final Duration _initialDelay, final Duration _period ) {
        return service.scheduleAtFixedRate( _command, _initialDelay.toMillis(), _period.toMillis(), TimeUnit.MILLISECONDS );
    }


    /**
     * Creates and executes a periodic action that executes first after the given initial delay, and subsequently with the given delay between the
     * termination of one execution and the commencement of the next.  If any execution of the task encounters an exception, subsequent executions are
     * suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor.
     *
     * @param _command The task to execute.
     * @param _initialDelay The time to delay first execution.
     * @param _delay The delay between the termination of one execution and the commencement of the next.
     * @param _unit The time unit of the {@code _initialDelay} and {@code _delay} parameters.
     * @return a {@link ScheduledFuture} representing pending completion of the task, and whose {@code get()} method will throw an exception upon
     *         cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay( final Runnable _command, final long _initialDelay, final long _delay, final TimeUnit _unit ) {
        return service.scheduleWithFixedDelay( _command, _initialDelay, _delay, _unit );
    }


    /**
     * Creates and executes a periodic action that executes first after the given initial delay, and subsequently with the given delay between the
     * termination of one execution and the commencement of the next.  If any execution of the task encounters an exception, subsequent executions are
     * suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor.
     *
     * @param _command The task to execute.
     * @param _initialDelay The time to delay first execution.
     * @param _delay The delay between the termination of one execution and the commencement of the next.
     * @return a {@link ScheduledFuture} representing pending completion of the task, and whose {@code get()} method will throw an exception upon
     *         cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    public ScheduledFuture<?> scheduleWithFixedDelay( final Runnable _command, final Duration _initialDelay, final Duration _delay ) {
        return service.scheduleWithFixedDelay( _command, _initialDelay.toMillis(), _delay.toMillis(), TimeUnit.MILLISECONDS );
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
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    @Override
    public void shutdown() {
        service.shutdown();
    }


    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    @Override
    public List<Runnable> shutdownNow() {
        return service.shutdownNow();
    }


    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    @Override
    public boolean isShutdown() {
        return service.isShutdown();
    }


    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    @Override
    public boolean isTerminated() {
        return service.isTerminated();
    }


    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean awaitTermination( final long timeout, final TimeUnit unit ) throws InterruptedException {
        return service.awaitTermination( timeout, unit );
    }


    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    @Override
    public <T> Future<T> submit( final Callable<T> task ) {
        return service.submit( task );
    }


    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task the task to submit
     * @param result the result to return
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    @Override
    public <T> Future<T> submit( final Runnable task, final T result ) {
        return service.submit( task, result );
    }


    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    @Override
    public Future<?> submit( final Runnable task ) {
        return service.submit( task );
    }


    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks ) throws InterruptedException {
        return service.invokeAll( tasks );
    }


    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or
     *         unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *         for execution
     */
    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit ) throws InterruptedException {
        return service.invokeAll( tasks, timeout, unit );
    }


    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do. Upon normal or exceptional return,
     * tasks that have not completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks or any element task
     *         subject to execution is {@code null}
     * @throws IllegalArgumentException if tasks is empty
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> tasks ) throws InterruptedException, ExecutionException {
        return service.invokeAny( tasks );
    }


    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element
     *         task subject to execution is {@code null}
     * @throws TimeoutException if the given timeout elapses before
     *         any task successfully completes
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
        return service.invokeAny( tasks, timeout, unit );
    }


    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the {@code Executor} implementation.
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution
     * @throws NullPointerException if command is null
     */
    @Override
    public void execute( final Runnable command ) {
        service.execute( command );
    }
}
