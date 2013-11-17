package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class PlayerListUpdate extends Message {

    public final String[] LIST;
    public final Boolean VANISH;

    public PlayerListUpdate(String[] list, boolean vanish) {
        this.LIST = list;
        this.VANISH = vanish;
    }

    @Override
    public String toString() {
        return "PlayerListUpdate[" + LIST.length + "," + VANISH + "]";
    }
}
