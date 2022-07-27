package com.dilatush.test;

import com.dilatush.util.CompositeModuloMath;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class CompositeModuloMathTests {

    public static void main( String[] args ) {

        // parameters...
        int bits = 8000;  // number of bits in results...
        int ops  = 1000;  // number of operations to do...
        int its  = 100;   // number of iterations to do...

        // get a source of randomness...
        var sr = new SecureRandom();

        // set our modulus...
        BigInteger p = BigInteger.probablePrime( bits / 2, sr );
        BigInteger q = BigInteger.probablePrime( bits / 2, sr );
        BigInteger n = p.multiply( q );
        CompositeModuloMath.CompositeIntegerModulus m = new CompositeModuloMath.CompositeIntegerModulus( n, p, q );

        // generate random BigIntegers of the size we need...
        var rbi = new BigInteger[ops + 1];
        for( int i = 0; i < rbi.length; i++ ) {
            rbi[i] = new BigInteger( bits, sr );
        }

        // warm up and check multiply validity...
        var stdResult = multiplyStd( m, rbi );
        var crtResult = multiplyCRT( m, rbi );
        if( Arrays.equals( stdResult, crtResult ) )
            out( "Multiply results are equal" );
        else {
            out( "Multiply results are NOT equal" );
            return;
        }

        // run timing test on multiply...
        long t1 = System.currentTimeMillis();
        for( int i = 0; i < its; i++ ) {
            multiplyStd( m, rbi );
        }
        long t2 = System.currentTimeMillis();
        for( int i = 0; i < its; i++ ) {
            multiplyCRT( m, rbi );
        }
        long t3 = System.currentTimeMillis();
        out( "Multiply times: standard " + (t2 - t1) + "ms, CRT " + (t3 - t2) + "ms." );


        sr.hashCode();
    }


    private static void out( String _m ) {
        System.out.println( _m );
    }


    private static BigInteger[] multiplyStd( CompositeModuloMath.CompositeIntegerModulus _m, BigInteger[] _rbi ) {
        var result = new BigInteger[ _rbi.length - 1];
        for( int i = 0; i < result.length; i++ ) {
            result[i] = _rbi[i].multiply( _rbi[i + 1] ).mod( _m.n() );
        }
        return result;
    }


    private static BigInteger[] multiplyCRT( CompositeModuloMath.CompositeIntegerModulus _m, BigInteger[] _rbi ) {
        var result = new BigInteger[ _rbi.length - 1];
        for( int i = 0; i < result.length; i++ ) {
            result[i] = CompositeModuloMath.multiply( _m, _rbi[i], _rbi[i + 1] );
        }
        return result;
    }
}
