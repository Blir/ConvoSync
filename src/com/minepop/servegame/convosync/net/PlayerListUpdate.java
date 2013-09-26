package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class PlayerListUpdate extends Message {
    
    public final String[] LIST;
    
    public PlayerListUpdate(String[] list) {
        this.LIST = list;
    }
    
    @Override
    public String toString() {
        return "PlayerListUpdate[" + LIST.length + "]";
    }
}
