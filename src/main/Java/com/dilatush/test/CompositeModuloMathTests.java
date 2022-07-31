package com.dilatush.test;

import com.dilatush.util.CompositeModuloMath;

import java.math.BigInteger;
import java.security.SecureRandom;

import static java.math.BigInteger.ONE;

public class CompositeModuloMathTests {

    public static void main( String[] args ) {

        // exponentiation test parameters...
        int e_bits = 4000;  // number of bits in the base and the exponent...
        int e_ops =  100;   // number of exponentiation operations to do on each iteration...
        int e_its =  1;     // number of iterations to do...

        // multiplication test parameters...
        int m_bits = 8000;   // number of bits in results...
        int m_ops  = 100;    // number of operations to do...
        int m_its  = 1000;   // number of iterations to do...

        // get a source of randomness...
        var sr = new SecureRandom();


        // exponentiation tests...

        // set our modulus...
        var p = BigInteger.probablePrime( e_bits / 2, sr );
        var q = BigInteger.probablePrime( e_bits / 2, sr );
        var n = p.multiply( q );
        var m = new CompositeModuloMath.CompositeIntegerModulus( n, p, q );

        // make an array of numbers to exponentiate...
        var exbi = new BigInteger[e_ops+1];
        for( int i = 0; i < exbi.length; i++ ) {
            exbi[i] = new BigInteger( e_bits, sr );
        }

        // warm up and check validity...
        var stdResult = expStd( m, exbi );
        var crtResult = expCRT( m, exbi );
        if( stdResult.equals( crtResult ) )
            out( "Exponentiation results are equal" );
        else {
            out( "Exponentiation results are NOT equal" );
            return;
        }

        // run timing test on exponentiation...
        long t1 = System.currentTimeMillis();
        for( int i = 0; i < e_its; i++ ) {
            expStd( m, exbi );
        }
        long t2 = System.currentTimeMillis();
        for( int i = 0; i < e_its; i++ ) {
            expCRT( m, exbi );
        }
        long t3 = System.currentTimeMillis();
        out( "Exponentiation times: standard " + (t2 - t1) + "ms, CRT " + (t3 - t2) + "ms." );



        // multiply tests...

        // set our modulus...
        p = BigInteger.probablePrime( m_bits / 2, sr );
        q = BigInteger.probablePrime( m_bits / 2, sr );
        n = p.multiply( q );
        m = new CompositeModuloMath.CompositeIntegerModulus( n, p, q );

        // generate random BigIntegers of the size we need...
        var rbi = new BigInteger[m_ops];
        for( int i = 0; i < rbi.length; i++ ) {
            rbi[i] = new BigInteger( m_bits, sr );
        }

        // convert them to CRT form...
        var rcrt = new CompositeModuloMath.XCRT[m_ops];
        for( int i = 0; i < rcrt.length; i++ ) {
            rcrt[i] = new CompositeModuloMath.XCRT( m, rbi[i] );
        }

        // warm up and check multiply validity...
        stdResult = multiplyStd( m, rbi );
        crtResult = multiplyCRT( m, rcrt );
        if( stdResult.equals( crtResult ) )
            out( "Multiply results are equal" );
        else {
            out( "Multiply results are NOT equal" );
            return;
        }

        // run timing test on multiply...
        t1 = System.currentTimeMillis();
        for( int i = 0; i < m_its; i++ ) {
            multiplyStd( m, rbi );
        }
        t2 = System.currentTimeMillis();
        for( int i = 0; i < m_its; i++ ) {
            multiplyCRT( m, rcrt );
        }
        t3 = System.currentTimeMillis();
        out( "Multiply times: standard " + (t2 - t1) + "ms, CRT " + (t3 - t2) + "ms." );


        sr.hashCode();
    }


    private static void out( String _m ) {
        System.out.println( _m );
    }


    private static BigInteger multiplyStd( CompositeModuloMath.CompositeIntegerModulus _m, BigInteger[] _rbi ) {
        var result = ONE;
        for( BigInteger _bigInteger : _rbi ) {
            result = result.multiply( _bigInteger ).mod( _m.n() );
        }
        return result;
    }


    private static BigInteger multiplyCRT( CompositeModuloMath.CompositeIntegerModulus _m, CompositeModuloMath.XCRT[] _rcrt ) {
        var result = new CompositeModuloMath.XCRT( _m, ONE );
        for( CompositeModuloMath.XCRT xcrt : _rcrt ) {
            result = result.multiply( xcrt );
        }
        return result.deCRT();
    }


    private static BigInteger expStd( CompositeModuloMath.CompositeIntegerModulus _m, BigInteger[] _xs ) {
        var result = _xs[0];
        for( int i = 1; i < _xs.length; i++ ) {
            result = result.modPow( _xs[i], _m.n() );
        }
        return result;
    }


    private static BigInteger expCRT( CompositeModuloMath.CompositeIntegerModulus _m, BigInteger[] _xs ) {
        var result = _xs[0];
        for( int i = 1; i < _xs.length; i++ ) {
            result = CompositeModuloMath.pow( result, _xs[i], _m );
        }
        return result;
    }
}
