package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class DisconnectMessage extends Message {

    public final Reason REASON;
    
    public static enum Reason {

        RESTARTING, CLOSING, KICKED, CRASHED
    }
    
    public DisconnectMessage(Reason reason) {
        this.REASON = reason;
    }
    
    public DisconnectMessage() {
        this.REASON = null;
    }

    @Override
    public String toString() {
        return "DisconnectMessage[" + REASON + "]";
    }
}
