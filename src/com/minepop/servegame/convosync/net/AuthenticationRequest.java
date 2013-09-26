package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class AuthenticationRequest extends Message {

    public final String NAME, PASSWORD, VERSION;

    public AuthenticationRequest(String name, String password, String version) {
        this.NAME = name;
        this.PASSWORD = password;
        this.VERSION = version;
    }
    
    @Override
    public String toString() {
        return "AuthenticationRequest[" + NAME + "," + VERSION + "]";
    }
}
