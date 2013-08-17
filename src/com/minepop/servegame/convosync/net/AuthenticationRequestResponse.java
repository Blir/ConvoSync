package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class AuthenticationRequestResponse extends Message {

    public final boolean AUTH;

    public AuthenticationRequestResponse(boolean auth) {
        this.AUTH = auth;
    }
    
    @Override
    public String toString() {
        return "AuthenticationRequestResponse[" + AUTH + "]";
    }
}
