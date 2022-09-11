package com.dilatush.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RSATest {

    @Test
    void privateKey() {

        var n = new BigInteger( "21" );
        var p = new BigInteger( "7" );
        var q = new BigInteger( "3" );
        var t = new BigInteger( "6" );
        var de = new BigInteger( "5" );
        var ds = new BigInteger( "11" );
        var priKey1 = new RSA.RSAPrivateKey( new CompositeModuloMath.CompositeIntegerModulus( n, p, q ), t, de, ds );
        var ts = priKey1.toString();
        assertEquals( "p:Bw;q:Aw;dE:BQ;dS:Cw;", ts,   "RSAPrivateKey unexpected string value: " + ts );
        var priKey2Outcome = RSA.RSAPrivateKey.fromString( ts );
        assertTrue(   priKey2Outcome.ok(),            "string -> RSAPrivateKey not ok: " + priKey2Outcome.msg() );
        assertEquals( priKey1, priKey2Outcome.info(), "RSAPrivateKey -> string -> RSAPrivateKey failed" );
    }


    @Test
    void publicKey() {

        var n = new BigInteger( "21" );
        var ee = new BigInteger( "5" );
        var es = new BigInteger( "11" );
        var pubKey1 = new RSA.RSAPublicKey( n, ee, es );

        var ts = pubKey1.toString();
        assertEquals( "n:FQ;eE:BQ;eS:Cw;", ts,   "RSAPublicKey unexpected string value: " + ts );
        var pubKey2Outcome = RSA.RSAPublicKey.fromString( ts );
        assertTrue(   pubKey2Outcome.ok(),            "string -> RSAPublicKey not ok: " + pubKey2Outcome.msg() );
        assertEquals( pubKey1, pubKey2Outcome.info(), "RSAPublicKey -> string -> RSAPublicKey failed" );
    }
}