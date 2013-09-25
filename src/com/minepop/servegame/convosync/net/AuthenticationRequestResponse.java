package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class AuthenticationRequestResponse extends Message {

    public final boolean AUTH;
    public final Reason REASON;
    public final String VERSION;

    public static enum Reason {

        INVALID_PASSWORD, INVALID_USER, BANNED, LOGGED_IN
    }

    public AuthenticationRequestResponse(boolean auth, Reason reason, String version) {
        this.AUTH = auth;
        this.REASON = reason;
        this.VERSION = version;
    }

    @Override
    public String toString() {
        return "AuthenticationRequestResponse[" + AUTH + "," + VERSION + "," + REASON + "]";
    }
}
