package com.dilatush.util.test;

import com.dilatush.util.BigIntegers;

import java.math.BigInteger;

public class BigIntegersTest {

    public static void main( String[] _args ) {
        rootTests();
    }


    private static void rootTests() {

        var cr = BigIntegers.root( BigInteger.valueOf( 3343574), 25 );
        cr.hashCode();
    }
}
