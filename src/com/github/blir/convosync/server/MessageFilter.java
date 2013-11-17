package com.github.blir.convosync.server;

import com.github.blir.convosync.net.CustomMessage;

/**
 *
 * @author Blir
 */
public abstract class MessageFilter {

    private boolean enabled;

    public MessageFilter()
            throws IllegalStateException {
        if (getName() == null) {
            throw new IllegalStateException("invalid name");
        }
        enabled = true;
    }

    public abstract String getName();

    public abstract boolean filter(CustomMessage msg, Client client);

    public final void disable() {
        enabled = false;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof MessageFilter
               ? this.getName().equals(((MessageFilter) o).getName())
               : false;
    }

    @Override
    public String toString() {
        return "MessageFilter[" + getName() + "," + enabled + "]";
    }
}
