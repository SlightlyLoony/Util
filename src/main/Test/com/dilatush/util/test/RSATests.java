package com.dilatush.util.test;

import com.dilatush.util.RSA;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSATests {

    public static void main( final String[] _args ) {

        // some setup...
        var random = new SecureRandom();

        // get some RSA keys...
        var keysOutcome = RSA.generateKeys( random, 2048 );
        if( keysOutcome.notOk() )
            throw new IllegalStateException( keysOutcome.msg() );
        var keys = keysOutcome.info();

        // make sure the public and private exponents are inverses...
        var prod = keys.privateKey().dEncrypting().multiply( keys.publicKey().eEncrypting() ).mod( keys.privateKey().t() );
        if( prod.compareTo( BigInteger.ONE ) != 0 )
            throw new IllegalStateException( "Encrypting exponents not inverse" );
        prod = keys.privateKey().dSigning().multiply( keys.publicKey().eSigning() ).mod( keys.privateKey().t() );
        if( prod.compareTo( BigInteger.ONE ) != 0 )
            throw new IllegalStateException( "Signing exponents not inverse" );

        // try encrypting and decrypting...
        for( int i = 0; i < 10000; i++ ) {

            // first with public key encryption...
            var plainText = RSA.getRandomPlainText( random, keys.publicKey() );
            var cipherText = RSA.encrypt( keys.publicKey(), plainText );
            var decryptedText = RSA.decrypt( keys.privateKey(), cipherText );
            var goodDecrypt = (plainText.compareTo( decryptedText ) == 0);
            if( !goodDecrypt )
                plainText.hashCode();

            // then with private key encryption...
            cipherText = RSA.encrypt( keys.privateKey(), plainText );
            decryptedText = RSA.decrypt( keys.publicKey(), cipherText );
            goodDecrypt = (plainText.compareTo( decryptedText ) == 0);
            if( !goodDecrypt )
                plainText.hashCode();
        }

        new Object().hashCode();
    }
}
