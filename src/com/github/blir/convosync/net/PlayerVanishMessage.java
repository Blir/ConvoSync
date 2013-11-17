package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class PlayerVanishMessage extends Message {

    public final String PLAYER;
    public final boolean VANISH;

    public PlayerVanishMessage(String player, boolean join) {
        this.PLAYER = player;
        this.VANISH = join;
    }
    
    @Override
    public String toString() {
        return "PlayerVanishMessage[" + PLAYER + "," + VANISH + "]";
    }
}
