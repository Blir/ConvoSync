package com.github.blir.convosync.net;

import java.util.UUID;

/**
 *
 * @author Blir
 */
public class Player extends Message {

    public final UUID UUID;
    public final String NAME;

    public Player(UUID uuid, String name) {
        this.UUID = uuid;
        this.NAME = name;
    }
}
