package com.dilatush.util;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TwilioConfiguration {

    private final String account;
    private final String password;
    private final String phone;


    public TwilioConfiguration( final String _account, final String _password, final String _phone ) {
        account = _account;
        password = _password;
        phone = _phone;
    }


    public String getAccount() {
        return account;
    }


    public String getPassword() {
        return password;
    }


    public String getPhone() {
        return phone;
    }
}
