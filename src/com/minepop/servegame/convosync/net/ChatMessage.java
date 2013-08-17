package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class ChatMessage extends Message {

    public final String MSG;
    public final boolean OVERRIDE;

    public ChatMessage(String msg, boolean override) {
        this.MSG = msg;
        this.OVERRIDE = override;
    }
    
    @Override
    public String toString() {
        return "ChatMessage[" + MSG + "," + OVERRIDE + "]";
    }
}
