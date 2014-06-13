package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class MessageRecipient extends Message {

    public final String NAME;
    public final SenderType TYPE;

    public static enum SenderType {

        CONVOSYNC_CONSOLE, MINECRAFT_CONSOLE, MINECRAFT_PLAYER, CONVOSYNC_CLIENT,
        UNKNOWN
    }

    public MessageRecipient(String name, SenderType type) {
        this.NAME = name;
        this.TYPE = type == null ? SenderType.UNKNOWN : type;
    }
    
    public MessageRecipient(String name) {
        this.NAME = name;
        this.TYPE = SenderType.UNKNOWN;
    }

    @Override
    public String toString() {
        return String.format("MessageRecipient[%s,%s]", NAME, TYPE);
    }
}
