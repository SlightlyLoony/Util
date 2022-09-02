package com.dilatush.util.test;

import com.dilatush.util.RSA;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSATests {

    public static void main( final String[] _args ) {

        // some setup...
        var random = new SecureRandom();

        // mask generator function test...
        var inBytes = new byte[32];  // the value we want to hash...
        random.nextBytes( inBytes );
        var mask = RSA.mask( inBytes, 100 );

        // get some RSA keys...
        var keysOutcome = RSA.generateKeys( random, 2000 );
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
            System.out.print( i + "," );

            // first with public key encryption...
            var plainText = RSA.getRandomPlainText( random, keys.publicKey() );
            var cipherText = RSA.encrypt( keys.publicKey(), plainText );
            var decryptedText = RSA.decrypt( keys.privateKey(), cipherText );
            var goodDecrypt = (plainText.compareTo( decryptedText ) == 0);
            if( !goodDecrypt )
                throw new IllegalStateException( "Public key encryption/private key decryption failure" );

            // then with private key encryption...
            cipherText = RSA.encrypt( keys.privateKey(), plainText );
            decryptedText = RSA.decrypt( keys.publicKey(), cipherText );
            goodDecrypt = (plainText.compareTo( decryptedText ) == 0);
            if( !goodDecrypt )
                throw new IllegalStateException( "Private key encryption/public key decryption failure" );
        }

        new Object().hashCode();
    }
}
