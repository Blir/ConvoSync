package com.minepop.servegame.convosync.server;

import com.minepop.servegame.convosync.net.CustomMessage;
import com.minepop.servegame.convosync.net.Message;

/**
 *
 * @author Blir
 * @param <T> the message this handler handles
 */
public abstract class MessageHandler<T extends CustomMessage> {

    private boolean enabled;

    public MessageHandler()
            throws IllegalStateException {
        if (getName() == null) {
            throw new IllegalStateException("invalid name");
        }
        enabled = true;
    }

    public final boolean handle(Client handler, Message o) {
        if (!enabled) {
            return true;
        }
        try {
            handle(handler, (T) o);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public final void disable() {
        enabled = false;
    }

    public abstract void handle(Client handler, T t);

    public abstract String getName();

    @Override
    public final boolean equals(Object o) {
        return o instanceof MessageHandler
               ? this.getName().equals(((MessageHandler) o).getName())
               : false;
    }

    @Override
    public String toString() {
        return "MessageHandler[" + getName() + "," + enabled + "]";
    }
}
