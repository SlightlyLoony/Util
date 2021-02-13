package com.dilatush.util.info;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Instances of this class provide public read-only access to a contained information source that can be privately set.  The contained information
 * source could be an instance of either {@link InfoSource} or {@link InfoView}, which means that these views can be "chained" to any depth, in effect
 * allowing proxying of information from a source at lower, private levels of a class hierarchy to a higher, public level, while still being safe
 * from accidental corruption.  Instances of this class are typically {@code public final} fields on the class that publicly exposes them.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class InfoView<T> implements Info<T> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "MMM dd, uuuu hh:mm:ss.SSS" );

    private Info<T> provider;


    /**
     * Create a new instance of this class with the given immutable information provider.
     *
     * @param _provider The information provider for this instance.
     */
    public InfoView( final Info<T> _provider ) {
        provider = _provider;
    }


    /**
     * Create a new instance of this class that will report itself as unavailable until {@link #setSource} is called, using the given "gadget" to
     * provide a lambda to the caller that can be used to set the {@link Info} contained in this instance.  An example:
     * <pre>{@code
     *         AtomicReference<Consumer<Info<String>>> setMyStringInfo = new AtomicReference<>();
     *         InfoView<String> myStringInfo = new InfoView<>( setMyStringInfo::set );
     *         setMyStringInfo.get().accept( new InfoSource<>( "Test!" ) );
     * }</pre>
     * The first line creates an effectively-final variable to hold a {@code Consumer<Info<String>>}, which acts as a setter for the {@code InfoView}
     * instance.  The second line creates the {@code InfoView} instance, with the argument {@code setMyStringInfo::set} method reference calling the
     * setter on the {@code AtomicReference} to set the setter.  The third line shows using that setter to set the information source in the
     * {@code InfoView} instance to a new value.  That's a lot harder to describe than it is to write the actual code that uses it!
     *
     * @param _gadget The "gadget" that sets the caller's setter.
     */
    public InfoView( final Consumer<Consumer<Info<T>>> _gadget ) {
        provider = new InfoSource<>( null );  // make an unavailable provider to start with...
        _gadget.accept( this::setSource );
    }


    /**
     * Create a new instance of this class that will report itself as unavailable until {@link #setSource} is called, using the given "gadget" to
     * provide a lambda to the caller that can be used to set the information contained in this instance.  An example:
     * <pre>{@code
     *         AtomicReference<Consumer<String>> setMyOtherStringInfo = new AtomicReference<>();
     *         InfoView<String> myOtherStringInfo = new InfoView<>( setMyOtherStringInfo::set, false );
     *         setMyOtherStringInfo.get().accept( "Another test!" );
     * }</pre>
     * The first line creates an effectively-final variable to hold a {@code Consumer<Info<String>>}, which acts as a setter for the {@code InfoView}
     * instance.  The second line creates the {@code InfoView} instance, with the argument {@code setMyStringInfo::set} method reference calling the
     * setter on the {@code AtomicReference} to set the setter.  The third line shows using that setter to set the information source in the
     * {@code InfoView} instance to a new instance of {@link InfoSource} with the given information.  That's a lot harder to describe than it is to
     * write the actual code that uses it!
     *
     * @param _gadget The "gadget" that sets the caller's setter.
     * @param _signature This value is ignored; it's here only to allow this constructor to have a different signature than the other one.
     */
    @SuppressWarnings( "unused" )
    public InfoView( final Consumer<Consumer<T>> _gadget, boolean _signature ) {
        provider = new InfoSource<>( null );  // make an unavailable provider to start with...
        _gadget.accept( this::setInfo );

    }


    /**
     * Sets the {@link Info} provider to the given provider.  This method is only called through a "gadget" supplied by a constructor.
     *
     * @param _provider The {@link Info} provider to set as the provider for this instance.
     */
    private void setSource( final Info<T> _provider ) {
        provider = _provider;
    }


    /**
     * Sets the provider to a new instance of {@link InfoSource} with the given information.  This method is only called through a "gadget" supplied
     * by a constructor.
     *
     * @param _info The information for a new instance of {@link InfoSource} to set as the provider for this instance.
     */
    private void setInfo( final T _info ) {
        provider = new InfoSource<>( _info );
    }


    /**
     * Returns the information held by this information provider, or {@code null} if none is available.  It is possible that the information returned
     * by this method is from a different record than the timestamp returned by {@link #getInfoTimestamp}.  To get the information with the correct
     * timestamp, use {@link #getInfoSource} to get the {@link InfoSource} instance, then use {@code getInfo()} and {@code getInfoTimestamp()} on it.
     *
     * @return the information held by this information source
     */
    @Override
    public T getInfo() {
        return provider.getInfo();
    }


    /**
     * Returns the timestamp for the information held by this information provider, which is the time that the information was recorded.  Note that a
     * timestamp will be returned even if there is no information available, in which case it is the time that the unavailability was recorded.  It is
     * possible that the timestamp returned by his method is from a different record than the information returned by {@link #getInfo}.  To get the
     * timestamp with the correct information, use {@link #getInfoSource} to get the {@link InfoSource} instance, then use {@code getInfo()} and
     * {@code getInfoTimestamp()} on it.
     *
     * @return the timestamp for the
     */
    @Override
    public Instant getInfoTimestamp() {
        return provider.getInfoTimestamp();
    }


    /**
     * Returns the {@link InfoSource} instance that is the source of information for this provider.
     *
     * @return the {@link InfoSource} instance that is the source of information for this provider
     */
    @Override
    public InfoSource<T> getInfoSource() {
        return provider.getInfoSource();
    }


    /**
     * Returns {@code true} if information is available from this information source.
     *
     * @return {@code true} if information is available from this information source
     */
    @Override
    public boolean isInfoAvailable() {
        return provider.isInfoAvailable();
    }


    /**
     * Returns a string representation of this instance.
     *
     * @return a string representation of this instance
     */
    @Override
    public String toString() {
        ZonedDateTime stamp = ZonedDateTime.ofInstant( provider.getInfoTimestamp(), ZoneId.systemDefault() );
        return "InfoView: " + formatter.format( stamp ) + ": " + ((provider.getInfo() == null) ? "null" : provider.getInfo().toString() );
    }
}
