package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class AuthenticationRequestResponse extends Message {

    public enum Reason {

        INVALID_PASSWORD, INVALID_USER, BANNED, LOGGED_IN
    }
    public final boolean AUTH;
    public final Reason REASON;

    public AuthenticationRequestResponse(boolean auth, Reason reason) {
        this.AUTH = auth;
        this.REASON = reason;
    }

    @Override
    public String toString() {
        return "AuthenticationRequestResponse[" + AUTH + "]";
    }
}
