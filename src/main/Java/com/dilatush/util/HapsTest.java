package com.dilatush.util;

import static java.lang.Thread.sleep;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class HapsTest {

    private enum Hap { YES, NO, TRUE, FALSE }

    public static void r() {
        out( "r" );
    }

    public static void d( final Object _data ) {
        out( "d: " + _data );
    }

    public static void h( final Hap _hap ) {
        out( "h: " + _hap );
    }

    public static void hd( final Hap _hap, final Object _data ) {
        out( "hd: " + _hap + " with " + _data );
    }

    public static void main( final String[] _args ) throws InterruptedException {

        Haps<Hap> haps = new Haps<>( 100, Hap.YES );
        haps.subscribe( Hap.YES, HapsTest::r );
        haps.subscribeToHap( Hap.YES, HapsTest::h );
        haps.subscribeToData( Hap.YES, HapsTest::d );
        Object hd = haps.subscribeToHapAndData( Hap.YES, HapsTest::hd );
        haps.post( Hap.YES, "test" );

        sleep( 1000 );

        haps.unsubscribe( hd );

        sleep( 1000 );
        haps.post( Hap.YES, "test" );
        sleep( 1000 );
    }

    public static void out( final String _msg ) {
        System.out.println( _msg );
    }
}
