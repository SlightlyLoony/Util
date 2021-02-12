package com.dilatush.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Instances of this class implement a simple system for posting "Haps" (short for "Happenings"), and for objects to subscribe to them.  Each Hap is
 * comprised of an enum (of a class supplied by the user of the class) that represents the type of Hap, and an optional data object that can be
 * anything at all (including {@code null}).  All Haps are posted to listeners in a single thread, which means (a) all the listeners for a particular
 * posted Hap are called sequentially (though in no particular guaranteed order), and (b) that all the listeners for one Hap posting are called before
 * the next Hap is dispatched to its listeners.  The system is quite performant thanks to its simplicity, one weird trick for reusing Haps with no
 * data (generally the most common case), and table-driven dispatching of Haps to listeners.</p>
 * <p>On the performance, though, there is a big caveat: overall Hap dispatching performance is very sensitive to any delays (from blocking or pure
 * computational overhead) occurring in a Hap listener.  Authors should be very careful not to incur significant delays, especially for I/O blocking,
 * inside of listeners.  When blocking or significant computation overhead is required in response to a Hap, that should be done in a separate
 * thread.</p>
 * <p>If listeners throw a unhandled exception, the dispatcher within Haps will catch the exception, log a SEVERE error (with stack trace), and then
 * resume working.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public final class Haps<E extends Enum<E>> {

    // our logger...
    final static private Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    // the dispatch table with lists of subscribers, indexed by enum ordinal...
    private final List<List<HapsData<E>>>      dispatch;

    // the queue of not-yet-dispatched Haps...
    private final ArrayBlockingQueue<Hap<E>>   queue;

    // our cache of reusable Haps (those with no data)...
    private final List<Hap<E>>                 hapCache;

    // the scheduler to use for scheduled Haps, if enabled (otherwise null)...
    private final ScheduledExecutor            scheduler;

    // a list of all the Haps...
    private final List<E>                      haps;


    /**
     * Create a new instance of this class with the given maximum size for the queue of not-yet-dispatched Haps, with scheduled Haps enabled (using
     * the given scheduler) or disabled (if the given scheduler is {@code null} , and the given sample of an enum, which must be of the same type as
     * that specified for the generic type.  In other words, if the enum class you use to specify the types of Haps is named {@code MyHaps}, then your
     * Haps instance will have a type of {@code Haps<MyHaps>}, and the sample must be an instance of {@code MyHaps}.  This sample is required because
     * {@code Haps} needs get the {@code Class} object for the Haps enum class, and it cannot do this without an actual instance of the enum.
     *
     * @param _maxQueue The maximum size for the queue of not-yet-dispatched Haps.
     * @param _scheduler The {@link ScheduledExecutor} instance to use for scheduling Haps.
     * @param _sample The sample Hap enum.
     */
    public Haps( final int _maxQueue, final ScheduledExecutor _scheduler, final E _sample ) {

        // fail fast if we're missing important stuff...
        if( _maxQueue < 10 )
            throw new IllegalArgumentException( "Invalid maximum queue size; must be at least 10" );
        if( _sample == null )
            throw new IllegalArgumentException( "Missing sample Hap enum" );

        // initialize our dispatch lists...
        haps = getHapEnums( _sample );
        dispatch = new ArrayList<>( haps.size() );
        haps.forEach( (hapEnum) -> {
            dispatch.add( new ArrayList<>( 25 ) );  // add an array pre-sized for 10 subscribers (almost always plenty, but will resize if needed)...
        } );

        // build our cache of no-data Haps...
        hapCache = new ArrayList<>( haps.size() );
        haps.forEach( ( hapEnum) -> hapCache.add( new Hap<>( hapEnum, null ) ) );

        // set up our Hap queue...
        queue = new ArrayBlockingQueue<>( _maxQueue );

        // set up our scheduler...
        scheduler = _scheduler;

        // start up our executor and get our dispatcher going...
        new ExecutorService( 1 ).submit( this::dispatcher );
    }


    /**
     * Create a new instance of this class with the given maximum size for the queue of not-yet-dispatched Haps, with scheduled Haps disabled, and the
     * given sample of an enum, which must be of the same type as that specified for the generic type.  In other words, if the enum class you use to
     * specify the types of Haps is named {@code MyHaps}, then your Haps instance will have a type of {@code Haps<MyHaps>}, and the sample must be an
     * instance of {@code MyHaps}.  This sample is required because {@code Haps} needs get the {@code Class} object for the Haps enum class, and it
     * cannot do this without an actual instance of the enum.
     *
     * @param _maxQueue The maximum size for the queue of not-yet-dispatched Haps.
     * @param _sample The sample Hap enum.
     */
    public Haps( final int _maxQueue, final E _sample ) {
        this( _maxQueue, false, _sample );
    }


    /**
     * Create a new instance of this class with the given maximum size for the queue of not-yet-dispatched Haps, with the given event scheduling mode,
     * and the given sample of an enum, which must be of the same type as that specified for the generic type.  In other words, if the enum class you
     * use to specify the types of Haps is named {@code MyHaps}, then your Haps instance will have a type of {@code Haps<MyHaps>}, and the sample must
     * be an instance of {@code MyHaps}.  This sample is required because {@code Haps} needs get the {@code Class} object for the Haps enum class, and
     * it cannot do this without an actual instance of the enum.  If Hap scheduling is enabled, an instance of a single-threaded
     * {@link ScheduledExecutor} will be created and used to handle the scheduling.
     *
     * @param _maxQueue The maximum size for the queue of not-yet-dispatched Haps.
     * @param _enableScheduling {@code True} to enable Hap scheduling, {@code false} to disable it.
     * @param _sample The sample Hap enum.
     */
    public Haps( final int _maxQueue, final boolean _enableScheduling, final E _sample ) {
        this( _maxQueue, _enableScheduling ? new ScheduledExecutor() : null, _sample );
    }


    /**
     * Post a Hap with associated data, which will be dispatched to all listeners subscribed to this Hap.
     *
     * @param _hapEnum The enum for the Hap to be posted.
     * @param _data The optional data associated with the Hap ({@code null} if none).
     */
    public void post( final E _hapEnum, final Object _data ) {
        queue.add( getHap( _hapEnum, _data ) );
    }


    /**
     * Post a Hap with no associated data, which will be dispatched to all listeners subscribed to this Hap.
     *
     * @param _hapEnum The enum for the Hap to be posted.
     */
    public void post( final E _hapEnum ) {
        queue.add( getHap( _hapEnum ) );
    }


    /**
     * Subscribe to the given Hap with a listener that receives neither the Hap or its associated data.  This kind of subscription is most useful
     * when you want to take some action on the occurrence of some particular Hap, and that Hap either has no associated data or it makes no
     * difference what that data is.
     *
     * @param _hapEnum The Hap to subscribe to.
     * @param _listener The {@code Runnable} that listens for the given Hap.
     * @return an opaque object (a handle) that must be used if unsubscribing from this subscription
     */
    public Object subscribe( final E _hapEnum, final Runnable _listener) {
        return subscribeImpl( SubscriptionType.ACTION, _hapEnum, _listener );
    }


    /**
     * Subscribe to the given Hap with a listener that receives just the Hap's associated data (and not the Hap itself).  Note that it is possible
     * for the associated data to be {@code null}.  This kind of subscription is most useful for Haps used to publish some kind of information.
     *
     * @param _hapEnum The Hap to subscribe to.
     * @param _listener The {@code Consumer<Object>} that listens for the given Hap.
     * @return an opaque object (a handle) that must be used if unsubscribing from this subscription
     */
    public Object subscribe( final E _hapEnum, final Consumer<Object> _listener ) {
        return subscribeImpl( SubscriptionType.DATA, _hapEnum, _listener );
    }


    /**
     * Subscribe to the given Hap with a listener that receives just the Hap (and not its associated data), for any number of different Hap enums.  If
     * no Hap enums are specified, then <i>all</i> Hap enums are subscribed to.  Note that the Hap enum argument is the <i>last</i> argument, not the
     * first as on the other {@code subscribe()} methods.  This is both because the varargs argument must be the last one, and because otherwise
     * the method signature would conflict with the other {@code subscribe()} methods.
     *
     * @param _listener The {@code Consumer<Hap>} that listens for the given Hap.
     * @param _hapEnums The Haps to subscribe to, or (if none specified), subscribe to <i>all</i> Haps.
     * @return a list of the opaque objects (handles) that must be used if unsubscribing from these subscriptions
     */
    @SafeVarargs
    public final List<Object> subscribe( final Consumer<E> _listener, final E... _hapEnums ) {

        // make a list to hold all the handles we generate...
        List<Object> handles = new ArrayList<>();

        // if subscribing to all, iterate over all the enum constants...
        if( _hapEnums.length == 0 )
            haps.forEach( (hap) -> handles.add( subscribeImpl( SubscriptionType.HAP, hap, _listener ) ) );

        // otherwise, iterate over the given enums...
        else
            Arrays.asList( _hapEnums ).forEach( (hap) -> handles.add( subscribeImpl( SubscriptionType.HAP, hap, _listener ) ) );

        return handles;
    }


    /**
     * Subscribe to the given Hap with a listener that receives the Hap and its associated data, for any number of different Hap enums.  If
     * no Hap enums are specified, then <i>all</i> Hap enums are subscribed to.  Note that the Hap enum argument is the <i>last</i> argument, not the
     * first as on the other {@code subscribe()} methods.  This is both because the varargs argument must be the last one, and because otherwise
     * the method signature would conflict with the other {@code subscribe()} methods.
     *
     * @param _listener The {@code BiConsumer<Hap,Object>} that listens for the given Hap.
     * @param _hapEnums The Haps to subscribe to, or (if none specified), subscribe to <i>all</i> Haps.
     * @return a list of the opaque objects (handles) that must be used if unsubscribing from these subscriptions
     */
    @SafeVarargs
    public final List<Object> subscribe( final BiConsumer<E, Object> _listener, final E... _hapEnums ) {

        // make a list to hold all the handles we generate...
        List<Object> handles = new ArrayList<>();

        // if subscribing to all, iterate over all the enum constants...
        if( _hapEnums.length == 0 )
            haps.forEach( (hap) -> handles.add( subscribeImpl( SubscriptionType.HAPDATA, hap, _listener ) ) );

            // otherwise, iterate over the given enums...
        else
            Arrays.asList( _hapEnums ).forEach( (hap) -> handles.add( subscribeImpl( SubscriptionType.HAPDATA, hap, _listener ) ) );

        return handles;
    }


    /**
     * Implements subscribe by creating a {@link HapsData} instance populated with all the information required for a subscription, then posting that
     * as a "system Hap" (a {@link Hap} instance with a null enum).  The dispatcher will then recognize that as a system Hap when it dequeues it,
     * and the modification of the dispatch tables occurs in the dispatch thread.  This little trick completely eliminates any threading issues when
     * subscribing.  The {@link #unsubscribe(Object)} method handles unsubscribing in a similar fashion, for the same reason.
     *
     * @param _subscriptionType Indicates whether the subscription is for data, Haps, both, or neither.
     * @param _hapEnum The enum indicating what kind of Hap is being subscribed to.
     * @param _listener The listener for this subscription, which is an Object because it is of a different type for each subscription type.
     * @return an opaque object (a handle) that must be used if unsubscribing from this subscription
     */
    private Object subscribeImpl( final SubscriptionType _subscriptionType, final E _hapEnum, final Object _listener ) {

        // make our subscribe data...
        HapsData<E> data = new HapsData<>( HapsAction.SUBSCRIBE, _subscriptionType, _hapEnum, _listener );

        // post our system event...
        queue.add( getSystemHap( data ) );

        // return the subscribe data to the user as an opaque handle to use when unsubscribing...
        return data;
    }


    /**
     * Unsubscribe from the subscription created with a {@code subscribe} method that returned the given {@code Object}.
     *
     * @param _handle The {@link Object} returned by the {@code subscribe} method used to create the subscription to unsubscribe from.
     */
    public void unsubscribe( final Object _handle ) {

        // fail fast if we got a null...
        if( _handle == null )
            throw new IllegalArgumentException( "Missing handles" );

        // get our system data...
        HapsData<E> data = cast( _handle );

        // post our system event...
        queue.add( getSystemHap( data.toUnsubscribe() ) );
    }


    /**
     * Unsubscribe from the subscriptions created with a {@code subscribe} method that returned the given {@code List&lt;Object&gt;}.
     *
     * @param _handles The {@link List} returned by the {@code subscribe} method used to create the subscriptions to unsubscribe from.
     */
    public void unsubscribe( final List<Object> _handles ) {

        // fail fast if we got a null...
        if( _handles == null )
            throw new IllegalArgumentException( "Missing handles" );

        _handles.forEach( this::unsubscribe );
    }


    /**
     * Schedule posting of the Hap with the given Hap enum and associated data after the given delay.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _data The optional (may be {@code null}) data associated with the Hap.
     * @param _delay The {@link Duration} specifying the delay from now until when the Hap is to be posted.  This duration must be non-negative.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> schedule( final E _hapEnum, final Object _data, final Duration _delay ) {

        // fail fast if the arguments are bogus...
        argsCheck( _hapEnum, _delay );

        return scheduler.schedule( () -> post( _hapEnum, _data ), _delay );
    }


    /**
     * Schedule posting of the Hap with the given Hap enum (with no associated data) after the given delay.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _delay The {@link Duration} specifying the delay from now until when the Hap is to be posted.  This duration must be non-negative.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> schedule( final E _hapEnum, final Duration _delay ) {
        return schedule( _hapEnum, null, _delay );
    }


    /**
     * Schedule posting of the Hap with the given Hap enum and associated data first after the given initial delay, and then on average at the rate
     * defined by the given period.  Note that if the scheduler's thread(s) become busy and can't keep up with the specified rate, the scheduled
     * Haps will queue up and execute in a burst once the schedule <i>does</i> have time available.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _data The optional (may be {@code null}) data associated with the Hap.  Note that this data will be the same on all scheduled Haps.
     * @param _initialDelay The {@link Duration} delay before the first scheduled Hap is posted.
     * @param _period The (on average) period between scheduled Hap postings, after the first one.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> scheduleAtFixedRate( final E _hapEnum, final Object _data, final Duration _initialDelay, final Duration _period ) {

        // fail fast if the arguments are bogus...
        argsCheck( _hapEnum, _initialDelay );
        if( (_period == null) || (_period.isNegative()) )
            throw new IllegalArgumentException( "Period is null or invalid" );

        return scheduler.scheduleAtFixedRate( () -> post( _hapEnum, _data ), _initialDelay, _period );
    }


    /**
     * Check the given arguments for being null or invalid.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _initialDelay The {@link Duration} delay before the first scheduled Hap is posted.
     */
    private void argsCheck( final E _hapEnum, final Duration _initialDelay ) {

        if( scheduler == null )
            throw new IllegalStateException( "Scheduling Haps is disabled" );
        if( _hapEnum == null )
            throw new IllegalArgumentException( "Missing Hap enum" );
        if( (_initialDelay == null) || (_initialDelay.isNegative()) )
            throw new IllegalArgumentException( "Initial delay is null or invalid" );
    }


    /**
     * Schedule posting of the Hap with the given Hap enum and associated data first after the given initial delay, and then on average at the rate
     * defined by the given period.  The associated data is obtained by calling the given {@link Supplier} when each of the scheduled Haps is posted.
     * Note that if the scheduler's thread(s) become busy and can't keep up with the specified rate, the scheduled Haps will queue up and execute in a
     * burst once the schedule <i>does</i> have time available.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _supplier The {@link Supplier} to be called on each Hap posting to obtain the associated data.
     * @param _initialDelay The {@link Duration} delay before the first scheduled Hap is posted.
     * @param _period The (on average) period between scheduled Hap postings, after the first one.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> scheduleAtFixedRateWithSupplier( final E _hapEnum, final Supplier<Object> _supplier,
                                                   final Duration _initialDelay, final Duration _period ) {

        // fail fast if we got no supplier...
        if( _supplier == null )
            throw new IllegalArgumentException( "Missing supplier" );

        return scheduleAtFixedRate( _hapEnum, _supplier.get(), _initialDelay, _period );
    }


    /**
     * Schedule posting of the Hap with the given Hap enum (with no associated data) first after the given initial delay, and then on average at the
     * rate defined by the given period.  Note that if the scheduler's thread(s) become busy and can't keep up with the specified rate, the scheduled
     * Haps will queue up and execute in a burst once the schedule <i>does</i> have time available.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _initialDelay The {@link Duration} delay before the first scheduled Hap is posted.
     * @param _period The (on average) period between scheduled Hap postings, after the first one.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> scheduleAtFixedRate( final E _hapEnum, final Duration _initialDelay, final Duration _period ) {
        return scheduleAtFixedRate( _hapEnum, null, _initialDelay, _period );
    }


    /**
     * Schedule posting of the Hap with the given Hap enum and associated data first after the given initial delay, and subsequently with the given
     * delay from the completion of one scheduled posting to the next scheduled posting.  The average rate over time will be somewhat less than the
     * rate defined by the given delay because of the execution time of each scheduled posting and the delays within the scheduler.  This method will
     * not cause bursts of postings when the scheduler's thread free up after a busy period.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _data The optional (may be {@code null}) data associated with the Hap.  Note that this data will be the same on all scheduled Haps.
     * @param _initialDelay The {@link Duration} delay before the first scheduled Hap is posted.
     * @param _delay The delay between scheduled Hap postings.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> scheduleWithFixedDelay( final E _hapEnum, final Object _data, final Duration _initialDelay, final Duration _delay ) {

        // fail fast if the arguments are bogus...
        argsCheck( _hapEnum, _initialDelay );
        if( (_delay == null) || (_delay.isNegative()) )
            throw new IllegalArgumentException( "Delay is null or invalid" );

        return scheduler.scheduleWithFixedDelay( () -> post( _hapEnum, _data ), _initialDelay, _delay );
    }



    /**
     * Schedule posting of the Hap with the given Hap enum and associated data first after the given initial delay, and subsequently with the given
     * delay from the completion of one scheduled posting to the next scheduled posting.  The associated data is obtained by calling the given
     * {@link Supplier} when each of the scheduled Haps is posted.  The average rate over time will be somewhat less than the rate defined by the
     * given delay because of the execution time of each scheduled posting and the delays within the scheduler.  This method will not cause bursts of
     * postings when the scheduler's thread free up after a busy period.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _supplier The {@link Supplier} to be called on each Hap posting to obtain the associated data.
     * @param _initialDelay The {@link Duration} delay before the first scheduled Hap is posted.
     * @param _delay The delay between scheduled Hap postings.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> scheduleWithFixedDelayWithSupplier( final E _hapEnum, final Supplier<Object> _supplier,
                                                                  final Duration _initialDelay, final Duration _delay ) {

        // fail fast if the arguments are bogus...
        argsCheck( _hapEnum, _initialDelay );
        if( (_delay == null) || (_delay.isNegative()) )
            throw new IllegalArgumentException( "Delay is null or invalid" );
        if( _supplier == null )
            throw new IllegalArgumentException( "Missing supplier" );

        return scheduler.scheduleWithFixedDelay( () -> post( _hapEnum, _supplier.get() ), _initialDelay, _delay );
    }


    /**
     * Schedule posting of the Hap with the given Hap enum with no associated data first after the given initial delay, and subsequently with the
     * given delay from the completion of one scheduled posting to the next scheduled posting.  The average rate over time will be somewhat less than
     * the rate defined by the given delay because of the execution time of each scheduled posting and the delays within the scheduler.  This method
     * will not cause bursts of postings when the scheduler's thread free up after a busy period.
     *
     * @param _hapEnum The enum indicating what kind of Hap is to be posted.
     * @param _initialDelay The {@link Duration} delay before the first scheduled Hap is posted.
     * @param _delay The delay between scheduled Hap postings.
     * @return A {@link ScheduledFuture} that allows this scheduled Hap to be cancelled.
     * @throws IllegalStateException if scheduling is not enabled
     */
    public ScheduledFuture<?> scheduleWithFixedDelay( final E _hapEnum, final Duration _initialDelay, final Duration _delay ) {
        return scheduleWithFixedDelay( _hapEnum, null, _initialDelay, _delay );
    }


        /**
         * The {@link Runnable} functional implementation that runs in this class' {@link ExecutorService}.  It blocks until a {@link Hap} is available
         * in the queue, then removes that Hap and handles it.  System Haps are detected by their null Hap enum value; they are handled separately.  All
         * other Haps are dispatched to all their listeners.
         */
    private void dispatcher() {

        try {

            // hopefully we stay in this loop forever...
            while( !Thread.currentThread().isInterrupted() ) {

                // wait for something to show up on our queue...
                Hap<E> hap = queue.take();

                // if it's a system Hap, handle it...
                if( hap.hap == null )
                    handleSystemHap( hap );

                // otherwise, dispatch it...
                else
                    handleNormalHap( hap );
            }
        }
        catch( InterruptedException _e ) {
            // naught to do; we just leave...
        }
    }


    /**
     * <p>Handles the dispatching of a normal Hap to its listeners.  This method looks up the list of {@link HapsData} records for the given Hap in the
     * dispatch table; each of those records describes a subscriber (including the listener).  Each listener on that list is called.  The kind of
     * listener depends on the type of subscription, which is described by the subscription type field in the HapsData record.  This method does
     * an unchecked cast of the listener to the correct type, based on that subscription type.  Any changes to this logic must be done with great
     * care to ensure the integrity of this little "trick".</p>
     * <p>Any unhandled exceptions thrown by listeners are caught, logged, and otherwise ignored.</p>
     *
     * @param hap The normal Hap to handle.
     */
    @SuppressWarnings( "unchecked" )
    private void handleNormalHap( final Hap<E> hap ) {

        try {
            // for each of our subscribers...
            dispatch.get( hap.hap.ordinal() ).forEach( (data) -> {

                // be very, very careful if modifying this - we're making unchecked casts based on the value of the enum we stored in the
                // subscribe method - if this gets messed up, things will go south in a hurry...
                switch( data.subscriptionType ) {
                    case ACTION:  ((Runnable)              data.listener).run();                       break;
                    case HAP:     ((Consumer<E>)           data.listener).accept( hap.hap );           break;
                    case DATA:    ((Consumer<Object>)      data.listener).accept( hap.data );          break;
                    case HAPDATA: ((BiConsumer<E, Object>) data.listener).accept( hap.hap, hap.data ); break;
                }
            } );
        }

        // we catch every exception here, log it, and then keep on going...
        catch( Exception _exception ) {
            LOGGER.log( Level.SEVERE, "Unhandled exception in Hap listener", _exception );
        }
    }


    /**
     * Handle a system Hap, for subscribing or unsubscribing.
     *
     * @param _hap The system Hap to handle.
     */
    private void handleSystemHap( final Hap<E> _hap ) {

        // get our system data...
        HapsData<E> data = cast( _hap.data );

        // get the list of HapsData records with the current subscribers to this Hap...
        List<HapsData<E>> subscribers = dispatch.get( data.hapEnum.ordinal() );

        // do whatever we're supposed to do...
        switch( data.action ) {

            case SUBSCRIBE:
                subscribers.add( data );     // add our new subscriber...
                break;

            case UNSUBSCRIBE:
                subscribers.remove( data );  // remove this subscriber...
                break;
        }
    }


    /**
     * Make an unchecked cast of the data {@link Object} from a system Hap to a {@link HapsData} (which it had better be!).  This is in a method only
     * so that we can annotate it to suppress the unchecked warning.
     *
     * @param _data The data Object to cast.
     * @return the HapsData instance that needed to be cast
     */
    @SuppressWarnings( "unchecked" )
    private HapsData<E> cast( Object _data ) {
        return (HapsData<E>) _data;
    }


    /**
     * The kinds of actions that can be described in a {@link HapsData} instance.
     */
    private enum HapsAction { SUBSCRIBE, UNSUBSCRIBE }


    /**
     * The kinds of subscriptions that can be described in a {@link HapsData} instance.
     */
    private enum SubscriptionType { DATA, HAP, HAPDATA, ACTION }


    /**
     * A record that describes a subscription and the action being taken (subscribe or unsubscribe).  The {@link #equals(Object)} and
     * {@link #hashCode()} implementations deliberately exclude the {@link #action} field so that a record describing an unsubscribe will match
     * the record describing the subscription that is stored in the dispatch table.
     */
    private static class HapsData<E> {

        private final HapsAction       action;              // subscribe or unsubscribe...
        private final SubscriptionType subscriptionType;    // the type of subscription, set by the subscribe() methods...
        private final E                hapEnum;             // the enum defining the type of Hap...
        private final Object           listener;            // one of four types; is cast to the right one in handleNormalHap()...


        /**
         * Create a new instance of this class with the given values...
         *
         * @param _action The action to take (subscribe or unsubscribe).
         * @param _subscriptionType The type of subscription (to the Hap enum, the associated data, neither, or both).
         * @param _hapEnum The enum defining the type of Hap.
         * @param _listener The listener, which may be any of four types depending on the subscription type.
         */
        private HapsData( final HapsAction _action, final SubscriptionType _subscriptionType, final E _hapEnum, final Object _listener ) {
            action           = _action;
            subscriptionType = _subscriptionType;
            hapEnum          = _hapEnum;
            listener         = _listener;
        }


        /**
         * Returns a new instance of this class that is the same as this instance, except that the action in the new instance is unsubscribe.
         *
         * @return the new {@link HapsData} instance
         */
        private HapsData<E> toUnsubscribe() {
            return new HapsData<>( HapsAction.UNSUBSCRIBE, subscriptionType, hapEnum, listener );
        }


        /**
         * Returns {@code true} if this instance is equal to the given {@link Object}.  Note that the action field is deliberately ignored.  The
         * listener field (which is a lambda) <i>is</i> included, when ordinarily lambda objects cannot be compared (as their equals(Object)
         * methods are not implemented.  It works in this case because we are always comparing the same instance of the lambda, and the default
         * {@link Object#equals(Object)} method works fine for that.
         *
         * @param _o The object to compare to this object.
         * @return {@code true} if this instance is equal to the given {@link Object}
         */
        @Override
        public boolean equals( final Object _o ) {
            if( this == _o ) return true;
            if( _o == null || getClass() != _o.getClass() ) return false;
            HapsData<?> hapsData = (HapsData<?>) _o;
            return subscriptionType == hapsData.subscriptionType && hapEnum.equals( hapsData.hapEnum ) && listener.equals( hapsData.listener );
        }


        /**
         * The hash code for this object.
         *
         * @return the hash code for this object
         */
        @Override
        public int hashCode() {
            return Objects.hash( subscriptionType, hapEnum, listener );
        }
    }


    /**
     * Returns a list containing all of the Hap enum values.
     *
     * @param _sample An example Hap enum.  The concrete example is necessary so that this method can use the {@code _event.getClass()} method,
     *               which is not available from just the type name.
     * @return a list containing all of the Hap enum values
     */
    @SuppressWarnings( "unchecked" )
    private List<E> getHapEnums( final E _sample ) {
        return Arrays.asList( ((Class<E>) _sample.getClass()).getEnumConstants() );
    }


    /**
     * Return a new instance of a system Hap (meaning a Hap with a null Hap enum) with the given {@link HapsData} instance as its associated data.
     *
     * @param _data The HapsData record for this Hap.
     * @return the new instance of a system Hap
     */
    private Hap<E> getSystemHap( final HapsData<E> _data ) {
        return new Hap<>( null, _data );
    }


    /**
     * Return an instance of a {@link Hap} with the given Hap enum defining the type of Hap, and the optional associated data (which may be
     * {@code null}).  If the associated data is not {@code null}, then a new Hap instance is created.  If the associated data <i>is</i> {@code null},
     * then a cached (and reusable) Hap instance is returned.  The cache of reusable Haps is created in {@link Haps()}.
     *
     * @param _hapEnum The enum defining what type of Hap is needed.
     * @param _data The optional data associated with this Hap.
     * @return the instance of Hap
     */
    private Hap<E> getHap( final E _hapEnum, final Object _data ) {

        if( _hapEnum == null )
            throw new IllegalArgumentException( "Missing Hap enum" );

        return (_data == null) ? hapCache.get( _hapEnum.ordinal() ) : new Hap<>( _hapEnum, _data );
    }


    /**
     * Return a cached (and reusable) Hap instance is returned.  The cache of reusable Haps is created in {@link Haps()}.
     *
     * @param _hapEnum The enum defining what type of Hap is needed.
     * @return the instance of Hap
     */
    private Hap<E> getHap( final E _hapEnum ) {
        return hapCache.get( _hapEnum.ordinal() );
    }


    /**
     * Instances of this class define a Hap that can be queued and dispatched.
     *
     * @author Tom Dilatush  tom@dilatush.com
     */
    private static class Hap<E extends Enum<E>> {

        private final E hap;       // the enum defining what type of Hap this is (null for system Hap)...
        private final Object data; // the optional data associated with this Hap...


        /**
         * Create a new instance of Hap with the given values.
         *
         * @param _hap The enum defining what type of Hap this is (null for system Hap).
         * @param _data The optional data associated with this Hap.
         */
        private Hap( final E _hap, final Object _data ) {
            hap = _hap;
            data = _data;
        }
    }
}
