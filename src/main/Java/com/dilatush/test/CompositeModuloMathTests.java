package com.dilatush.test;

import com.dilatush.util.CompositeModuloMath;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CompositeModuloMathTests {

    public static void main( String[] args ) {

        // parameters...
        int bits = 8000;  // number of bits in results...
        int ops  = 100;  // number of operations to do...
        int its  = 1000;   // number of iterations to do...

        // get a source of randomness...
        var sr = new SecureRandom();

        // set our modulus...
        BigInteger p = BigInteger.probablePrime( bits / 2, sr );
        BigInteger q = BigInteger.probablePrime( bits / 2, sr );
        BigInteger n = p.multiply( q );
        CompositeModuloMath.CompositeIntegerModulus m = new CompositeModuloMath.CompositeIntegerModulus( n, p, q );

        // generate random BigIntegers of the size we need...
        var rbi = new BigInteger[ops];
        for( int i = 0; i < rbi.length; i++ ) {
            rbi[i] = new BigInteger( bits, sr );
        }

        // convert them to CRT form...
        var rcrt = new CompositeModuloMath.XCRT[ops];
        for( int i = 0; i < rcrt.length; i++ ) {
            rcrt[i] = new CompositeModuloMath.XCRT( m, rbi[i] );
        }

        // warm up and check multiply validity...
        var stdResult = multiplyStd( m, rbi );
        var crtResult = multiplyCRT( m, rcrt );
        if( stdResult.equals( crtResult ) )
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
            multiplyCRT( m, rcrt );
        }
        long t3 = System.currentTimeMillis();
        out( "Multiply times: standard " + (t2 - t1) + "ms, CRT " + (t3 - t2) + "ms." );


        sr.hashCode();
    }


    private static void out( String _m ) {
        System.out.println( _m );
    }


    private static BigInteger multiplyStd( CompositeModuloMath.CompositeIntegerModulus _m, BigInteger[] _rbi ) {
        var result = BigInteger.ONE;
        for( BigInteger _bigInteger : _rbi ) {
            result = result.multiply( _bigInteger ).mod( _m.n() );
        }
        return result;
    }


    private static BigInteger multiplyCRT( CompositeModuloMath.CompositeIntegerModulus _m, CompositeModuloMath.XCRT[] _rcrt ) {
        var result = new CompositeModuloMath.XCRT( _m, BigInteger.ONE );
        for( CompositeModuloMath.XCRT xcrt : _rcrt ) {
            result = result.multiply( xcrt );
        }
        return result.deCRT();
    }
}
