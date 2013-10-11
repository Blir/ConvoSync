package com.minepop.servegame.convosync.server;

import com.minepop.servegame.convosync.net.*;
import com.minepop.servegame.convosync.server.Client.ClientType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static com.minepop.servegame.convosync.Main.COLOR_CHAR;
import static com.minepop.servegame.convosync.Main.format;
import static com.minepop.servegame.convosync.server.ConvoSyncServer.LOGGER;

/**
 *
 * @author Blir
 */
public final class Messenger {

    private ConvoSyncServer server;
    protected List<Client> deadClients = new ArrayList<Client>();
    protected List<Client> newClients = new ArrayList<Client>();

    protected Messenger(ConvoSyncServer server) {
        this.server = server;
    }

    protected synchronized void out(Object o, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }

        LOGGER.log(Level.FINE, "Adding {0}", newClients);
        server.clients.addAll(newClients);
        newClients.clear();

        if (o instanceof String) {
            out(new ChatMessage((String) o, false), sender);
        } else if (o instanceof PrivateMessage) {
            out((PrivateMessage) o, sender);
        } else if (o instanceof PlayerMessage) {
            out((PlayerMessage) o, sender);
        } else if (o instanceof ChatMessage) {
            out((ChatMessage) o, sender);
        } else if (o instanceof CommandMessage) {
            out((CommandMessage) o, sender);
        } else if (o instanceof PlayerListUpdate) {
            out((PlayerListUpdate) o);
        } else if (o instanceof DisconnectMessage) {
            close((DisconnectMessage) o);
        }

        LOGGER.log(Level.FINE, "Removing {0}", deadClients);
        server.clients.removeAll(deadClients);
        deadClients.clear();
    }

    private void out(ChatMessage msg, Client sender) {
        for (Client client : server.clients) {
            if (client != sender && client.auth) {
                client.sendMsg(msg, false);
            }
        }
        LOGGER.log(Level.INFO, "[{0}] {1} ",
                   new Object[]{sender == null ? "NA" : sender.socket.getPort(),
                                format(msg.MSG)});
    }

    protected void sendPlayerListUpdate() {
        PlayerListUpdate update;
        update = new PlayerListUpdate(server.userMap.keySet().toArray(
                new String[server.userMap.size()]), false);
        out(update, null);
    }

    protected void vanishPlayer(String s) {
        Set<String> userCopy = (new HashMap<String, String>(server.userMap)).keySet();
        userCopy.remove(s);
        PlayerListUpdate update = new PlayerListUpdate(userCopy.toArray(
                new String[userCopy.size()]), true);
        out(update, null);
    }

    private void out(PrivateMessage msg, Client sender) {
        String clientName = server.userMap.get(msg.RECIPIENT);
        if (clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT;
        }
        if (clientName == null && sender != null) {
            sender.sendMsg(new PlayerMessage(
                    COLOR_CHAR + "cPlayer \"" + COLOR_CHAR + "9" + msg.RECIPIENT + COLOR_CHAR + "c\" not found.",
                    msg.SENDER), false);
            return;
        }
        for (Client client : server.clients) {
            if (client.localname.equals(clientName)) {
                client.sendMsg(msg, false);
                return;
            }
        }
    }

    private void out(PlayerMessage msg, Client sender) {
        String clientName = server.userMap.get(msg.RECIPIENT);
        if (clientName == null) {
            LOGGER.log(Level.WARNING, "Is {0} a ghost?", msg.RECIPIENT);
            return;
        }
        if (clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT;
        }
        for (Client client : server.clients) {
            if (client.localname.equals(clientName)) {
                client.sendMsg(msg, false);
                return;
            }
        }
    }

    private void out(CommandMessage msg, Client sender) {
        for (Client client : server.clients) {
            if (client.type == ClientType.PLUGIN
                && client.name.equalsIgnoreCase(msg.TARGET)) {
                client.sendMsg(msg, false);
                if (sender != null) {
                    sender.sendMsg(new PlayerMessage(
                            COLOR_CHAR + "a" + msg + " sent!", msg.SENDER), false);
                }
                return;
            }
        }
        if (sender != null) {
            sender.sendMsg(new PlayerMessage(
                    COLOR_CHAR + "cServer " + COLOR_CHAR
                    + "9" + msg.TARGET + COLOR_CHAR + "c not found.", msg.SENDER), false);
        }
    }

    private void out(PlayerListUpdate update) {
        for (Client client : server.clients) {
            if (client.type == ClientType.APPLICATION && client.auth
                && (!update.VANISH || !server.getUser(client.name).op)) {
                client.sendMsg(update, false);
            }
        }
    }

    private void close(DisconnectMessage dmsg) {
        for (Client client : server.clients) {
            try {
                client.close(false, true, dmsg);
            } catch (IOException ex) {
                // ignore
            }
        }
    }
}
