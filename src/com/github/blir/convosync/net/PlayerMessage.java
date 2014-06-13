package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class PlayerMessage extends ChatMessage {

    public final MessageRecipient RECIPIENT;

    public PlayerMessage(String msg, MessageRecipient recip) {
        super(msg, true);
        this.RECIPIENT = recip;
    }
    
    public PlayerMessage(String msg, String recip) {
        this(msg, new MessageRecipient(recip, null));
    }
    
    @Override
    public String toString() {
        return "PlayerMessage[" + MSG + "," + RECIPIENT + "]";
    }
}
