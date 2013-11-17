package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class PlayerListMessage extends Message {

    public final String[] LIST;
    public final boolean JOIN;

    public PlayerListMessage(String[] list, boolean join) {
        this.LIST = list;
        this.JOIN = join;
    }

    public PlayerListMessage(String player, boolean join) {
        this.LIST = new String[]{player};
        this.JOIN = join;
    }

    @Override
    public String toString() {
        return "PlayerListMessage[" + JOIN + "]";
    }
}
