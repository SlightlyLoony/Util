package com.dilatush.test;

import com.dilatush.util.RSA;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSATests {

    public static void main( final String[] _args ) {

        // get some RSA keys...
        var random = new SecureRandom();
        var keys = RSA.generateKeys( random, 2500, 7, 11 );

        // make sure the public and private exponents are inverses...
        var prod = keys.privateKey().dEncrypting().multiply( keys.publicKey().eEncrypting() ).mod( keys.privateKey().t() );
        if( prod.compareTo( BigInteger.ONE ) != 0 )
            throw new IllegalStateException( "Encrypting exponents not inverse" );
        prod = keys.privateKey().dSigning().multiply( keys.publicKey().eSigning() ).mod( keys.privateKey().t() );
        if( prod.compareTo( BigInteger.ONE ) != 0 )
            throw new IllegalStateException( "Signing exponents not inverse" );

        // try encrypting and decrypting...
        var plainText = BigInteger.probablePrime( keys.privateKey().m().n().bitLength(), random );
        var cipherText = RSA.encryptPublic( keys.publicKey(), plainText );
        var decryptedText = RSA.decryptPrivate( keys.privateKey(), cipherText );

        new Object().hashCode();
    }
}
