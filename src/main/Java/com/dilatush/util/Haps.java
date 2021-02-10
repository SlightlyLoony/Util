package com.dilatush.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
    private final List<List<HapsData<E>>>    dispatch;

    // the queue of not-yet-dispatched Haps...
    private final ArrayBlockingQueue<Hap<E>>   queue;

    // our cache of reusable Haps (those with no data)...
    private final List<Hap<E>>                 hapCache;


    /**
     * Create a new instance of this class with the given maximum size for the queue of not-yet-dispatched Haps, and the given sample of an enum,
     * which must be of the same type as that specified for the generic type.  In other words, if the enum class you use to specify the types of
     * Haps is named {@code MyHaps}, then your Haps instance will have a type of {@code Haps<MyHaps>}, and the sample must be an instance of
     * {@code MyHaps}.  This sample is required because {@code Haps} needs get the {@code Class} object for the Haps enum class, and it cannot do this
     * without an actual instance of the enum.
     *
     * @param _maxQueue The maximum size for the queue of not-yet-dispatched Haps.
     * @param _sample The sample Hap enum.
     */
    public Haps( final int _maxQueue, final E _sample ) {

        // fail fast if we're missing important stuff...
        if( _maxQueue < 10 )
            throw new IllegalArgumentException( "Invalid maximum queue size; must be at least 10" );
        if( _sample == null )
            throw new IllegalArgumentException( "Missing sample Hap enum" );

        // initialize our dispatch lists...
        List<E> haps = getHapEnums( _sample );
        dispatch = new ArrayList<>( haps.size() );
        haps.forEach( (hapEnum) -> {
            dispatch.add( new ArrayList<>( 25 ) );  // add an array pre-sized for 10 subscribers (almost always plenty, but will resize if needed)...
        } );

        // build our cache of no-data Haps...
        hapCache = new ArrayList<>( haps.size() );
        haps.forEach( ( hapEnum) -> hapCache.add( new Hap<>( hapEnum, null ) ) );

        // set up our Hap queue...
        queue = new ArrayBlockingQueue<>( _maxQueue );

        // start up our executor and get our dispatcher going...
        new ExecutorService( 1 ).submit( this::dispatcher );
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
     * Subscribe to the given Hap with a listener that receives both the Hap and its associated data.  This kind of subscription is mainly useful if
     * for some reason you have a single listener for multiple kinds of Haps.  We wish you wouldn't subscribe a single listener to multiple kinds of
     * Haps, but if you really want to, you can.  Yes, we're being judgemental here.
     *
     * @param _hapEnum The Hap to subscribe to.
     * @param _listener The {@code BiConsumer<Hap,Object>} that listens for the given Hap.
     * @return an opaque object (a handle) that must be used if unsubscribing from this subscription
     */
    public Object subscribeToHapAndData( final E _hapEnum, final BiConsumer<E, Object> _listener ) {
        return subscribeImpl( SubscriptionType.HAPDATA, _hapEnum, _listener );
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
     * Subscribe to the given Hap with a listener that receives just the Hap (and not its associated data).  This kind of subscription is mainly
     * useful if for some reason you have a single listener for multiple kinds of Haps, and those Haps either have no associated data or it makes
     * no difference what that data is.  We wish you wouldn't subscribe a single listener to multiple kinds of Haps, but if you really want to, you
     * can.  Yes, we're being judgemental here.
     *
     * @param _hapEnum The Hap to subscribe to.
     * @param _listener The {@code Consumer<Hap>} that listens for the given Hap.
     * @return an opaque object (a handle) that must be used if unsubscribing from this subscription
     */
    public Object subscribeToHap( final E _hapEnum, final Consumer<E> _listener ) {
        return subscribeImpl( SubscriptionType.HAP, _hapEnum, _listener );
    }


    /**
     * Subscribe to the given Hap with a listener that receives just the Hap's associated data (and not the Hap itself).  Note that it is possible
     * for the associated data to be {@code null}.  This kind of subscription is most useful for Haps used to publish some kind of information.
     *
     * @param _hapEnum The Hap to subscribe to.
     * @param _listener The {@code Consumer<Object>} that listens for the given Hap.
     * @return an opaque object (a handle) that must be used if unsubscribing from this subscription
     */
    public Object subscribeToData( final E _hapEnum, final Consumer<Object> _listener ) {
        return subscribeImpl( SubscriptionType.DATA, _hapEnum, _listener );
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

        // get our system data...
        HapsData<E> data = cast( _handle );

        // post our system event...
        queue.add( getSystemHap( data.toUnsubscribe() ) );
    }


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
                    dispatch( hap );
            }
        }
        catch( InterruptedException _e ) {
            // naught to do; we just leave...
        }
    }


    @SuppressWarnings( "unchecked" )
    private void dispatch( final Hap<E> hap ) {

        try {
            // for each of our subscribers...
            dispatch.get( hap.hap.ordinal() ).forEach( (data) -> {

                // be very, very careful if modifying this - we're making unchecked casts based on the value of the enum we stored in the
                // subscribe method - if this gets messed up, things will go south in a hurry...
                switch( data.subscriptionType ) {
                    case ACTION:  ((Runnable)              data.listener).run();                       break;
                    case HAP:     ((Consumer<E>)           data.listener).accept( hap.hap );           break;
                    case DATA:    ((Consumer<Object>)      data.listener).accept( hap.hap );           break;
                    case HAPDATA: ((BiConsumer<E, Object>) data.listener).accept( hap.hap, hap.data ); break;
                }
            } );
        }
        catch( Exception _exception ) {
            LOGGER.log( Level.SEVERE, "Unhandled exception in Hap listener", _exception );
        }
    }


    private void handleSystemHap( final Hap<E> _hap ) {

        // get our system data...
        HapsData<E> data = cast( _hap.data );

        // do whatever we're supposed to do...
        switch( data.action ) {

            case SUBSCRIBE:
                dispatch.get( data.hapEnum.ordinal() ).add( data );
                break;

            case UNSUBSCRIBE:
                dispatch.get( data.hapEnum.ordinal() ).remove( data );
                break;
        }
    }


    @SuppressWarnings( "unchecked" )
    private HapsData<E> cast( Object _data ) {
        return (HapsData<E>) _data;
    }


    private enum HapsAction { SUBSCRIBE, UNSUBSCRIBE }
    
    private enum SubscriptionType { DATA, HAP, HAPDATA, ACTION }


    private static class HapsData<E> {

        private final HapsAction       action;
        private final SubscriptionType subscriptionType;
        private final E                hapEnum;
        private final Object           listener;  // actually either an instance of BiConsumer<E, Object> or of Consumer<E>...


        private HapsData( final HapsAction _action, final SubscriptionType _subscriptionType, final E _hapEnum, final Object _listener ) {
            action           = _action;
            subscriptionType = _subscriptionType;
            hapEnum          = _hapEnum;
            listener         = _listener;
        }

        private HapsData<E> toUnsubscribe() {
            return new HapsData<>( HapsAction.UNSUBSCRIBE, subscriptionType, hapEnum, listener );
        }


        @Override
        public boolean equals( final Object _o ) {
            if( this == _o ) return true;
            if( _o == null || getClass() != _o.getClass() ) return false;
            HapsData<?> hapsData = (HapsData<?>) _o;
            return subscriptionType == hapsData.subscriptionType && hapEnum.equals( hapsData.hapEnum ) && listener.equals( hapsData.listener );
        }


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


    private Hap<E> getSystemHap( final HapsData<E> _data ) {
        return new Hap<>( null, _data );
    }


    private Hap<E> getHap( final E _hapEnum, final Object _data ) {

        if( _hapEnum == null )
            throw new IllegalArgumentException( "Missing Hap enum" );

        return (_data == null) ? hapCache.get( _hapEnum.ordinal() ) : new Hap<>( _hapEnum, _data );
    }


    private Hap<E> getHap( final E _hapEnum ) {
        return hapCache.get( _hapEnum.ordinal() );
    }


    /**
     * @author Tom Dilatush  tom@dilatush.com
     */
    private static class Hap<E extends Enum<E>> {

        private final E hap;
        private final Object data;

        private Hap( final E _hap, final Object _data ) {
            hap = _hap;
            data = _data;
        }
    }
}
