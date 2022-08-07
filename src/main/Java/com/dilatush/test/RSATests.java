package com.dilatush.test;

import com.dilatush.util.RSA;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSATests {

    public static void main( final String[] _args ) {

        // some setup...
        var random = new SecureRandom();

        // get some RSA keys...
        var keys = RSA.generateKeys( random, 2048 );

        // make sure the public and private exponents are inverses...
        var prod = keys.privateKey().dEncrypting().multiply( keys.publicKey().eEncrypting() ).mod( keys.privateKey().t() );
        if( prod.compareTo( BigInteger.ONE ) != 0 )
            throw new IllegalStateException( "Encrypting exponents not inverse" );
        prod = keys.privateKey().dSigning().multiply( keys.publicKey().eSigning() ).mod( keys.privateKey().t() );
        if( prod.compareTo( BigInteger.ONE ) != 0 )
            throw new IllegalStateException( "Signing exponents not inverse" );

        // try encrypting and decrypting...
        for( int i = 0; i < 10000; i++ ) {
            var plainText = new BigInteger( keys.privateKey().m().n().bitLength(), random ).mod( keys.publicKey().n() );
            var cipherText = RSA.encryptPublic( keys.publicKey(), plainText );
            var decryptedText = RSA.decryptPrivate( keys.privateKey(), cipherText );
            var goodDecrypt = (plainText.compareTo( decryptedText ) == 0);
            if( !goodDecrypt )
                plainText.hashCode();
            System.out.println( plainText );
        }

        new Object().hashCode();
    }
}
