package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class PlayerMessage extends ChatMessage {

    public final String RECIPIENT;

    public PlayerMessage(String msg, String recip) {
        super(msg, true);
        this.RECIPIENT = recip;
    }
    
    @Override
    public String toString() {
        return "PlayerMessage[" + MSG + "," + RECIPIENT + "]";
    }
}
