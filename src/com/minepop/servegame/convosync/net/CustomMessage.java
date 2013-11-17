package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public abstract class CustomMessage extends Message {

    protected boolean overrides;

    public CustomMessage()
            throws IllegalStateException {
        if (getName() == null) {
            throw new IllegalStateException("invalid name");
        }
    }

    public abstract String getName();

    public final boolean overrides() {
        return overrides;
    }

    @Override
    public String toString() {
        return "CustomMessage[" + getName() + "]";
    }
}
