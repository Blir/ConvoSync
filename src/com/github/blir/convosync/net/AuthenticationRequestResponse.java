package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class AuthenticationRequestResponse extends Message {

    public final boolean AUTH;
    public final Reason REASON;
    public final String VERSION;
    public final boolean OP;

    public static enum Reason {

        INVALID_PASSWORD, INVALID_USER, BANNED, LOGGED_IN
    }

    public AuthenticationRequestResponse(boolean auth, Reason reason,
                                         String version, boolean op) {
        this.AUTH = auth;
        this.REASON = reason;
        this.VERSION = version;
        this.OP = op;
    }

    @Override
    public String toString() {
        return "AuthenticationRequestResponse[" + AUTH + "," + VERSION + "," + REASON + "]";
    }
}
