package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class CommandResponse extends PlayerMessage {
    
    public CommandResponse(String msg, MessageRecipient recip) {
        super(msg, recip);
    }
    
    public CommandResponse(String msg, String recip) {
        super(msg, recip);
    }
}
