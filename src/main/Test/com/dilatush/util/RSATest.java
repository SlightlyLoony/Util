package com.dilatush.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RSATest {

    private static int KEY_BIT_LENGTH    = 2400;    // bits in RSA modulus...
    private static int ENCRYPT_TEST_RUNS = 10000;   // number of runs for encryption/decryption tests...

    private static SecureRandom   random;
    private static RSA.RSAKeyPair keys;


    // setup, run before all tests...
    @BeforeAll
    static void generateTestKeys() {

        // create a source of randomness for use in all the tests...
        random = new SecureRandom();

        // create a set of RSA keys for use in all the tests...
        var keysOutcome = RSA.generateKeys( random, KEY_BIT_LENGTH );
        assertTrue( keysOutcome.ok(), "failed to generate RSA keys: " + keysOutcome.msg() );
        keys = keysOutcome.info();
    }


    // test RSAPrivateKey record...
    @Test
    void privateKey() {

        // create an RSA private key with small (but nonsense) values...
        var n = new BigInteger( "21" );
        var p = new BigInteger( "7" );
        var q = new BigInteger( "3" );
        var t = new BigInteger( "6" );
        var de = new BigInteger( "5" );
        var ds = new BigInteger( "11" );
        var priKey1 = new RSA.RSAPrivateKey( new CompositeModuloMath.CompositeIntegerModulus( n, p, q ), t, de, ds );

        // get the string form of the private key, and verify it...
        var ts = priKey1.toString();
        assertEquals( "p:Bw;q:Aw;dE:BQ;dS:Cw;", ts,   "RSAPrivateKey unexpected string value: " + ts );

        // create a new private key from the string form of the first one, and verify that it's the same...
        var priKey2Outcome = RSA.RSAPrivateKey.fromString( ts );
        assertTrue(   priKey2Outcome.ok(),            "string -> RSAPrivateKey not ok: " + priKey2Outcome.msg() );
        assertEquals( priKey1, priKey2Outcome.info(), "RSAPrivateKey -> string -> RSAPrivateKey failed" );
    }


    // test RSAPublicKey record...
    @Test
    void publicKey() {

        // create an RSA public key with small (but nonsense) values...
        var n = new BigInteger( "21" );
        var ee = new BigInteger( "5" );
        var es = new BigInteger( "11" );
        var pubKey1 = new RSA.RSAPublicKey( n, ee, es );

        // get the string form of the public key, and verify it...
        var ts = pubKey1.toString();
        assertEquals( "n:FQ;eE:BQ;eS:Cw;", ts,   "RSAPublicKey unexpected string value: " + ts );

        // create a new public key from the string form of the first one, and verify that it's the same...
        var pubKey2Outcome = RSA.RSAPublicKey.fromString( ts );
        assertTrue(   pubKey2Outcome.ok(),            "string -> RSAPublicKey not ok: " + pubKey2Outcome.msg() );
        assertEquals( pubKey1, pubKey2Outcome.info(), "RSAPublicKey -> string -> RSAPublicKey failed" );
    }


    // test encrypt( RSAPublicKey, BigInteger ) and decrypt( RSAPrivateKey, BigInteger )...
    @Test
    void encrypt1() {

        // loop until we've run all our tests...
        for( int i = 0; i < ENCRYPT_TEST_RUNS; i++ ) {

            // get a random number to test...
            var testNum = RSA.getRandomPlainText( random, keys.publicKey() );

            // encrypt the test number...
            var cipherText = RSA.encrypt( keys.publicKey(), testNum );

            // decrypt the encrypted test number...
            var plainText = RSA.decrypt( keys.privateKey(), cipherText );

            // verify that the encrypt/decrypt operations produced the original test number...
            assertEquals( testNum, plainText, "result of encrypt/decrypt is not the same as the original test value" );
        }
    }


    // test encrypt( RSAPrivateKey, BigInteger ) and decrypt( RSAPublicKey, BigInteger )...
    @Test
    void encrypt2() {

        // loop until we've run all our tests...
        for( int i = 0; i < ENCRYPT_TEST_RUNS; i++ ) {

            // get a random number to test...
            var testNum = RSA.getRandomPlainText( random, keys.privateKey() );

            // encrypt the test number...
            var cipherText = RSA.encrypt( keys.privateKey(), testNum );

            // decrypt the encrypted test number...
            var plainText = RSA.decrypt( keys.publicKey(), cipherText );

            // verify that the encrypt/decrypt operations produced the original test number...
            assertEquals( testNum, plainText, "result of encrypt/decrypt is not the same as the original test value" );
        }

    }
}