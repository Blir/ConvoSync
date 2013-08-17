package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class AuthenticationRequest extends Message {

    public final String NAME, PASSWORD;

    public AuthenticationRequest(String name, String password) {
        this.NAME = name;
        this.PASSWORD = password;
    }
    
    @Override
    public String toString() {
        return "AuthenticationRequest[" + NAME + "]";
    }
}
