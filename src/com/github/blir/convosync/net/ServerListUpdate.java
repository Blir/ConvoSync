package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class ServerListUpdate extends Message {

    public final String[] LIST;

    public ServerListUpdate(String[] list) {
        this.LIST = list;
    }

    @Override
    public String toString() {
        return "ServerListUpdate[" + LIST.length + "]";
    }
}
