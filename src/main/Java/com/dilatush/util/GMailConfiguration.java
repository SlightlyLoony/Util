package com.dilatush.util;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class GMailConfiguration {

    private final String account;
    private final String password;


    public GMailConfiguration( final String _account, final String _password ) {
        account = _account;
        password = _password;
    }


    public String getAccount() {
        return account;
    }


    public String getPassword() {
        return password;
    }
}
