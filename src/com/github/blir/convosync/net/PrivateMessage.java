package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class PrivateMessage extends PlayerMessage {

    public final MessageRecipient SENDER;
    public final String SERVER;

    public PrivateMessage(MessageRecipient recip, MessageRecipient sender, String msg, String server) {
        super(msg, recip);
        this.SENDER = sender;
        this.SERVER = server;
    }

    @Override
    public String toString() {
        return "PrivateMessage[" + SENDER + "," + RECIPIENT + "]";
    }
}
